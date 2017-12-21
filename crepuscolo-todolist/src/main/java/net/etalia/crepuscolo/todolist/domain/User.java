package net.etalia.crepuscolo.todolist.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import net.etalia.crepuscolo.check.Cache;
import net.etalia.crepuscolo.check.ChSelf;
import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.Stored;
import net.etalia.crepuscolo.validation.ValidationMessage;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name="user")
public class User extends BaseEntity implements Authenticable, Stored {

	private String email;
	private String username;
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
