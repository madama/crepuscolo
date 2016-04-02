package net.etalia.crepuscolo.queue;

public interface SendQueue<T> {

	public interface SendBatch<T> {
		public void put(T object);
		public void prepare();
		public void send();
	}

	public SendBatch<T> startBatch();

}
