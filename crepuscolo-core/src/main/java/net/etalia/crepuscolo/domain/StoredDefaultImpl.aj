package net.etalia.crepuscolo.domain;

import javax.persistence.Column;

public aspect StoredDefaultImpl {

	private long Stored.lastModified = System.currentTimeMillis();

	private long Stored.creationDate = System.currentTimeMillis();

	@Column(nullable=false)
	public long Stored.getLastModified() {
		return this.lastModified;
	}

	public void Stored.setLastModified(long timestamp) {
		this.lastModified = timestamp;
	}

	@Column(nullable=false)
	public long Stored.getCreationDate() {
		return this.creationDate;
	}

	public void Stored.setCreationDate(long timestamp) {
		this.creationDate = timestamp;
	}

}
