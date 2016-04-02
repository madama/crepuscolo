package net.etalia.crepuscolo.queue;

import net.etalia.crepuscolo.domain.Jsonable;

public class WrapDummy implements Jsonable {

	private String name;
	private Jsonable wrapped;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Jsonable getWrapped() {
		return wrapped;
	}
	public void setWrapped(Jsonable wrapped) {
		this.wrapped = wrapped;
	}

}
