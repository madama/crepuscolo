package net.etalia.crepuscolo.services;

import java.util.Set;

import javax.validation.ConstraintViolation;

import net.etalia.crepuscolo.domain.Entity;

public interface ValidationService {

	public boolean isValid(Entity object, Class<?>... groups);

	public Set<ConstraintViolation<Entity>> validate(Entity object, Class<?>... groups);

}
