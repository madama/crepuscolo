package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class intRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "int";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] { "^(-)?[0-9]*$" , ""};
	}

}
