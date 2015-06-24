package net.etalia.crepuscolo.mvc;

import java.util.Map;

public interface Response<T> {

	public T cast();
	
	public Map<String,Object> asMap();
	
	public int getStatusCode();
	
	public Map<String,String> getHeaders();
	
	// Getters on wrapped stuff
	
}
