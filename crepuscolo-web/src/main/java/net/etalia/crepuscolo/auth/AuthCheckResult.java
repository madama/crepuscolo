package net.etalia.crepuscolo.auth;

public class AuthCheckResult {

	private String urlSpec;
	private int statusCode;

	public AuthCheckResult(String urlSpec, int statusCode) {
		this.urlSpec = urlSpec;
		this.statusCode = statusCode;
	}

	public String getUrl() {
		return urlSpec;
	}

	public boolean getOk() {
		return statusCode == 100;
	}

	public String getError() {
		return statusCode == 100 ? null : (statusCode + "");
	}

}
