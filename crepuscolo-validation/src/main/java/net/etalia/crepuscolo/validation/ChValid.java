package net.etalia.crepuscolo.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.groups.Default;

import net.etalia.crepuscolo.check.CheckerAnnotation;
import net.etalia.crepuscolo.services.ValidationService;

/**
 * Checks if the given instance or parameter value is valid, as checked by {@link ValidationService#isValid(net.etalia.domain.Persistent)}
 * <p>
 * Will ACCEPT a null value, combine it with ChNotNull if a not null check is needed.
 * </p>
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.PARAMETER})
@CheckerAnnotation()//value=ChValidChecker.class)
public @interface ChValid {

	Class<?>[] value() default { Default.class };
	
}
