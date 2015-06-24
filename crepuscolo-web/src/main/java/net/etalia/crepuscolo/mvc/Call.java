package net.etalia.crepuscolo.mvc;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.etalia.crepuscolo.auth.AuthData;
import net.etalia.crepuscolo.auth.AuthFilter;
import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.mvc.Caller.HttpMethod;
import net.etalia.crepuscolo.utils.Check;
import net.etalia.crepuscolo.utils.URIBuilder;
import net.etalia.jalia.DefaultOptions;
import net.etalia.jalia.OutField;
import net.etalia.jalia.spring.JaliaParametersFilter;


/**
 * 
 * Api aa = Call.service(Api.class);
 * Entity entity = Call.method(aa.method(param,param)).withFields("x","y","z").execute().cast();
 * 
 * Response<?> resp = Call.path("/user/" + id).addParameter("name","Mario").addHeader(...).execute();
 * resp.getHeader();
 * resp.getJsonBody();
 * 
 * @author Simone Gianni <simoneg@apache.org>
 *
 * @param <T>
 */
public abstract class Call<Ret> {

	private static Map<HttpMethod, BitSet> DEFAULT_ACCEPTS = new HashMap<HttpMethod, BitSet>();

	static {
		{
			BitSet bs = new BitSet(600);
			bs.set(200,205);
			DEFAULT_ACCEPTS.put(HttpMethod.GET, bs);
		}
		{
			BitSet bs = new BitSet(600);
			bs.set(200,205);
			DEFAULT_ACCEPTS.put(HttpMethod.POST, bs);
		}
		{
			BitSet bs = new BitSet(600);
			bs.set(200,205);
			DEFAULT_ACCEPTS.put(HttpMethod.PUT, bs);
			DEFAULT_ACCEPTS.put(HttpMethod.DELETE, bs);
		}
		{
			BitSet bs = new BitSet(600);
			bs.set(200,299);
			DEFAULT_ACCEPTS.put(null, bs);
		}
	}

	public static CrepuscoloObjectMapper eom = new CrepuscoloObjectMapper(true);
	public static CrepuscoloObjectMapper eomFull = new CrepuscoloObjectMapper(true);

	static {
		eomFull.setOption(DefaultOptions.INCLUDE_NULLS, true);
		eomFull.setOption(DefaultOptions.INCLUDE_EMPTY, true);
	}

	protected Set<String> requestFields = new HashSet<String>();
	protected Set<String> sendFields = new HashSet<String>();
	protected Type returnType;
	protected HttpMethod method;
	protected String path;
	protected Map<String, Object> requestParameters = new HashMap<String, Object>();
	protected Map<String, String> requestHeaders = new HashMap<String, String>();
	private Object requestBodyBean;
	protected byte[] requestBody;
	protected String systemId;
	protected String userId;

	protected BitSet accept = null;

	public Call(Type type, HttpMethod method, String path) {
		this.returnType = type;
		this.method = method;
		this.path = path;
		setupAuth();
	}

	public Call<Ret> withFields(String... fields) {
		if (fields == null) return this;
		requestFields.addAll(Arrays.asList(fields));
		return this;
	}

	public Call<Ret> sendFields(String... fields) {
		if (fields == null) return this;
		sendFields.addAll(Arrays.asList(fields));
		return this;
	}

	public Call<Ret> accept(int... statusCodes) {
		if (accept == null) accept = new BitSet(600);
		for (int sc : statusCodes) accept.set(sc);
		return this;
	}

	public Call<Ret> acceptRange(int from, int to) {
		if (accept == null) accept = new BitSet(600);
		accept.set(from, to);
		return this;
	}

	public boolean isAcceptable(int statusCode) {
		BitSet bs = accept;
		if (bs == null) {
			bs = DEFAULT_ACCEPTS.get(this.method);
		}
		if (bs == null) {
			bs = DEFAULT_ACCEPTS.get(null);
		}
		if (bs == null) return true;
		return bs.get(statusCode);
	}

	protected void prepareBody() {
		if (requestBody == null && requestBodyBean != null) {
			try {
				if (this.sendFields.size() == 0) {
					this.setBody(eom.writeValueAsBytes(requestBodyBean, new OutField(true)));
				} else {
					this.setBody(eomFull.writeValueAsBytes(requestBodyBean, OutField.getRoot(this.sendFields.toArray(new String[this.sendFields.size()]))));
				}
			} catch (Exception e) {
				throw new RuntimeException("Error setting entity as request body", e);
			}
		}
	}

	public Response<Ret> execute() {
		return execute(true);
	}

	public abstract Response<Ret> execute(boolean check);

	public Call<Ret> withRequestedFields() {
		OutField fields = JaliaParametersFilter.getFields();
		if (fields == null)
			requestFields.addAll(fields.toStringList());
		return this;
	}

	protected String convertToString(Object obj) {
		String valstr = null;
		if (obj instanceof Class) {
			valstr = ((Class<?>)obj).getName();
		} else {
			// XXX SG maybe using .toString directly is not the best thing to do?
			valstr = obj.toString();
		}
		return valstr;
	}

	public Call<Ret> setPathVariable(String name, Object argument) {
		// XXX SG maybe using .toString directly is not the best thing to do?
		String npath = this.path.replaceAll("\\{" + name + "(\\:.*)?\\}", argument.toString());
		Check.illegalargument.assertFalse("Cannot find parameter {" + name + "} in path " + this.path, npath.equals(this.path));
		this.path = npath;
		return this;
	}	

	public Call<Ret> setParameter(String name, Object value) {
		if (value == null) {
			this.requestParameters.remove(name);
		} else {
			this.requestParameters.put(name, value);
		}
		return this;
	}

	public Call<Ret> setHeader(String name, Object value) {
		if (value == null) {
			this.requestHeaders.remove(name);
		} else {
			this.requestHeaders.put(name, convertToString(value));
		}
		return this;
	}

	public Call<Ret> setBody(byte[] body) {
		this.requestBody = body;
		return this;
	}

	public Call<Ret> setBody(Object o) {
		this.requestBodyBean = o;
		return this;
	}

	public Call<Ret> authAsSystem(String system) {
		this.systemId = system;
		this.userId = AuthFilter.getAuthData().getUserId();
		setupAuth();
		return this;
	}

	protected void setupAuth() {
		if (systemId != null) {
			setHeader("Authorization", AuthData.produceForSystem(systemId, userId));
		} else if (userId != null) {
			setHeader("Authorization", AuthData.produceForUser(userId, "abc"));
		} else if (AuthFilter.getAuthData().getCurrentToken() != null) {
			setHeader("Authorization", "Etalia " + AuthFilter.getAuthData().getCurrentToken());
		}
	}

	public Call<Ret> authAsUser(String userId) {
		this.userId = userId;
		setupAuth();
		return this;
	}

	public Call<Ret> authAsToken(String token) {
		setHeader("Authorization", "Etalia " + token);
		return this;
	}

	public boolean hasParameters() {
		return requestParameters != null && requestParameters.size() > 0;
	}

	public boolean hasBody() {
		return requestBody != null && requestBody.length > 0;
	}

	public boolean hasFields() {
		return requestFields != null && requestFields.size() > 0;
	}

	public boolean hasHeaders() {
		return requestHeaders != null && requestHeaders.size() > 0;
	}

	protected String getFieldsAsString() {
		if (!hasFields()) return null;
		StringBuilder sb = new StringBuilder();
		for (String fld : requestFields) {
			sb.append(fld);
			sb.append(',');
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public HttpMethod getMethod() {
		return this.method;
	}

	public String getPath() {
		return this.path;
	}

	protected void setRequestParameters(URIBuilder ub) {
		for (Entry<String, Object> entry : this.requestParameters.entrySet()) {
			Object value = entry.getValue();
			if (value == null) continue;
			if (value.getClass().isArray()) {
				// This may not return what is expected
				value = Arrays.asList(value);
			}
			if (value instanceof Collection) {
				for (Object inval : ((Collection<Object>)value)) {
					ub.addParameter(entry.getKey(), convertToString(inval));
				}
			} else {
				ub.addParameter(entry.getKey(), convertToString(value));
			}
		}
	}

}
