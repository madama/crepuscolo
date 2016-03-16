package net.etalia.crepuscolo.todolist.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.Stored;
import net.etalia.crepuscolo.validation.ValidationMessage;

@javax.persistence.Entity
@Table(name="tasklist")
public class TaskList extends BaseEntity implements Completable, Stored {

	private User user;
	private String name;
	private List<Task> tasks;

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

	@OneToMany
	public List<Task> getTasks() {
		return this.tasks;
	}
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

}
