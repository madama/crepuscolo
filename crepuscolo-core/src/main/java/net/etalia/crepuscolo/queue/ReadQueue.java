package net.etalia.crepuscolo.queue;


public interface ReadQueue<T> extends Iterable<T> {

	public MessageIterator<T> iterator();

	public void reject(ReadReceipt receipt, Throwable exception);

	public void delete(ReadReceipt receipt);

}
