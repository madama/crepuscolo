package net.etalia.crepuscolo.domain;

import java.util.UUID;

import net.etalia.crepuscolo.utils.Check;

public class ID {

	private final String id;

	public ID(String id) {
		this.id = Check.notNull(id);
	}

	public static ID of(String id) {
		return new ID(id);
	}

	public static ID create(Class<? extends Entity> clazz) {
		String id = innerIdCreate(clazz);
		return new ID(id);
	}

	private static String innerIdCreate(Class<? extends Entity> clazz) {
		String id = Entities.getPrefix(clazz) + UUID.randomUUID().toString();
		return id;
	}

	@Override
	public String toString() {
		return (String)id;
	}

}
