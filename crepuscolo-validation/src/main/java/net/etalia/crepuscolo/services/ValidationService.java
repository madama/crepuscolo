package net.etalia.crepuscolo.services;

import java.util.Set;

import javax.validation.ConstraintViolation;

import net.etalia.crepuscolo.domain.BaseEntity;

public interface ValidationService {

	public boolean isValid(BaseEntity object, Class<?>... groups);

	public Set<ConstraintViolation<BaseEntity>> validate(BaseEntity object, Class<?>... groups);

}
