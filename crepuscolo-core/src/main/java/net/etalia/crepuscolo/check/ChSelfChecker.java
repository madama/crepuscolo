package net.etalia.crepuscolo.check;

import net.etalia.crepuscolo.check.BaseStringOrBeanChecker;
import net.etalia.crepuscolo.check.CheckPoint;
import net.etalia.crepuscolo.services.AuthService.Verification;

public class ChSelfChecker extends BaseStringOrBeanChecker {

	@Override
	public int check(CheckPoint p) {
		Object obj = p.getInstance();
		String checkId = getIdOf(obj);
		String userId = p.getAuthenticableId(Verification.NONE);
		if (checkId == null || userId == null) return 403;
		return checkId.equals(userId) ? 0 : 403;
	}

}
