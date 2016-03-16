package net.etalia.crepuscolo.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.utils.HttpException;

public class ValidationFailedException extends HttpException {

	public ValidationFailedException(Set<ConstraintViolation<BaseEntity>> violations) {
		statusCode(500);
		message("VALIDATIONFAIL");
		property("validation", violationsToProperties(violations));
	}

	private static Map<String, Object> violationsToProperties(Set<ConstraintViolation<BaseEntity>> violations) {
		Map<String,Object> ret = new HashMap<String, Object>();
		for (ConstraintViolation<BaseEntity> viol : violations) {
			ret.put(viol.getPropertyPath().toString(), viol.getMessageTemplate());
		}
		return ret;
	}

}
