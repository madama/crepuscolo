package net.etalia.crepuscolo.services;

import java.util.List;
import java.util.Map;

import net.etalia.crepuscolo.domain.BaseEntity;

public interface StorageService {

	public <T extends BaseEntity> T load(Class<T> clz, String id);

	public <T extends BaseEntity> T load(Class<T> clz, Map<String, ?> andCriteria);

	public <T extends BaseEntity> T load(String query, Map<String, ?> criteria);

	public <T extends BaseEntity> T loadStored(Class<T> clz, String id);
	
	public <T extends BaseEntity> T save(T obj);

	public <T extends BaseEntity> T save(T obj, boolean andFlush);
	
	public <T extends BaseEntity> List<T> list(String query);

	public <T extends BaseEntity> List<T> list(String query, Map<String, ?> criteria);
	
	public <T extends BaseEntity> List<T> list(String query, Map<String, ?> criteria, Integer offset, Integer length);

	public <T extends Object> T rawQuery(String query, Map<String, ?> criteria);

	public List<? extends Object> rawQueryList(String query, Map<String, ?> criteria);

	public List<? extends Object> rawQueryList(String query, Map<String, ?> criteria, Integer offset, Integer length);
	
	public int rawUpdate(String query, Map<String, ?> criteria);

	public <T extends Object> T nativeQuery(String query, Map<String, ?> criteria);

	public List<? extends Object> nativeQueryList(String query, Map<String, ?> criteria);

	public void delete(BaseEntity obj);
	
	public <T extends BaseEntity> boolean delete(Class<T> clazz, String id);

	public boolean isPersisted(BaseEntity object);



}
