package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class NotNullRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "notnull";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] {".+",""};
	}

}
