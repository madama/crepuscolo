package net.etalia.crepuscolo.todolist.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.Stored;
import net.etalia.crepuscolo.validation.ValidationMessage;

@Entity
@Table(name="task")
public class Task extends BaseEntity implements Completable, Stored {

	private User user;
	private String name;
	private String description;
	private Priority priority;

	@ManyToOne
	public User getUser() {
		return this.user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	@NotNull(message=ValidationMessage.REQUIRED)
	@Length(min=2, max=40, message=ValidationMessage.LENGTH)
	@Column(nullable=false)
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@NotNull(message=ValidationMessage.REQUIRED)
	@Length(min=5, max=250, message=ValidationMessage.LENGTH)
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Enumerated(EnumType.STRING)
	@Column(nullable=false)
	public Priority getPriority() {
		return this.priority;
	}
	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public enum Priority {
		LOW,
		NORMAL,
		CRITICAL;
	}

}
