package net.etalia.crepuscolo.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.ServiceHack;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonSetter;

@MappedSuperclass
@JsonDefaultFields(value="id,extraData")
public class BaseEntity implements Jsonable {

	String id;
	long version;

	@Transient
	private Map<String, Object> extraData;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Version
	@JsonIgnore
	public long getVersion() {
		return this.version;
	}
	protected void setVersion(long version) {
		this.version = version;
	}

	@Transient
	@JsonIgnore
	protected StorageService getStorageService() {
		return ServiceHack.getInstance().getStorageService();
	}

	@Transient
	@JsonIgnore
	protected AuthService getAuthService() {
		return ServiceHack.getInstance().getAuthService();
	}

	/**
	 * Gets additional data connected to the bean.
	 * 
	 * <p>
	 * Additional datas are not persisted on the DB; but are transmitted over
	 * to the other party when de/serializing from/to JSON.
	 * </p>
	 * 
	 * @param <T> Since there is no type checking, a "mock" type is used. 
	 * @param name The name of the additional data element.
	 * @return The data, if any, otherwise null. There is no type check.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getExtraData(String name) {
		if (extraData == null) return null;
		return (T)extraData.get(name);
	}

	public void clearExtraData() {
		if (this.extraData == null) return;
		this.extraData.clear();
	}

	@Transient
	@JsonSetter	
	protected void setExtraData(Map<String,Object> map) {
		if (map.size() == 0) {
			this.extraData = null;
		} else {
			this.extraData = map;
		}
	}

	/**
	 * Adds (or overwrites existing) additional data connected to the bean.
	 * <p>
	 * Additional datas are not persisted on the DB; but are transmitted over
	 * to the other party when de/serializing from/to JSON.
	 * </p><p>
	 * There is no type check enforced, so everything can be placed inside additional
	 * datas. However, it's preferable to use only types that can be serialized to JSON,
	 * like :
	 * <ul>
	 *   <li>Primitive types (int, long, boolean ...) and their boxed versions (Integer, Long ...)</li>
	 *   <li>String</li>
	 *   <li>Date</li>
	 *   <li>Collections (Map, List) and arrays of the previous types</li>
	 *   <li>Other beans that the system is able to serialize</li>
	 * </ul>
	 * 
	 * @param <T> Since there is no type checking, a "mock" type is used. 
	 * @param name The name of the additional data element.
	 * @param value Data value to be set/added.
	 * @return The previous data, if any, otherwise null. 
	 */
	public <T> T setExtraData(String name, Object value) {
		if (extraData == null) extraData = new HashMap<String, Object>();
		T ret = getExtraData(name);
		extraData.put(name, value);
		return ret;
	}
	

	@SuppressWarnings("unchecked")
	public <T> T removeExtraData(String name) {
		if (extraData == null) return null;
		return (T)extraData.remove(name);
	}

	/**
	 * @return The additional data associated to this bean using {@link #setExtraData(String, Object)}, or an empty map, in both cases not modifiable.
	 */
	@Transient
	public Map<String,Object> getExtraData() {
		if (this.extraData == null) return Collections.emptyMap();
		return Collections.unmodifiableMap(this.extraData);
	}

	@Transient
	public Map<String,Object> getInternalExtraData() {
		if (this.extraData == null) return Collections.emptyMap();
		return this.extraData;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof BaseEntity)) {
			return false;
		}
		if (getId() == null || ((BaseEntity)obj).getId() == null) {
			return false;
		}
		return getId().equals(((BaseEntity)obj).getId());
	}

	@Override
	public int hashCode() {
		if (getId() == null) return super.hashCode();
		return getId().hashCode() ^ BaseEntity.class.hashCode();
	}

}
