package net.etalia.crepuscolo.cleaning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation prevents the {@link BeanStringCleaner} to apply string cleaning to a given property. 
 * 
 * This annotation must be applied on the getter.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface StringCleaning {

	boolean perform() default true;
	boolean keepNewLines() default false;

}
