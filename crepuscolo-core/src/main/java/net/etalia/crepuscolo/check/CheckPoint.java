package net.etalia.crepuscolo.check;

import java.lang.reflect.Method;

import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.services.AuthService.Verification;

/**
 * Gives access to context to a {@link Checker#check(CheckPoint)} method.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
public interface CheckPoint {

	/**
	 * Retrieves current {@link Authenticable} id, checking with the given {@link Verification} level.
	 * 
	 * @param level The verification level checks to perform.
	 * @return The user id, or null if no user is logged.
	 */
	public String getAuthenticableId(Verification level);

	/**
	 * Retrieves the current {@link Authenticable}, checking with the given {@link Verification} level.
	 * 
	 * @param level The verification level checks to perform.
	 * @return The {@link Authenticable}, or null is no user is logged.
	 */
	public Authenticable getAuthenticable(Verification level);

	/**
	 * Retrieves the current instance the check is being performed on.
	 * <p>
	 * When checking on a service method, this will be the instance of the service implementation,
	 * which is nearly useless. When checking on a parameter, it will return the parameter value.
	 * When checking on a bean getter or setter, it will be the instance of the bean on which
	 * the getter/setter has been called.
	 * </p>
	 * @return The current instance of the service, of the parameter, or of the bean we are checking.
	 */
	public Object getInstance();

	/**
	 * Retrieves the current method the checker is being applied on.
	 * 
	 * <p>
	 * This SHOUD NOT be used to change the checker test based on the method name. It is useful
	 * for writing highly reusable checkers, that can fetch what needed in {@link #getParameters()} 
	 * depending on the method signature, for example. 
	 * </p>
	 * 
	 * @return The {@link Method} the checker is being run on.
	 */
	public Method getMethod();

	/**
	 * Retrieves parameters passed to the method execution.
	 * 
	 * <p>
	 * Parameters can be used to fetch more objects needed to perform the check.
	 * </p>
	 * @return An array with parameters of the method call, primitives converted to wrappers.
	 */
	public Object[] getParameters();

	/**
	 * Looks for the current "mode" for auth checks. Mode is an internal state, which can be
	 * set using {@link CheckMode} annotation, and can be used to customize checkers behaviour. 
	 * @return
	 */
	public boolean isInMode(String mode);

}
