package net.etalia.crepuscolo.utils;

/**
 * An exception that will cause the web MVC tier to retry the request when RetryFilter is used.
 * 
 * The number of retries and the sleep period between retries can be specified.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
public class RetryException extends RuntimeException {

	private static final long serialVersionUID = -6557814201889273524L;
	
	private int retries = 0;
	private long sleep = 100;
	
	public RetryException() {
		super();
	}

	public RetryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryException(String message) {
		super(message);
	}

	public RetryException(Throwable cause) {
		super(cause);
	}

	public RetryException(int retries, long sleep) {
		this(retries, sleep, null, null);
	}

	public RetryException(int retries, long sleep, String message, Throwable cause) {
		super(message, cause);
		this.retries = retries;
		this.sleep = sleep;
	}

	public RetryException(int retries, long sleep, String message) {
		this(retries, sleep, message, null);
	}

	public RetryException(int retries, long sleep, Throwable cause) {
		this(retries, sleep, null, cause);
	}

	public int getRetries() {
		return retries;
	}

	public long getSleep() {
		return sleep;
	}

	@SuppressWarnings("unchecked")
	public <T extends RetryException> T setRetries(int retries) {
		this.retries = retries;
		return (T)this;
	}

	@SuppressWarnings("unchecked")
	public <T extends RetryException> T setSleep(long sleep) {
		this.sleep = sleep;
		return (T)this;
	}

}
