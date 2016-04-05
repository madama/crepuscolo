package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class RangeRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "range";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		String min = attrs.get("min").toString();
		String max = attrs.get("max").toString();
		
		int minlen = Math.min(min.length(), max.length());
		int maxlen = Math.max(min.length(), max.length());
		
		String regexp = "^";
		if (min.startsWith("-")) {
			regexp += "-?";
		}
		regexp += "[0-9\\.\\,]{" + minlen + "," + maxlen + "}$";
		
		return new String[] { regexp, "" };
	}

}
