package net.etalia.crepuscolo.todolist.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import net.etalia.crepuscolo.check.Cache;
import net.etalia.crepuscolo.check.ChSelf;
import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.domain.Entity;
import net.etalia.crepuscolo.domain.Stored;
import net.etalia.crepuscolo.validation.ValidationMessage;
import net.etalia.jalia.annotations.JsonIgnore;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;

@javax.persistence.Entity
@Table(name="user")
public class User extends Entity implements Authenticable, Stored {

	private String email;
	private String username;
	private String password;
	private String tokenSalt;
	private String description;

	@Email(message=ValidationMessage.MALFORMED)
	@NotNull(message=ValidationMessage.REQUIRED)
	@Column(unique=true, nullable=false)
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	@NotNull(message=ValidationMessage.REQUIRED)
	@Length(min=2, max=40, message=ValidationMessage.LENGTH)
	@Column(nullable=false)
	public String getUsername() {
		return this.username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	@JsonIgnore
	@Column(nullable=false)
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	@JsonIgnore
	@Column(nullable=false)
	public String getTokenSalt() {
		return tokenSalt;
	}
	@Override
	public void setTokenSalt(String tokenSalt) {
		this.tokenSalt = tokenSalt;
	}

	@NotNull(message=ValidationMessage.REQUIRED)
	@Length(min=5, max=250, message=ValidationMessage.LENGTH)
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Transient
	@ChSelf
	@Cache(expires="1h 30m")
	public List<Task> getRandomTasks() {
		return null;
	}

	public class Queries {
		public static final String BY_EMAIL = "from User u where u.email=:email";
	}

}
