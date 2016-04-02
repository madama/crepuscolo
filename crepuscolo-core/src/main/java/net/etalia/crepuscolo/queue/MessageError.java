package net.etalia.crepuscolo.queue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class MessageError {

	private String payload;
	private String stacktrace;
	private String queue;
	private Date lastError;
	private String threadName;

	public MessageError(String queue) {
		this.queue = queue;
		this.lastError = new Date();
		this.threadName = Thread.currentThread().getName();
	}
	
	public MessageError() {}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getStacktrace() {
		return stacktrace;
	}

	public void setStacktrace(String stacktrace) {
		this.stacktrace = stacktrace;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public Date getLastError() {
		return lastError;
	}

	public void setLastError(Date lastError) {
		this.lastError = lastError;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThrowable(Throwable t) {
		if (t == null) {
			this.stacktrace = null;
			return;
		}
		try (
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			) {
			t.printStackTrace(pw);
			this.stacktrace = sw.toString();
		} catch (Throwable in) {
			
		}
	}

	public void setThread(Thread t) {
		if (t == null) {
			this.threadName = null;
			return;
		}
		this.threadName = t.getName();
	}

}
