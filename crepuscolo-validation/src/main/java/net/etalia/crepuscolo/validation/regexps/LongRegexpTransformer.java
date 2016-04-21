package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class LongRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "Long";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] { "^(-)?[0-9]*$" , ""};
	}

}
