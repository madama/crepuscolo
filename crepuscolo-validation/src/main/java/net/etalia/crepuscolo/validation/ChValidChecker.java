package net.etalia.crepuscolo.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import net.etalia.crepuscolo.check.CheckPoint;
import net.etalia.crepuscolo.check.Checker;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.services.ValidationFailedException;
import net.etalia.crepuscolo.services.ValidationService;

@Configurable(autowire=Autowire.BY_TYPE)
public class ChValidChecker implements Checker {

	@Autowired(required=true)
	private ValidationService validationService = null;
	
	private Class<?>[] value = null;
	
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}
	
	public void setValue(Class<?>[] value) {
		this.value = value;
	}
	
	@Override
	public int check(CheckPoint p) {
		Object inst = p.getInstance();
		if (inst == null) return 0;
		if (!(inst instanceof BaseEntity)) return 500;
		Set<ConstraintViolation<BaseEntity>> viols = validationService.validate((BaseEntity)inst, this.value);
		if (viols.isEmpty()) return 0;
		throw new ValidationFailedException(viols);
	}

}
