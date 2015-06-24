package net.etalia.crepuscolo.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

public class HttpException extends RuntimeException {

	private static final long serialVersionUID = -1698656943532877561L;

	private String errorCode = null;
	private String message;
	private int statusCode = 500;

	private Map<String,Object> properties;

	private boolean statusCodeSet;

	public HttpException() {
		super();
	}

	public HttpException statusCode(int code) {
		this.statusCode = code;
		this.statusCodeSet = true;
		return this;
	}

	public HttpException errorCode(String code) {
		this.errorCode = code;
		return this;
	}

	public HttpException message(String message) {
		this.message = message;
		return this;
	}

	public HttpException cause(Throwable cause) {
		this.initCause(cause);
		return this;
	}

	public HttpException statusCode(HttpStatus status) {
		this.statusCode(status.value());
		if (this.message == null) this.message = status.getReasonPhrase();
		return this;
	}	

	@Override
	public String getMessage() {
		return this.message;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public boolean hasSetStatusCode() {
		return statusCodeSet;
	}

	public HttpException properties(Map<String, Object> properties) {
		this.properties = properties;
		return this;
	}

	public HttpException property(String name, Object value) {
		if (this.properties == null) this.properties = new HashMap<String, Object>();
		this.properties.put(name, value);
		return this;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		if (properties == null)
			return super.toString();
		return super.toString() + "\n" + this.properties.toString();
	}

}
