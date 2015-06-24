package net.etalia.crepuscolo.mvc.httpclient;

import java.util.Arrays;
import java.util.Map;

import net.etalia.crepuscolo.mvc.Auther;
import net.etalia.crepuscolo.utils.Strings;
import net.etalia.crepuscolo.utils.URIBuilder;
import net.etalia.crepuscolo.utils.URIBuilder.NameValuePair;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

public class HttpClientAuther extends Auther {

	private HttpClientCaller<?> caller;

	public HttpClientAuther(HttpClientCaller<?> caller) {
		super(caller);
		this.caller = caller;
	}

	@Override
	public Map<String, Boolean> check() {
		String uri = Strings.pathConcat(this.caller.getBaseUrl(), "/authcheck");
		if (System.getProperty("embedWebPort") != null)
			uri = uri.replace("${embedWebPort}", System.getProperty("embedWebPort"));
		HttpPost post = new HttpPost(uri);
		byte[] reqPayload = URIBuilder.format(Arrays.asList(new NameValuePair("check", getCheckPayload())), URIBuilder.UTF_8).getBytes();
		HttpEntity entity = new ByteArrayEntity(reqPayload);
		post.setEntity(entity);
		post.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
		if (token != null) post.setHeader("Authorization", "Crepuscolo " + token);

		HttpResponse httpresp = null;
		byte[] payload = null;
		try {
			httpresp = caller.getHttpClient().execute(post);
			// If there is an entity, read it now and consume it, to avoid connection leaks
			entity = httpresp.getEntity();
			if (entity != null) {
				payload = EntityUtils.toByteArray(entity);
				EntityUtils.consumeQuietly(entity);
			}
			
			if (httpresp.getStatusLine().getStatusCode() != 200) {
				throw new IllegalStateException("Authcheck call failed " + HttpClientCall.httpCallTrace(post, reqPayload, httpresp, payload));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error executing HTTP call" + HttpClientCall.httpCallTrace(post, reqPayload, httpresp, null), e);
		}
		return super.parseResponse(new String(payload));
	}

}
