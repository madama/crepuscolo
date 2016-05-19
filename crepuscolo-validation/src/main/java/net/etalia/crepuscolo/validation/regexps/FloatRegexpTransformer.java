package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class FloatRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "Float";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		return new String[] { "^(-)?[0-9]*(\\.|\\,)?[0-9]*$" , ""};
	}

}
