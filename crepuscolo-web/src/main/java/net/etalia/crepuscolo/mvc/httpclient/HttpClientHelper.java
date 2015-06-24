package net.etalia.crepuscolo.mvc.httpclient;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;

public class HttpClientHelper {

	public HttpClient createDefaultClient(int connections, int timeout) {
		PoolingClientConnectionManager cm = 
			new PoolingClientConnectionManager(
				SchemeRegistryFactory.createDefault(),
				timeout, TimeUnit.MILLISECONDS
			);
		cm.setDefaultMaxPerRoute(connections);
		cm.setMaxTotal(connections);
		
		DefaultHttpClient ret = new DefaultHttpClient(cm);
		
		ret.getParams().setParameter("http.socket.timeout", timeout);
		ret.getParams().setParameter("http.connection.timeout", timeout);
		//ret.getParams().setParameter("http.connection-manager.timeout", timeout);
		ret.getParams().setParameter("http.protocol.head-body-timeout", timeout);
		return ret;
	}

}
