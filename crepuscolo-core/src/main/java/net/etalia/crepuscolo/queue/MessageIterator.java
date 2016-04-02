package net.etalia.crepuscolo.queue;

import java.util.Iterator;

public interface MessageIterator<T> extends Iterator<T> {

	public void reject();

	public void reject(Throwable exception);

	public ReadReceipt skip();

	public String getMessageId();

}