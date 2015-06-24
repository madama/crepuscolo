package net.etalia.crepuscolo.services;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import net.etalia.crepuscolo.domain.Entity;

public class ValidationServiceImpl implements ValidationService {

	protected Validator validator;

	public void init() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Override
	public boolean isValid(Entity object, Class<?>... groups) {
		return validate(object, groups).isEmpty();
	}

	@Override
	public Set<ConstraintViolation<Entity>> validate(Entity object, Class<?>... groups) {
		return validator.validate(object, groups);
	}

}
