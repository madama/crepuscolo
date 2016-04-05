package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class DigitsRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "digits";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] { "^[0-9]{0," + attrs.get("integer") + "}[\\.,]{1}[0-9]{0," + attrs.get("fraction") + "}$", ""};
	}

}
