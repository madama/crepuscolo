package net.etalia.crepuscolo.utils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element is not supposed to be null in any case. 
 * 
 * When used on a parameter, it indicates a parameter that cannot be null or an error will occur.
 * 
 * When used on a method, it indicates a method that will never return null.
 * 
 * When used on a field, it indicates a field that is not supposed to contain null in any case.
 * 
 * This annotation will not, obviously, enforce a check, but simply give a hint to the developer.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface NotNullable {
}