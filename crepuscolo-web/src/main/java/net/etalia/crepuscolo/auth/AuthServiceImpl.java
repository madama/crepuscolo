package net.etalia.crepuscolo.auth;

import java.security.SecureRandom;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import net.etalia.crepuscolo.codec.Base64Codec;
import net.etalia.crepuscolo.codec.Digester;
import net.etalia.crepuscolo.domain.Authenticable;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.crepuscolo.utils.HttpException;
import net.etalia.crepuscolo.utils.Strings;

@Configurable
public class AuthServiceImpl implements AuthService {

	protected Log log = LogFactory.getLog(AuthServiceImpl.class);

	@Autowired(required=false)
	private StorageService storageService;

	private SecureRandom secureRandom = new SecureRandom();

	private Class<? extends BaseEntity> userClass;

	/**
	 * Max time (in millis) for normal tokens
	 */
	private long maxTokenTime = 24l * 60l * 60000l;

	public void setMaxTokenTime(long maxTokenTime) {
		this.maxTokenTime = maxTokenTime;
	}

	public void setUserClass(Class<? extends BaseEntity> userClass) {
		if (!ArrayUtils.contains(userClass.getInterfaces(), Authenticable.class)) {
			throw new IllegalArgumentException("Given class does not implements Authenticable!");
		}
		this.userClass = userClass;
	}

	protected AuthData getAuthData() {
		return AuthFilter.getAuthData();
	}

	@Override
	public String getPrincipalUserId(Verification level) {
		return getPrincipalUserId(null, level);
	}

	protected String getPrincipalUserId(AuthData authData, Verification level) {
		if (authData == null) authData = getAuthData();
		String uid = authData.getUserId();
		log.debug("Verifying userId : " + uid);
		if (level.equals(Verification.NONE)) return uid;
		
		// If more than NONE, we need it to be specified at least
		if (Strings.nullOrBlank(uid)) {
			throw new HttpException().statusCode(HttpStatus.UNAUTHORIZED).errorCode("INVALID");
		}
		// Check for time validity
		checkTimeValidity(authData);

		if (level.equals(Verification.LOGGED)) {
			// No need to do more checks on the DB
			return uid;
		}
		// Get the user from the DB to perform all checks
		getUserInternal(authData ,level);
		return uid;
	}

	@Override
	public String getSystemId(Verification level) {
		AuthData authData = getAuthData();
		String sysid = authData.getSystemId();
		log.debug("Verifying systemId : " + sysid);
		if (level.equals(Verification.NONE)) return sysid;
		if (Strings.nullOrBlank(sysid)) {
			throw new HttpException().statusCode(HttpStatus.UNAUTHORIZED);
		}
		// Check for time validity
		checkTimeValidity(authData);

		if (level.equals(Verification.LOGGED) || authData.getSystemId() != null) {
			// No need to check on the DB
			return sysid;
		}
		// TODO what other checks for systemid ??
		return sysid;
	}

	@Transactional
	protected Authenticable getUserInternal(AuthData authData, Verification level) {
		if (authData == null) authData = getAuthData();
		String uid = authData.getUserId();
		log.debug("Loading userId : " + uid);
		
		Authenticable authenticable = null;
		if (Strings.notNullOrBlank(uid)) {
			// Load it from the storage
			if (storageService != null && userClass != null) {
				authenticable = (Authenticable) storageService.load(userClass, uid);
			//} else if (capiCaller != null) {
			//	authenticable = capiCaller.method(capiCaller.service().getUser(uid)).execute().cast();
			}
		}
		checkFor(authenticable, level);
		// If NONE, simply return whatever we have
		if (level.equals(Verification.NONE)) {
			return authenticable;
		}
		// If more than NONE, we need the user at least
		if (authenticable == null) {
			throw new HttpException().statusCode(HttpStatus.UNAUTHORIZED).errorCode("INVALID");
		}
		
		// If forced, return the user
		if (authData.isForced()) {
			return authenticable;
		}
		
		// Check for time validity
		checkTimeValidity(authData);
		
		if (level.equals(Verification.LOGGED) || authData.getSystemId() != null) {
			return authenticable;
		}
		
		// Check for password validity
		String tkpwd = tokenizePassword(authenticable, level.equals(Verification.VITAL));
		if (!authData.getUserPassword().equals(tkpwd)) {
			throw new HttpException().statusCode(HttpStatus.UNAUTHORIZED).errorCode("INVALID");
		}
		return authenticable;
	}

	@Override
	public Authenticable getPrincipalUser(Verification level) {
		return getUserInternal(null, level);
	}

	@Override
	public void setPrincipalUserId(String userId) {
		if (userId == null) {
			AuthFilter.clearAuthData();
		} else {
			AuthData ret = AuthFilter.newAuthData();
			ret.forceUserId(userId);
		}
	}

	private String hidePasswordFormatV1(String fromWeb, byte[] saltBytes) {
		// Parse the fromWeb as a base64
		Base64Codec b64 = new Base64Codec();
		byte[] decoded = b64.decodeNoGzip(fromWeb);
		
		// Put them together
		byte[] payload = new byte[decoded.length + saltBytes.length];
		System.arraycopy(decoded, 0, payload, 0, decoded.length);
		System.arraycopy(saltBytes, 0, payload, decoded.length, saltBytes.length);
		
		// Run a few rounds of SHA
		Digester digester = new Digester();
		for (int i = 0; i < 100; i++) {
			payload = digester.sha1(payload).unwrap();
		}
		
		// Encode it
		return "1/" + b64.encodeUrlSafeNoPad(payload) + ":" + b64.encodeUrlSafeNoPad(saltBytes);
	}

	@Override
	public String hidePassword(String fromWeb) {
		// Check if it already has the right format
		if (fromWeb.charAt(1) == '/' && fromWeb.indexOf(':') != -1) return fromWeb;
		// Generate a random salt
		byte[] saltBytes = new byte[20];
		secureRandom.nextBytes(saltBytes);
		return hidePasswordFormatV1(fromWeb, saltBytes);
	}

	@Override
	public boolean verifyPassword(String fromDb, String fromWeb) {
		if (fromDb.charAt(1) != '/' || fromDb.indexOf(':') == -1) throw new IllegalArgumentException("Given hidden password from DB is not in valid format");
		
		if (fromDb.startsWith("1/")) {
			// Version 1
			// Decode the salt
			String salt = fromDb.substring(fromDb.indexOf(':') + 1);
			byte[] saltBytes = new Base64Codec().decodeNoGzip(salt);
			return hidePasswordFormatV1(fromWeb, saltBytes).equals(fromDb);
		} else {
			throw new IllegalArgumentException("Version " + fromDb.charAt(0) + " of hidden password is unsupported");
		}
	}

	@Override
	public String tokenizePassword(Authenticable authenticable, boolean renewSalt) {
		if (Strings.nullOrBlank(authenticable.getPassword())) throw new HttpException().statusCode(HttpStatus.UNAUTHORIZED).errorCode("NOPWD");
		String ret = new Digester().md5(authenticable.getPassword() + authenticable.getTokenSalt()).toBase64UrlSafeNoPad().substring(0, 15);
		if (renewSalt) {
			authenticable.setTokenSalt(Long.toHexString(secureRandom.nextLong()));
		}
		return ret;
	}

	@Override
	public String verifyToken(String token, boolean validPort, boolean https, Verification level) {
		AuthData ad = new AuthData(token, validPort, https);
		return getPrincipalUserId(ad, level);
	}

	protected void checkFor(Authenticable authenticable, Verification level) {
	}

	protected void checkTimeValidity(AuthData authData) {
		if (!checkTimeValidityCondition(authData)) {
			throw new HttpException().statusCode(HttpStatus.UNAUTHORIZED).errorCode("INVALID");
		}
	}

	protected boolean checkTimeValidityCondition(AuthData authData) {
		return System.currentTimeMillis() > authData.getTimeStamp() + maxTokenTime;
	}

}
