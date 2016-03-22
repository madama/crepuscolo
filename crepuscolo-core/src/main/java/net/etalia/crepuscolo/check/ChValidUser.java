package net.etalia.crepuscolo.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.etalia.crepuscolo.services.AuthService.Verification;

/**
 * Checks if there is a valid user logged in, depending on {@link Verification} level.
 * <p>
 * Cannot be applied to parameters, but only on methods.
 * </p>
 * <p>
 * If verification level is {@link Verification#NONE} it never fails, otherwise
 * it fails based on {@link Verification} level checks. 
 * </p>
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@CheckerAnnotation()//value=ChValidUserChecker.class)
public @interface ChValidUser {

	Verification value() default Verification.LOGGED;
	
}
