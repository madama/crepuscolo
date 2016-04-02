package net.etalia.crepuscolo.auth;

import java.util.ArrayList;
import java.util.List;

import net.etalia.crepuscolo.check.CheckPoint;
import net.etalia.crepuscolo.check.Checker;
import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.services.AuthService.Verification;
import net.etalia.crepuscolo.utils.Beans;

public class MockCheckChecker implements Checker {

	public static boolean checkResult = true;
	public static List<String> checkFail = new ArrayList<String>();
	public static String expectUser = null;
	public static List<RecordedCheck> recorded = new ArrayList<RecordedCheck>();

	public static class RecordedCheck {
		CheckPoint checkPoint;
		MockCheckChecker checker;
	}

	public static void reset() {
		recorded.clear();
		checkFail = new ArrayList<String>();
		checkResult = true;
	}

	private String testValue;

	public void setTestValue(String testValue) {
		this.testValue = testValue;
	}
	public String getTestValue() {
		return testValue;
	}

	@Override
	public int check(CheckPoint p) {
		RecordedCheck rec = new RecordedCheck();
		rec.checkPoint = p;
		rec.checker = this;
		recorded.add(rec);
		
		if (checkResult == false) return 403;
		if (Beans.has(expectUser)) {
			Authenticable user = p.getAuthenticable(Verification.NONE);
			if (user == null) return 403;
			//if (!user.getId().equals(expectUser)) return 403;
		}
		if (p.isInMode(testValue)) return 403;
		return !checkFail.contains(testValue) ? 0 : 403;
	}

}
