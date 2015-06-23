package net.etalia.crepuscolo.utils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element is thread safe.
 * 
 * When used on a method, that method is thread safe.
 * 
 * When used on a class, all methods in that class (and the class itself, with respect to access to fields etc..) are thread safe.
 * 
 * This annotation will not, obviously, enforce a check, but simply give a hint to the developer.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ThreadSafe {

}
