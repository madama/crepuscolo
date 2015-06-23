package net.etalia.crepuscolo.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.etalia.crepuscolo.check.CheckerAnnotation;
import net.etalia.crepuscolo.domain.Authenticable;

/**
 * Checks if the current user is the same of the given {@link Authenticable}.
 * <p>
 * Can be applied on any argument of type {@link Authenticable} or String, in which case
 * the string will be interpreted as a user id.
 * </p>
 * <p>
 * Will fail the check (and give UNAUTHORIZED error) if the user is null or if there is no current user.
 * </p>
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.PARAMETER})
@CheckerAnnotation()
public @interface ChSelf {

}
