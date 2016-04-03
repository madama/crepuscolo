package net.etalia.crepuscolo.queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ReadQueuePoller<T> extends Thread {

	protected Log log = LogFactory.getLog(ReadQueuePoller.class);

	protected ReadQueue<T> queue = null;
	private int wait = 0;

	private volatile boolean mustStop = false;

	public ReadQueuePoller(ReadQueue<T> queue) {
		this(queue, 0);
	}
	public ReadQueuePoller(ReadQueue<T> queue, int wait) {
		this.queue = queue;
		this.wait = wait;
	}

	@Override
	public void run() {
		while (!isInterrupted() && !mustStop) {
			try {
				if (wait > 0) {
					sleep(wait);
				}
				MessageIterator<T> iterator = queue.iterator();
				if (iterator.hasNext()) {
					try {
						parse(iterator);
					} catch (Throwable e) {
						if (e instanceof InterruptedException) return;
						log.warn("Error while parsing queue messages", e);
					}
				}
			} catch (InterruptedException e) {
				interrupt();
				return;
			} catch (Throwable t) {
				log.warn("Error while fecthing SQS messages, will retry soon", t);
				try {
					Thread.sleep(wait * 10);
				} catch (InterruptedException e) {
					interrupt();
					return;
				}
			}
		}
	}

	protected abstract void parse(MessageIterator<T> iterator);

	public void kill() {
		this.interrupt();
		this.mustStop  = true;
	}

}
