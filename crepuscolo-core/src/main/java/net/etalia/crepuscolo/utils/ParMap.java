package net.etalia.crepuscolo.utils;

public class ParMap extends ChainMap<Object> {

	private static final long serialVersionUID = -5215567805355798049L;

	public ParMap() {
	}

	public ParMap(String name, Object value) {
		this.put(name, value);
	}

	public ParMap(Object... vals) {
		this.add(vals);
	}

	@Override
	public ParMap add(String name, Object value) {
		this.put(name, value);
		return this;
	}

	public ParMap add(Object... vals) {
		for (int i = 0; i < vals.length; i+=2) {
			this.put((String)vals[i], vals[i+1]);
		}
		return this;
	}

}
