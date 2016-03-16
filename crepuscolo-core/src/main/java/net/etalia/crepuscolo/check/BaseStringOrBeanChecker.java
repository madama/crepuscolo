package net.etalia.crepuscolo.check;

import net.etalia.crepuscolo.domain.Entities;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.services.StorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Abstract parameter checker that can convert an id given as a string to the corresponding bean.
 * 
 */
@Configurable
public abstract class BaseStringOrBeanChecker implements Checker {

	@Autowired(required=false)
	protected StorageService storageService = null;

	public void setStorageService(StorageService storageService) {
		this.storageService = storageService;
	}

	@SuppressWarnings("unchecked")
	protected <T extends BaseEntity> T convert(Object obj) {
		if (obj == null) return null;
		if (obj instanceof String) {
			Class<T> type = Entities.getDomainClassByID((String) obj);
			if (type == null) throw new IllegalArgumentException("Cannot find type for " + obj);
			Class<?> clazz = type;
			if (!(BaseEntity.class.isAssignableFrom(clazz))) throw new IllegalArgumentException("Class " + clazz.getName() + " is not a subclass of Persistent, cannot be fetched");
			return convert(((Class<T>)clazz), obj); 
		}
		return (T)obj;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends BaseEntity> T convert(Class<T> clazz, Object obj) {
		T ret = null;
		if (obj == null) return null;
		if (obj instanceof String) {
			if (storageService == null) throw new IllegalStateException("Cannot convert a String to " + clazz.getName() + " without a StorageService");
			String id = (String) obj;
			Class<T> type = Entities.getDomainClassByID(id);
			if (type != null) {
				if (!clazz.isAssignableFrom(type)) throw new IllegalArgumentException("Was expecting " + clazz + " but id " + id + " is for " + type);
				clazz = type;
			}
			ret = storageService.load(clazz, (String)obj);
		} else if (clazz.isInstance(obj)) {
			ret = (T) obj;
		} else {
			throw new IllegalArgumentException("Checker searching of String or " + clazz.getName() + " but found " + obj + " instead");
		}
		return ret;
	}
	
	protected String getIdOf(Object obj) {
		if (obj == null) return null;
		if (obj instanceof String) return (String)obj;
		if (obj instanceof BaseEntity) return ((BaseEntity)obj).getId();
		throw new IllegalArgumentException("Cannot extract an id from " + obj);
	}
	
}
