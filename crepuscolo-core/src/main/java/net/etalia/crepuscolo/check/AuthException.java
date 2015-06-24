package net.etalia.crepuscolo.check;

public class AuthException extends RuntimeException {

	private int statusCode = 0;

	public AuthException statusCode(int code) {
		this.statusCode = code;
		return this;
	}

	public int getStatusCode() {
		return statusCode;
	}

}
