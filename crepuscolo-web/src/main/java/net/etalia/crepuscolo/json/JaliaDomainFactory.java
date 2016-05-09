package net.etalia.crepuscolo.json;

import java.util.HashMap;
import java.util.Map;

import net.etalia.crepuscolo.check.CheckerFactory;
import net.etalia.crepuscolo.cleaning.BeanStringCleaner;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.PaginationList;
import net.etalia.crepuscolo.services.CreationService;
import net.etalia.crepuscolo.services.CreationServiceImpl;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.crepuscolo.utils.HibernateUnwrapper;
import net.etalia.jalia.BeanJsonDeSer;
import net.etalia.jalia.EntityFactory;
import net.etalia.jalia.EntityNameProvider;
import net.etalia.jalia.JsonClassData;
import net.etalia.jalia.JsonClassDataFactory;
import net.etalia.jalia.JsonClassDataFactoryImpl;
import net.etalia.jalia.JsonContext;
import net.etalia.jalia.OutField;
import net.etalia.jalia.TypeUtil;

public class JaliaDomainFactory implements EntityNameProvider, EntityFactory, JsonClassDataFactory {

	protected JsonClassDataFactoryImpl classDataFactory = new JsonClassDataFactoryImpl();
	protected Map<Class<?>, String> nameMappingsByClass = new HashMap<>();
	protected Map<String, Class<?>> nameMappingsByName = new HashMap<>();

	protected StorageService storageService;
	protected CreationService creationService = new CreationServiceImpl();
	protected CheckerFactory checkerFactory;
	protected boolean client = false;

	public void map(Class<?> clazz) {
		map(clazz,clazz.getSimpleName());
	}

	public void map(Class<?> clazz, String name) {
		nameMappingsByClass.put(clazz, name);
		nameMappingsByName.put(name, clazz);
	}

	@Override
	public String getEntityName(Class<?> clazz) {
		String ret = nameMappingsByClass.get(clazz);
		if (ret == null) {
			// TODO parse an annotation?
		}
		return ret;
	}

	@Override
	public Class<?> getEntityClass(String name) {
		return nameMappingsByName.get(name);
	}

	public void setStorageService(StorageService storageService) {
		this.storageService = storageService;
	}
	public void setCreationService(CreationService creationService) {
		this.creationService = creationService;
	}
	public void setCheckerFactory(CheckerFactory checkerFactory) {
		this.checkerFactory = checkerFactory;
	}
	public void setClient(boolean client) {
		this.client = client;
	}

	/**
	 * Avoids direct "id" manipulation and replaces with {@link CheckersJsonClassData}
	 */
	@Override
	public JsonClassData getClassData(Class<?> clazz, JsonContext context) {
		JsonClassData jcd = classDataFactory.getClassData(clazz, context);
		if (!jcd.isNew()) return jcd;
		CheckersJsonClassData ret = new CheckersJsonClassData(jcd, checkerFactory, this.client);
		ret.ignoreGetter("id");
		ret.ignoreSetter("id");
		if (this.client)
			ret.clearDefaults();
		classDataFactory.cache(clazz, ret);
		return ret;
	}

	/**
	 * Uses {@link Persistent#getId()} to fetch the id.
	 */
	@Override
	public String getId(Object entity, JsonContext context) {
		if (entity instanceof BaseEntity)
			 return ((BaseEntity) entity).getId();
		JsonClassData jcd = getClassData(entity.getClass(), context);
		if (jcd == null) return null;
		Object value = jcd.getValue("id", entity, true);
		if (value != null) return value.toString();
		return null;
	}

	/**
	 * Loads from {@link StorageService} if given, or uses {@link CreationService} to create new entities
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object buildEntity(Class<?> clazz, String id, JsonContext context) {
		if (BaseEntity.class.isAssignableFrom(clazz)) {
			BaseEntity ret = null;
			if (storageService != null && id != null)
				ret = storageService.load((Class<? extends BaseEntity>)clazz, id);
			if (ret != null) return ret;
			ret = creationService.newInstance((Class<? extends BaseEntity>)clazz);
			if (id != null) ret.setId(id);
			return ret;
		}
		Object ret = TypeUtil.get(clazz).newInstance();
		JsonClassData jcd = getClassData(clazz, context);
		if (jcd != null) {
			jcd.setValue("id", id, ret, true);
			return ret;
		}
		return null;
	}

	/**
	 * When serializing, unraps Hibernate proxies using {@link HibernateUnwrapper}
	 */
	@Override
	public Object prepare(Object obj, boolean serializing, JsonContext context) {
		if (serializing) {
			obj = HibernateUnwrapper.unwrap(obj);
			// Pagination lists were skipped during previous outfield system, so we need this hack
			OutField fields = context.getCurrentFields();
			if (obj instanceof PaginationList) {
				if (fields != null && fields.hasSubs()) {
					fields.reparentSubs("data");
					fields.getCreateSub("pagination.*");
				}
			}
		}
		if (!serializing && !context.isRoot() && storageService != null && obj instanceof BaseEntity) {
			if (!context.getFromStackBoolean(BeanJsonDeSer.REUSE_WITHOUT_ID)) {
				if (storageService.isPersisted((BaseEntity)obj)) {
					throw new DontModifyException(obj);
				}
			}
		}
		return obj;
	}

	/**
	 * When deserializing, applies {@link BeanStringCleaner}
	 */
	@Override
	public Object finish(Object obj, boolean serializing, JsonContext context) {
		if (!serializing) {
			BeanStringCleaner.cleanBean(obj);
		}
		return obj;
	}

}
