package net.etalia.crepuscolo.auth;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.etalia.crepuscolo.codec.Token;
import net.etalia.crepuscolo.codec.Token.TokenReader;
import net.etalia.crepuscolo.utils.Beans;
import net.etalia.crepuscolo.utils.Strings;


/**
 * Creates and parses auth tokens.
 * <p>
 * Auth tokens are creating using {@link Token} class, and use following format :
 * <ol>
 *   <li>UserId, string, not mandatory (may be blank) if a systemid is specified</li>
 *   <li>Timestamp, seconds till epoch, mandatory</li>
 *   <li>SystemId, string, not mandatory (may be blank or absent) is a userId is specified</li>
 *   <li>md5 sum with salt, as specified in {@link Token}</li>
 * </ol>
 * </p>
 * <p>
 * A simple token will contain a userid and a timestamp. That is already enough to verify, together
 * with the md5 salted signature, that the token is valid and retrieve the user. The timestamp
 * will later be used in {@link AuthServiceImpl} to check validity depending on the desired 
 * verification level.
 * </p>
 * <p>
 * Another scenario is when an internal system is calling another internal system. In that case
 * the systemid token will be populated. If a userid is specified, then the system is asking to pretend
 * to be that user, and will be authenticated as that user with maximum level. If a userid is not
 * specified, then the system is simply authenticating itself to the other service.
 * </p>
 * <p>
 * Since systemid is so powerful and is intended to be used only inside the lan/vpn of the datacenter,
 * it is considered valid only if :
 * <ul>
 *   <li>The md5 is valid using a different salt than the "user only" tokens</li>
 *   <li>The connection was received on a valid port, a port which should blocked at firewall level and
 *   accessible only from internal services (see {@link AuthFilter})</li>
 * </ul>
 * </p>
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
public class AuthData {

	public static String STATIC_SALT;
	public static String AUTHENTICATION_SIGN;

	protected Log log = LogFactory.getLog(AuthData.class);

	public enum Fields {
		UserId("ui"),
		TimeStamp("ts"),
		Https("hs"),
		SystemId("sid"),
		UserPassword("up"),
		Forced("f");
		
		private String fname;

		private Fields(String fname) {
			this.fname = fname;
		}
		
		public String fieldName() {
			return fname;
		}
	}

	private Map<String,Object> incoming = new HashMap<String, Object>();
	private String currentToken;

	public AuthData(String header, boolean validPort, boolean https) {
		log.debug("Authenticating with " + header + " validPort:" + validPort + " https:" + https);
		if (header != null) {
			this.currentToken = header;
			if (header.startsWith(AUTHENTICATION_SIGN + " ")) header = header.substring(7);
			TokenReader parser = Token.parse(header);
			incoming.put(Fields.UserId.fname, Strings.nullIfBlank(parser.getString()));
			incoming.put(Fields.TimeStamp.fname, parser.getLong());
			incoming.put(Fields.SystemId.fname, Strings.nullIfBlank(parser.getString()));
			incoming.put(Fields.UserPassword.fname, Strings.nullIfBlank(parser.getString()));
			parser.checkValid(STATIC_SALT);
		}
		// Put https
		incoming.put(Fields.Https.fname, https);
		
		log.debug("Auth data : " + incoming);
		
		// Check systemid only on valid ports
		if (incoming.get(Fields.SystemId.fname) != null) {
			if (!validPort) {
				throw new IllegalArgumentException("Cannot support systemId on non valid port");
			}
			// TODO check token timestamp validity?
			
			// Setup timestamp to pretend the user is always valid
			incoming.put(Fields.TimeStamp.fname, System.currentTimeMillis());
		}
	}

	public String getString(Fields field) {
		return (String)incoming.get(field.fname);
	}

	public Boolean getBoolean(Fields field) {
		return (Boolean)incoming.get(field.fname);
	}

	public Long getLong(Fields field) {
		return (Long)incoming.get(field.fname);
	}

	public String getUserId() {
		return getString(Fields.UserId);
	}

	public void forceUserId(String userid) {
		incoming.put(Fields.UserId.fname, userid);
		incoming.put(Fields.TimeStamp.fname, System.currentTimeMillis());
		incoming.put(Fields.Forced.fname, true);
	}

	public String getSystemId() {
		return getString(Fields.SystemId);
	}

	public String getUserPassword() {
		return getString(Fields.UserPassword);
	}

	public boolean isForced() {
		return Beans.has(getBoolean(Fields.Forced));
	}

	public long getTimeStamp() {
		Long ts = getLong(Fields.TimeStamp);
		if (ts == null) return 0;
		return ts * 1000;
	}

	public static String produce(String userId, String userPassword, String systemId) {
		return AUTHENTICATION_SIGN + " " + 
		Token.create()
		.addString(userId)
		.addLong(System.currentTimeMillis() / 1000)
		.addString(systemId)
		.addString(userPassword)
		.addSalt(STATIC_SALT)
		.encode();
	}

	public static String produceForUser(String userId, String userPassword) {
		return produce(userId, userPassword, "");
	}

	public static String produceForSystem(String systemId, String asUserId) {
		if (asUserId == null) asUserId = "";
		return produce(asUserId, "", systemId);
	}

	public String getCurrentToken() {
		return currentToken;
	}

}
