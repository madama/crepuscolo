package net.etalia.crepuscolo.check;

import net.etalia.crepuscolo.services.AuthService.Verification;

public class ChValidUserChecker implements Checker {

	private Verification level;
	
	public void setValue(Verification level) {
		this.level = level;
	}
	
	@Override
	public int check(CheckPoint p) {
		if (p.getAuthenticableId(level) == null) return 403;
		return 0;
	}

}
