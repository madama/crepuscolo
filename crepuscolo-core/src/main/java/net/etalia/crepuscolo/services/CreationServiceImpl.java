package net.etalia.crepuscolo.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import net.etalia.crepuscolo.domain.Entities;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.ID;
import net.etalia.crepuscolo.utils.Check;

public class CreationServiceImpl implements CreationService {

	private Map<Class<?>,Constructor<?>> constructors = new HashMap<Class<?>, Constructor<?>>();

	@Override
	public <T extends BaseEntity> T newInstance(Class<T> clazz) {
		@SuppressWarnings("unchecked")
		Constructor<T> constructor = (Constructor<T>) constructors.get(clazz);
		if (constructor == null) {
			try {
				constructor = clazz.getDeclaredConstructor();
				if (!Modifier.isPublic(constructor.getModifiers()))
					constructor.setAccessible(true);
			} catch (Exception e) {
				throw new IllegalStateException("Error while getting constructor for class " + clazz.getName(), e);
			}
			constructors.put(clazz, constructor);
		}
		try {
			return constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Error instantiating class " + clazz.getName() + " using " + constructor.toGenericString(), e);
		}
	}

	@Override
	public void assignId(BaseEntity obj) {
		obj.setId(ID.create(obj.getClass()).toString());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BaseEntity> T getEmptyInstance(String id) {
		Class<T> type = Entities.getDomainClassByID(id);
		Check.illegalargument.assertNotNull("Unknown type for prefix " + Entities.getPrefix(type) + " on id " + id, type);
		T obj = newInstance(type);
		obj.setId(id);
		return obj;
	}

}
