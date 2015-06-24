package net.etalia.crepuscolo.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import net.etalia.crepuscolo.domain.Entity;
import net.etalia.crepuscolo.utils.HttpException;

public class ValidationFailedException extends HttpException {

	public ValidationFailedException(Set<ConstraintViolation<Entity>> violations) {
		statusCode(500);
		message("VALIDATIONFAIL");
		property("validation", violationsToProperties(violations));
	}

	private static Map<String, Object> violationsToProperties(Set<ConstraintViolation<Entity>> violations) {
		Map<String,Object> ret = new HashMap<String, Object>();
		for (ConstraintViolation<Entity> viol : violations) {
			ret.put(viol.getPropertyPath().toString(), viol.getMessageTemplate());
		}
		return ret;
	}

}
