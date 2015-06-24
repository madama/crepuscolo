package net.etalia.crepuscolo.json;

public class DontModifyException extends RuntimeException {

	private Object entity;

	public DontModifyException(Object entity) {
		this.entity = entity;
	}

	public void setEntity(Object entity) {
		this.entity = entity;
	}
	public Object getEntity() {
		return entity;
	}

}
