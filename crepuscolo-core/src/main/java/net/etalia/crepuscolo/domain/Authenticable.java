package net.etalia.crepuscolo.domain;

public interface Authenticable {

	public String getPassword();

	public void setPassword(String password);

	public String getTokenSalt();

	public void setTokenSalt(String tokenSalt);

}
