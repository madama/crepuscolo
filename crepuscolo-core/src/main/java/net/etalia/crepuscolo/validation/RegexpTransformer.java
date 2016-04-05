package net.etalia.crepuscolo.validation;

import java.util.Map;

public interface RegexpTransformer {

	public String getName();

	public String[] getRegexp(Map<String, Object> attrs);

}
