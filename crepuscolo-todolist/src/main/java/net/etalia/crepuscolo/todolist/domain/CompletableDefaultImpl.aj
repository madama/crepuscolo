package net.etalia.crepuscolo.todolist.domain;

public aspect CompletableDefaultImpl {

	private Boolean Completable.complete;

	public Boolean Completable.getComplete() {
		return this.complete;
	}

	public void Completable.setComplete(Boolean complete) {
		this.complete = complete;
	}

}