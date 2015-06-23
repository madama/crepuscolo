package net.etalia.crepuscolo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class ServiceHack {

	private static ServiceHack instance = null;

	public static ServiceHack getInstance() {
		if (instance == null) {
			instance = new ServiceHack();
		}
		return instance;
	}

	public static ServiceHack createNewInstance() {
		instance = new ServiceHack();
		return instance;
	}

	@Autowired(required=false)
	private StorageService storageService;

	@Autowired(required=false)
	private CreationService creationService;

	@Autowired(required=false)
	private AuthService authService;

	public StorageService getStorageService() {
		return storageService;
	}

	public CreationService getCreationService() {
		return creationService;
	}

	public AuthService getAuthService() {
		return authService;
	}

}
