package net.etalia.crepuscolo.mvc.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.etalia.crepuscolo.mvc.Call;
import net.etalia.crepuscolo.mvc.Caller.HttpMethod;
import net.etalia.crepuscolo.mvc.JsonedException;
import net.etalia.crepuscolo.mvc.Response;
import net.etalia.crepuscolo.utils.Check;
import net.etalia.crepuscolo.utils.Strings;
import net.etalia.crepuscolo.utils.URIBuilder;
import net.etalia.jalia.TypeUtil;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpClientCall<X> extends Call<X> {

	protected final static Logger log = Logger.getLogger(HttpClientCall.class.getName());

	public String fieldsParameter = "outProperties";

	@SuppressWarnings("rawtypes")
	private HttpClientCaller caller;

	@SuppressWarnings("rawtypes")
	public HttpClientCall(HttpClientCaller caller, Type type, HttpMethod method, String path) {
		super(type, method, path);
		this.caller = caller;
	}

	@SuppressWarnings("rawtypes")
	public HttpClientCall(HttpClientCaller caller, Type type, HttpMethod method, String path, String fieldsParameter) {
		super(type, method, path);
		this.caller = caller;
		this.fieldsParameter = fieldsParameter;
	}

	@Override
	public Response<X> execute(boolean check) {
		prepareBody();
		
		String uri = Strings.pathConcat(this.caller.getBaseUrl(), super.path);
		if (System.getProperty("embedWebPort") != null)
			uri = uri.replace("${embedWebPort}", System.getProperty("embedWebPort"));
		HttpUriRequest message = null;
		
		URIBuilder ub = null;
		try {
			ub = new URIBuilder(uri);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Error parsing the uri " + uri, e);
		}
		
		if (hasFields()) {
			// Always add requested fields as a parameter
			// TODO we should optimize this later, using sets or whatever
			ub.addParameter(fieldsParameter, getFieldsAsString());
		}
		
		if (super.method == HttpMethod.GET || super.method == HttpMethod.DELETE) {
			Check.illegalstate.assertNull("Cannot send a body with a get request", super.requestBody);
			if (hasParameters()) {
				setRequestParameters(ub);
			} 
			try {
				uri = ub.build().toString();
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Error adding outProperties parameter to the uri " + uri, e);
			}				
			if (super.method == HttpMethod.GET) {
				message = new HttpGet(uri);
			} else {
				message = new HttpDelete(uri);
			}
		} else if (super.method == HttpMethod.POST || super.method == HttpMethod.PUT) {
			//Check.illegalstate.assertFalse("Cannot send both parameters and a body in a POST or PUT", hasParameters() && hasBody());
			byte[] payload = null;
			if (hasParameters() && !hasBody()) {
				List<NameValuePair> nvp = new ArrayList<NameValuePair>();
				for (Entry<String, Object> entry : super.requestParameters.entrySet()) {
					Object value = entry.getValue();
					if (value == null) continue;
					if (value.getClass().isArray()) {
						// This may not return what is expected
						value = Arrays.asList((Object[])value);
					}
					String name = entry.getKey();
					if (name.endsWith("[]")) {
						name = name.substring(0, name.length() - 2);
					}
					
					if (value instanceof Collection) {
						for (Object inval : ((Collection<Object>)value)) {
							nvp.add(new BasicNameValuePair(name, convertToString(inval)));
						}
					} else {
						nvp.add(new BasicNameValuePair(name, convertToString(value)));
					}
				}
				payload = URLEncodedUtils.format(nvp, "utf8").getBytes();
			} else if (hasParameters()) {
				setRequestParameters(ub);
			} 
			if (hasBody()) {
				payload = super.requestBody;
			}
			if (payload == null) payload = new byte[0];
			
			try {
				uri = ub.build().toString();
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Error adding outProperties parameter to the uri " + uri, e);
			}
			
			HttpEntity entity = new ByteArrayEntity(payload);
			if (super.method == HttpMethod.POST) {
				HttpPost post = new HttpPost(uri);
				post.setEntity(entity);
				message = post;
			} else {
				HttpPut put = new HttpPut(uri);
				put.setEntity(entity);
				message = put;
			}
			
			if (hasBody()) {
				message.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
			} else {
				message.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
			}
		}
		
		if (hasHeaders()) {
			for (Entry<String, String> entry : requestHeaders.entrySet()) {
				message.addHeader(entry.getKey(), entry.getValue());
			}
		}
		
		HttpResponse httpresp = null;
		byte[] payload = null;
		try {
			httpresp = caller.getHttpClient().execute(message);
			// If there is an entity, read it now and consume it, to avoid connection leaks
			HttpEntity entity = httpresp.getEntity();
			if (entity != null) {
				payload = EntityUtils.toByteArray(entity);
				EntityUtils.consumeQuietly(entity);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error executing HTTP call" + httpCallTrace(message, requestBody, httpresp, null), e);
		}
		
		
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, httpCallTrace(message, requestBody, httpresp, payload));
		}

		int statusCode = httpresp.getStatusLine().getStatusCode();
		if (check && !isAcceptable(statusCode)) {
			Header respContentType = httpresp.getFirstHeader(HttpHeaders.CONTENT_TYPE);
			if (respContentType != null && respContentType.getValue().indexOf("json") != -1) {
				Map<String,Object> readValue = null;
				try {
					readValue = eom.readValue(payload, new TypeUtil.Specific<Map<String,Object>>(){}.type());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (readValue != null) throw new JsonedException("Calling\n" + uri + "\nreturned\n" + httpresp.getStatusLine()+ httpCallTrace(message, requestBody, httpresp, payload), readValue, statusCode);
			}
			throw new IllegalStateException("Calling\n" + uri + "\nreturned\n" + httpresp.getStatusLine() + httpCallTrace(message, requestBody, httpresp, payload));
		}
		
		try {
			return new HttpClientResponse<X>(httpresp, payload, this.returnType);
		} catch (Throwable e) {
			throw new RuntimeException("Error parsing response from " + uri + "\n" + httpresp.getStatusLine() + httpCallTrace(message, requestBody, httpresp, null), e);
		}
	}

	public static String httpCallTrace(HttpRequest req, byte[] reqPayload, HttpResponse resp, byte[] respPayload) {
		StringBuilder trace = new StringBuilder();
		trace.append('\n');
		if (req != null) {
			trace.append(BasicLineFormatter.formatRequestLine(req.getRequestLine(), null));
			httpEntityTrace(req, reqPayload, trace);
			trace.append("\n\n");
			trace.append("----------------------------------------------------\n");
		}
		if (resp != null) {
			trace.append(BasicLineFormatter.formatStatusLine(resp.getStatusLine(), null));
			httpEntityTrace(resp, respPayload, trace);
		} else {
			trace.append("No response");
		}
		return trace.toString();
	}

	private static void httpEntityTrace(HttpMessage msg, byte[] payload,
			StringBuilder trace) {
		trace.append('\n');
		for (Header header : msg.getAllHeaders()) {
			trace.append(BasicLineFormatter.formatHeader(header, null));
			trace.append('\n');
		}
		trace.append('\n');
		if (payload != null) {
			trace.append(new String(payload));
		} else if (msg instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) msg).getEntity();
			if (entity.isRepeatable()) {
				InputStream content = null;
				try {
					content = ((HttpEntityEnclosingRequest) msg).getEntity().getContent();
					trace.append(IOUtils.toString(content));
				} catch (IOException e) {
					trace.append("!! Error while tracing payload\n");
				} finally {
					try {
						content.close();
					} catch (IOException e) {
					}
				}
			} else {
				trace.append("!! Unrepeatable payload\n");
			}
		} else if (msg instanceof HttpResponse) {
			HttpResponse resp = (HttpResponse) msg;
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				if (entity.isRepeatable()) {
					InputStream content = null;
					try {
						content = entity.getContent();
						trace.append(IOUtils.toString(content));
					} catch (IOException e) {
						trace.append("!! Error while tracing payload\n");
					} finally {
						try {
							content.close();
						} catch (IOException e) {
						}
					}
				} else {
					trace.append("!! Unrepeatable payload\n");
				}
			}
		}
	}
}
