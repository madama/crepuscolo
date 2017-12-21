package net.etalia.crepuscolo.domain;

import javax.persistence.Column;

import net.etalia.jalia.annotations.JsonIgnore;

public aspect AuthenticableDefaultImpl {

	private String Authenticable.password;
	private String Authenticable.tokenSalt;

	@JsonIgnore
	@Column(nullable=false)
	public String Authenticable.getPassword() {
		return this.password;
	}
	public void Authenticable.setPassword(String password) {
		this.password = password;
	}

	@JsonIgnore
	@Column(nullable=false)
	public String Authenticable.getTokenSalt() {
		return this.tokenSalt;
	}
	public void Authenticable.setTokenSalt(String tokenSalt) {
		this.tokenSalt = tokenSalt;
	}


}
