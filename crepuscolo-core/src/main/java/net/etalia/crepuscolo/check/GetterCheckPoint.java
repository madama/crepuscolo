package net.etalia.crepuscolo.check;

import java.lang.reflect.Method;

import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.AuthService.Verification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class GetterCheckPoint implements CheckPoint {

	private static final Object[] EMPTY = new Object[0];

	private Object bean;
	private Method getter;

	@Autowired(required=true)
	private AuthService authService = null;
	
	public void setAuthService(AuthService authService) {
		this.authService = authService;
	}

	public GetterCheckPoint(Object bean, Method getter) {
		this.bean = bean;
		this.getter = getter;
	}

	@Override
	public String getAuthenticableId(Verification level) {
		if (CheckAspect.aspectOf().isNullAuth()) return null;
		return authService.getPrincipalUserId(level);
	}

	@Override
	public Authenticable getAuthenticable(Verification level) {
		if (CheckAspect.aspectOf().isNullAuth()) return null;
		return authService.getPrincipalUser(level);
	}

	@Override
	public Object getInstance() {
		return bean;
	}

	@Override
	public Method getMethod() {
		return getter;
	}

	@Override
	public Object[] getParameters() {
		return EMPTY;
	}

	@Override
	public boolean isInMode(String mode) {
		return CheckAspect.aspectOf().isInMode(mode);
	}

}
