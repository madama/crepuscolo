package net.etalia.crepuscolo.auth;

public class MockAuthService extends AuthServiceImpl {

	private AuthData data = new AuthData(null, false, false);

	public MockAuthService useToken(String token) {
		data = new AuthData(token, true, true);
		return this;
	}

	public MockAuthService authAsUser(String uid, String password) {
		useToken(AuthData.produceForUser(uid, password));
		return this;
	}

	public MockAuthService authAsUser(String uid) {
		useToken(AuthData.produceForUser(uid, "aaa"));
		return this;
	}

	public MockAuthService authAsSystem(String systemId) {
		useToken(AuthData.produceForSystem(systemId, null));
		return this;
	}

	public MockAuthService authAsSystemAndUser(String systemId, String userId) {
		useToken(AuthData.produceForSystem(systemId, userId));
		return this;
	}

	@Override
	protected AuthData getAuthData() {
		return data;
	}

}
