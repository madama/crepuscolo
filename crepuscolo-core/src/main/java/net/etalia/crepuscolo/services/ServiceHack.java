package net.etalia.crepuscolo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;

@Configurable
public class ServiceHack {

	@Autowired
	private ApplicationContext appctx;

	private static ServiceHack instance = null;

	public static ServiceHack getInstance() {
		if (instance == null) {
			instance = new ServiceHack();
		}
		return instance;
	}

	@Autowired(required=false)
	private StorageService storageService;

	@Autowired(required=false)
	private CreationService creationService;

	@Autowired(required=false)
	private AuthService authService;

	@Deprecated
	public StorageService getStorageService() {
		return storageService;
	}

	@Deprecated
	public CreationService getCreationService() {
		return creationService;
	}

	@Deprecated
	public AuthService getAuthService() {
		return authService;
	}

	public <T> T getBean(Class<T> clazz) {
		if (appctx == null) return null;
		return appctx.getBean(clazz);
	}

	public Object getBean(String name) {
		if (appctx == null) return null;
		return appctx.getBean(name);
	}

}
