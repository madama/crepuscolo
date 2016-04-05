package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class LengthValidatorRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "length";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] {"^$|^[\\s\\S]{" + attrs.get("min") + "," + attrs.get("max") + "}$", ""};
	}

}
