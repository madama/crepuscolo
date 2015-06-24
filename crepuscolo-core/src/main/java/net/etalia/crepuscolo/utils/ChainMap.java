package net.etalia.crepuscolo.utils;

import java.util.HashMap;
import java.util.Map;

public class ChainMap<X> extends HashMap<String, X> implements Map<String,X> {

	private static final long serialVersionUID = 4512530980821184926L;

	public ChainMap() {
	}

	public ChainMap(String name, X value) {
		this.put(name, value);
	}

	public ChainMap<X> add(String name, X value) {
		this.put(name, value);
		return this;
	}

}
