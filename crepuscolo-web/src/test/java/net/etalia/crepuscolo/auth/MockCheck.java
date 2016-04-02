package net.etalia.crepuscolo.auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import net.etalia.crepuscolo.check.CheckerAnnotation;

@CheckerAnnotation()
@Retention(RetentionPolicy.RUNTIME)
public @interface MockCheck {

	String testValue() default "";

}
