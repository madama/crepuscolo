package net.etalia.crepuscolo.validation.regexps;

import java.util.Map;

import javax.validation.constraints.Pattern.Flag;

import net.etalia.crepuscolo.validation.RegexpTransformer;

public class PatternRegexpTransformer implements RegexpTransformer {

	@Override
	public String getName() {
		return "pattern";
	}

	@Override
	public String[] getRegexp(Map<String, Object> attrs) {
		String opts = "";
		Flag[] flags = (Flag[]) attrs.get("flags");
		if (flags != null) {
			for (Flag flag : flags) {
				if (flag.equals(Flag.CASE_INSENSITIVE)) opts += "i";
				if (flag.equals(Flag.MULTILINE)) opts += "m";
			}
		}
		return new String[] { (String)attrs.get("regexp"), opts };
	}

}
