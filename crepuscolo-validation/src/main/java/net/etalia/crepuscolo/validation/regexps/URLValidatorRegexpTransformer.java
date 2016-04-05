package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;
import net.etalia.crepuscolo.validation.ValidationRegexp;

public class URLValidatorRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "url";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] {ValidationRegexp.URL, "i"};
	}

}
