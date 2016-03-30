package net.etalia.crepuscolo.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Entities {

	private static Map<String, Class<? extends BaseEntity>[]> classes = new HashMap<String, Class<? extends BaseEntity>[]>();
	private static int prefixLength = 1;

	private Entities() {
	}

	public static void setPrefixLength(int length) {
		//TODO: chech on empty map
		prefixLength = length;
	}

	public static void add(String prefix, Class<? extends BaseEntity> mainClazz, Class<? extends BaseEntity>... otherClasses) {
		if (prefix.trim().length() != prefixLength) {
			throw new IllegalArgumentException("Prefix length is not valid.");
		}
		if (otherClasses.length > 0) { 
			Class<? extends BaseEntity>[] lst = new Class[otherClasses.length + 1];
			lst[0] = mainClazz;
			System.arraycopy(otherClasses, 0, lst, 1, otherClasses.length);
			classes.put(prefix, lst);
		} else {
			classes.put(prefix, new Class[] {mainClazz});
		}
	}

	@SuppressWarnings("unchecked")
	public static void add(String prefix, String clazz) throws ClassNotFoundException {
		add(prefix, (Class<? extends BaseEntity>) Class.forName(clazz));
	}

	@SuppressWarnings("unchecked")
	public static <T extends BaseEntity> Class<T> getDomainClassByPrefix(String prefix) {
		if (prefix.trim().length() != prefixLength) {
			throw new IllegalArgumentException("Prefix length is not valid.");
		}
		return (Class<T>) classes.get(prefix)[0];
	}

	public static <T extends BaseEntity> Class<T> getDomainClassByID(String id) {
		return getDomainClassByPrefix(id.substring(0, prefixLength));
	}

	public static String getPrefix(Class<? extends BaseEntity> clazz) {
		String prefix = null;
		for (Entry<String, Class<? extends BaseEntity>[]> entry : classes.entrySet()) {
			for (Class<? extends BaseEntity> extendedClass : entry.getValue()) {
				if (extendedClass == clazz) {
					prefix = entry.getKey();
					break;
				}
			}
		}
		if (prefix == null) {
			throw new IllegalArgumentException("Cannot find any prefix associated with this Class");
		}
		if (prefix.trim().length() != prefixLength) {
			throw new IllegalArgumentException("Prefix length is not valid.");
		}
		return prefix;
	}

	public static String getPrefix(String id) {
		return id.substring(0, prefixLength);
	}

}
