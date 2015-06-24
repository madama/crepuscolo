package net.etalia.crepuscolo.mvc.httpclient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.mvc.Call;
import net.etalia.crepuscolo.mvc.Response;
import net.etalia.jalia.BeanJsonDeSer;
import net.etalia.jalia.JsonDeSer;
import net.etalia.jalia.TypeUtil;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class HttpClientResponse<X> implements Response<X> {

	protected final static Logger log = Logger.getLogger(HttpClientResponse.class.getName());

	private Type type;
	private HttpResponse httpresp;
	private byte[] payload;

	private boolean consumed = false;
	private X value;

	public HttpClientResponse(HttpResponse httpresp, byte[] payload, Type type) {
		this.httpresp = httpresp;
		this.payload = payload;
		this.type = type;
	}

	@Override
	public X cast() {
		if (!consumed) {
			consumed = true;
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Deserialize: {}", new String(payload));
			}
			value = Call.eom.readValue(payload, TypeUtil.get(type));
		}
		return value;
	}

	@Override
	public int getStatusCode() {
		return this.httpresp.getStatusLine().getStatusCode();
	}

	@Override
	public Map<String, String> getHeaders() {
		Header[] allHeaders = this.httpresp.getAllHeaders();
		Map<String,String> ret = new HashMap<String, String>();
		for (Header header : allHeaders) {
			ret.put(header.getName(), header.getValue());
		}
		return ret;
	}

	@Override
	public Map<String, Object> asMap() {
		CrepuscoloObjectMapper om = new CrepuscoloObjectMapper(true);
		om.init();
		Iterator<JsonDeSer> iter = om.getRegisteredDeSers().iterator();
		while (iter.hasNext()) {
			if (iter.next() instanceof BeanJsonDeSer) {
				iter.remove();
				break;
			}
		}
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Deserialize to map : {}", new String(payload));
		}
		
		return om.readValue(payload);
	}

}
