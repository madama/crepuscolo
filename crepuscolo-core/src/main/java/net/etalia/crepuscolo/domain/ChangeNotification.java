package net.etalia.crepuscolo.domain;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.Jsonable;
import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonSetter;

/**
 * Represents a change travelling from an API to an {@link ChangeNotifier}
 * in another API.
 */
@JsonDefaultFields("type,clazz,instance,created")
public class ChangeNotification<T extends Jsonable> implements Jsonable {

	public enum Type {
		ADDED,
		UPDATED,
		DELETED
	}

	private Type type;
	private Class<? extends BaseEntity> clazz;
	private T instance;

	private long created = System.currentTimeMillis();

	public ChangeNotification() {
	}

	/**
	 * Build a new notification
	 * @param type The type of the notification
	 * @param clazz The class of the object
	 * @param id the id of the object
	 */
	public ChangeNotification(Type type, Class<? extends BaseEntity> clazz, T instance) {
		this.type = type;
		this.clazz = clazz;
		this.instance = instance;
	}

	public Type getType() {
		return type;
	}

	public Class<? extends BaseEntity> getClazz() {
		return clazz;
	}

	public T getInstance() {
		return instance;
	}

	public long getCreated() {
		return created;
	}

	@Override
	public String toString() {
		return "ChangeNotification [type=" + type + ", clazz=" + clazz + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instance == null) ? 0 : instance.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChangeNotification other = (ChangeNotification) obj;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public void setInstance(T inst) {
		this.instance = inst;
	}

	@JsonSetter
	private void setClazz(Class<? extends BaseEntity> clazz) {
		this.clazz = clazz;
	}

	@JsonSetter
	private void setType(Type type) {
		this.type = type;
	}

}