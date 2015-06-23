package net.etalia.crepuscolo.domain;

public interface Stored {

	public long getCreationDate();

	public void setCreationDate(long timestamp);

	public long getLastModified();

	public void setLastModified(long timestamp);

}
