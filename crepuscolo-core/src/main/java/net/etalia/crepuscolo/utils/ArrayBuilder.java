package net.etalia.crepuscolo.utils;

import org.apache.commons.lang3.ArrayUtils;

public class ArrayBuilder {

	private Object[] array;

	public ArrayBuilder() {
		clear();
	}

	public void clear() {
		array = new Object[] {};
	}

	public void add(Object obj) {
		array = ArrayUtils.add(array, obj);
	}

	public Object[] getArray() {
		return array;
	}

}
