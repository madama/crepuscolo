package net.etalia.crepuscolo.services;

import java.util.List;
import java.util.Map;

import net.etalia.crepuscolo.domain.Entity;

public interface StorageService {

	public <T extends Entity> T load(Class<T> clz, String id);

	public <T extends Entity> T load(String query, Map<String, ?> criteria);

	public <T extends Entity> T loadStored(Class<T> clz, String id);
	
	public <T extends Entity> T save(T obj);

	public <T extends Entity> T save(T obj, boolean andFlush);
	
	public <T extends Entity> List<T> list(String query);

	public <T extends Entity> List<T> list(String query, Map<String, ?> criteria);
	
	public <T extends Entity> List<T> list(String query, Map<String, ?> criteria, Integer offset, Integer length);

	public <T extends Object> T rawQuery(String query, Map<String, ?> criteria);

	public List<? extends Object> rawQueryList(String query, Map<String, ?> criteria);

	public List<? extends Object> rawQueryList(String query, Map<String, ?> criteria, Integer offset, Integer length);
	
	public int rawUpdate(String query, Map<String, ?> criteria);

	public <T extends Object> T nativeQuery(String query, Map<String, ?> criteria);

	public List<? extends Object> nativeQueryList(String query, Map<String, ?> criteria);

	public void delete(Entity obj);
	
	public <T extends Entity> boolean delete(Class<T> clazz, String id);

	public boolean isPersisted(Entity object);



}
