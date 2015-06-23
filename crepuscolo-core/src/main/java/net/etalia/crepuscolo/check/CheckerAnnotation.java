package net.etalia.crepuscolo.check;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generic annotation to define a {@link Checker} instance.
 * 
 * <p>
 * Usually specific annotation will be used to define a specific checker instance.
 * Those specific annotations must be annotated with this one to make them recognizable.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface CheckerAnnotation {

	Class<? extends Annotation> value() default CheckerAnnotation.class;

	String[] init() default {};

}
