package net.etalia.crepuscolo.todolist.domain;

import java.util.List;

import javax.persistence.Table;
import javax.persistence.Transient;

import net.etalia.crepuscolo.check.Cache;
import net.etalia.crepuscolo.check.ChSelf;
import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.domain.Entity;
import net.etalia.crepuscolo.domain.Stored;

@javax.persistence.Entity
@Table(name="user")
public class User extends Entity implements Authenticable, Stored {

	@Transient
	@ChSelf
	@Cache(expires="1h 30m")
	public List<Task> getRandomTasks() {
		return null;
	}

}
