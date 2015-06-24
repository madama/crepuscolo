package net.etalia.crepuscolo.utils;

import java.util.Date;

public class Time {

	static Time defaultInstance = new Time();

	public static Time getDefaultInstance() {
		return defaultInstance;
	}

	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public boolean isAfter(long start, long millisTimeout) {
		return currentTimeMillis() - start > millisTimeout;
	}

	public Date currentDate() {
		return new Date(currentTimeMillis());
	}

}
