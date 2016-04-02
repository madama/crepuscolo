package net.etalia.crepuscolo.mvc;

import java.util.Map;

public class JsonedException extends RuntimeException {

	private static final long serialVersionUID = -4745379703039071409L;

	private Map<String, Object> properties;

	private int statusCode;

	public JsonedException(String message, Map<String, Object> properties, int statusCode) {
		super(message + properties.toString());
		this.properties = properties;
		this.statusCode = statusCode;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public int getStatusCode() {
		return statusCode;
	}

}
