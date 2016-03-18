package net.etalia.crepuscolo.auth;

import java.util.Map;

public interface RegexpTransformer {

	public String getName();

	public String[] getRegexp(Map<String, Object> attrs);

}
