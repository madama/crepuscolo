package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class EmailValidatorRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "email";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] {"^$|^[a-zA-Z0-9._+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$","i"};
	}

}
