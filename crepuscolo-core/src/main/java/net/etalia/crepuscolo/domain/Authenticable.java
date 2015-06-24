package net.etalia.crepuscolo.domain;

public interface Authenticable {

	public String getPassword();

	public void setTokenSalt(String tokenSalt);

	public String getTokenSalt();

}
