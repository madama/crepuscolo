package net.etalia.crepuscolo.services;

import net.etalia.crepuscolo.domain.Authenticable;


public interface AuthService {

	/**
	 * Define the verification levels. 
	 */
	public enum Verification {
		/**
		 * Does no validation at all, not even formal one, it's basically useless.
		 */
		NONE,
		/**
		 * The logged verification level is the simplest one, it simply grants that auth infos are
		 * formally valid and not expired.
		 */
		LOGGED,
		/**
		 * Disposal enforce a database validation of password validity and user validity.
		 */
		DISPOSAL,
		/**
		 * Vital enforces all the checks in "disposal", plus forces a password rehash invalidating all
		 * current tokens around for that user.
		 */
		VITAL
	}

	/**
	 * Retrieves the userId specified in auth headers, performing the checks specified by the level.
	 * 
	 * Note that to perform {@link Verification#DISPOSAL} and {@link Verification#VITAL} checks
	 * the service implementation may go thru the database or perform a server to server call to
	 * another API.
	 * 
	 * @param level The verification level to achieve.
	 * @return The userId
	 * @throws Http 401 exceptions if current validation headers doesn't meet the required verification.
	 */
	public String getPrincipalUserId(Verification level);

	/**
	 * Retrieves the systemId of the call. The system id is always securely validated.
	 * @param level The verification level to achieve.
	 * @return The system id name, or null if the call is from an external client.
	 */
	public String getSystemId(Verification level);

	/**
	 * Same as {@link #getPrincipalUserId(Verification)} but also fetches the User object.
	 * @param level The verification level to achieve.
	 * @return The userId
	 * @throws Http 401 exceptions if current validation headers doesn't meet the required verification.
	 */
	public Authenticable getPrincipalUser(Verification level);

	/**
	 * Retrieves the profileId specified in auth headers.
	 * 
	 * The verification level applies as if {@link #getPrincipalUserId(Verification)} is called.
	 * 
	 * @param level
	 * @return
	 */
	public String getPrincipalProfileId(Verification level);

	/**
	 * Used internally to pretend to be a user before calling a method that relies on this service
	 * to know which user to apply a certain procedure.
	 * @param userId the userId of a user to pretend to be, will replace current principal user id if present
	 */
	public void setPrincipalUserId(String userId);

	/**
	 * "Hides" a password hashing it with SHA1 with a salt.
	 * <p>
	 * The input <code>fromWeb</code> should be a base64 representation of
	 * the password, preferably an MD5 of the password to avoid eavesdroppers.
	 * </p><p>
	 * The output will be a base64 encoded SHA1 of the given input base64 + a random salt,
	 * followed by a column and the base64 of the salt itself. 
	 * </p><p>
	 * The returned string can be stored and used later, together with the same 
	 * <code>fromWeb</code> input stream, to check password validity in the
	 * {@link #verifyPassword(String, String)} method.
	 * </p><p>
	 * If the given stirng is already in the right format, nothing is done and
	 * the same string is simply returned, so that this method can be called
	 * multiple times without accumulating outputs.
	 * </p>
	 * 
	 * @param fromWeb A base64, preferably MD5, representation of the password.
	 * @return A salt protected hash that can be used in {@link #verifyPassword(String, String)} to
	 * check password validity.
	 */
	public String hidePassword(String fromWeb);

	/**
	 * Verifies if the given <code>fromWeb</code> base64 string is the same as the given
	 * <code>fromDb</code> hidden password.
	 * <p>
	 * The input <code>fromWeb</code> should be a base64 representation of
	 * the password, preferably an MD5 of the password to avoid eavesdroppers.
	 * </p><p>
	 * The input <code>fromDb</code> must be a string produced by {@link #hidePassword(String)},
	 * previously stored in the DB.
	 * <p>
	 * 
	 * @param fromDb The hidden password produced by {@link #hidePassword(String)} and stored on the DB 
	 * @param fromWeb The base64 representation of a password, preferably MD5 of it.
	 * @return true if the password is verified, false otherwise
	 */
	public boolean verifyPassword(String fromDb, String fromWeb);

	/**
	 * Creates a representation of the password suitable for inclusion in the token. Given
	 * the same user, the same representation will be given as long as the salt is not renewed.
	 * <p>
	 * Renewing the salt will cause the old tokens to be unusable, so make replay attacks impossible.
	 * </p> 
	 * @param user The user to compute the password representation
	 * @param renewSalt Whether to renew the salt 
	 * @return A password representation to use in the token
	 */
	public String tokenizePassword(Authenticable user, boolean renewSalt);

	/**
	 * Verifies the given token for the given level.
	 * 
	 * The semantics of this method are the same of {@link #getPrincipalUserId(Verification)}, except
	 * that it uses the given token instead of the one got from the headers.
	 * 
	 * @param token The token to verify, can be token only or full header
	 * @param validPort If the connection arrived on a valid, protected port
	 * @param https If the connection arrived over encrypted channel
	 * @param level The verification level required
	 * @return The userId
	 * @throws Http 401 exceptions if current validation headers doesn't meet the required verification.
	 */
	public String verifyToken(String token, boolean validPort, boolean https, Verification level);

}
