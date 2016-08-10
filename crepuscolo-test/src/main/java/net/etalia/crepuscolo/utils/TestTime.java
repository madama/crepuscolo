package net.etalia.crepuscolo.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.etalia.crepuscolo.utils.Time;

public class TestTime extends Time implements Closeable {

	public void install() {
		Time.defaultInstance = this;
	}
	
	public static void uninstall() {
		Time.defaultInstance = new Time();
	}
	
	@Override
	public void close() {
		uninstall();
	}
	
	private boolean running = true;
	private long modifTime;
	
	public void advance(long howmuch) {
		this.advance(howmuch, TimeUnit.MILLISECONDS);
	}
	
	public void advance(long howmuch, TimeUnit unit) {
		if (this.running) {
			this.modifTime = this.currentTimeMillis();
			this.running = false;
		}
		this.modifTime += unit.toMillis(howmuch);
	}
	
	public void set(long when) {
		this.running = false;
		this.modifTime = when;
	}
	
	public long currentTimeMillis() {
		return this.running ? System.currentTimeMillis() : this.modifTime;
	}
	
	@Override
	public void sleep(long time) {
		if (this.running) {
			super.sleep(time);
		} else {
			this.advance(time);
		}
	}



	
	
}
