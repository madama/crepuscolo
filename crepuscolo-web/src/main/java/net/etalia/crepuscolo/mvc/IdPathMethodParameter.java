package net.etalia.crepuscolo.mvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestBody;

public class IdPathMethodParameter extends MethodParameter {

	private static RequestBody MOCK_ANN = null;

	static {
		try {
			Method meth = IdPathMethodParameter.class.getMethod("mock_ann_method", String.class);
			MOCK_ANN = (RequestBody) meth.getParameterAnnotations()[0][0];
		} catch (Exception e) {
			throw new IllegalStateException("Cannot fetch mock RequestBody annotation", e);
		}
	}

	public void mock_ann_method(@RequestBody String mock_param) {
		System.out.println("Useless method");
	}

	public IdPathMethodParameter(MethodParameter original) {
		super(original);
	}

	@Override
	public <T extends Annotation> T getParameterAnnotation(Class<T> annotationType) {
		T ret = super.getParameterAnnotation(annotationType);
		if (ret == null && RequestBody.class.equals(annotationType)) {
			return (T) MOCK_ANN;
		}
		return ret;
	}

}
