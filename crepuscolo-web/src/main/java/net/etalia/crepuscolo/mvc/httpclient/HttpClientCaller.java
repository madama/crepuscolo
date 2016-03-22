package net.etalia.crepuscolo.mvc.httpclient;

import java.lang.reflect.Type;

import org.apache.http.client.HttpClient;

import net.etalia.crepuscolo.mvc.Call;
import net.etalia.crepuscolo.mvc.Caller;

public class HttpClientCaller<Serv> extends Caller<Serv> {

	protected HttpClient httpClient;
	protected String baseUrl;
	protected String fieldsParameter;

	public String getFieldsParameter() {
		return fieldsParameter;
	}

	public void setFieldsParameter(String fieldsParameter) {
		this.fieldsParameter = fieldsParameter;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	public String getBaseUrl() {
		return baseUrl;
	}

	public HttpClientCaller(Class<Serv> serviceInterface) {
		super(serviceInterface);
	}

	public HttpClientCaller(Class<Serv> serviceInterface, HttpClient client, String baseUrl) {
		this(serviceInterface);
		this.setBaseUrl(baseUrl);
		this.setHttpClient(client);
	}

	@Override
	protected <X> Call<X> createCall(Type type, HttpMethod method, String path) {
		if (fieldsParameter == null) {
			return new HttpClientCall<X>(this, type, method, path);
		} else {
			return new HttpClientCall<X>(this, type, method, path, fieldsParameter);
		}
	}

}
