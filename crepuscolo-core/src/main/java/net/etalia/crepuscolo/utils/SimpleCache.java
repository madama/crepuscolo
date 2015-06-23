package net.etalia.crepuscolo.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple implementation of a cache with concurrent control.
 * 
 * <p>
 * The method {@link #getOrLock(String)} retrieves the value in the cache, but if no value
 * is found it return null and locks subseguent calls for the same key, thus giving the calling thread
 * time to initialize the value.
 * </p><p>
 * When the thread has initialized the value, calling {@link #putAndUnlock(String, Object)} both sets the value and
 * unlocks the other threads waiting on {@link #getOrLock(String)}.
 * </p><p>
 * If the thread initializing the value hits an exception during value initialization,
 * it has to call {@link #evictAndUnlock(String)}, if there are thread waiting on {@link #getOrLock(String)}
 * one of them will be returned null and given time to initialize the value.
 *  
 * @author Simone Gianni <simoneg@apache.org>
 *
 * @param <T>
 */
public class SimpleCache<T> {

	private static String LOCKED = "__LOCKED__";

	private static class CachedElement {
		long ts = System.currentTimeMillis();
		Object obj = LOCKED;
		
		public boolean expired(long expire) {
			return this.ts + expire < System.currentTimeMillis();
		}
	}

	private ConcurrentMap<String,CachedElement> cache = new ConcurrentHashMap<>();

	private int max;
	private long expire;

	public SimpleCache(int max, long expire) {
		this.max = max;
		if (this.max == -1) this.max = Integer.MAX_VALUE;
		this.expire = expire;
		if (this.expire == 0) this.expire = Long.MAX_VALUE / 2;
	}

	@SuppressWarnings("unchecked")
	public T getOrLock(String key) {
		CachedElement ncached = new CachedElement();
		CachedElement cached = cache.putIfAbsent(key, ncached);
		if (cached == null) 
			return null;
		boolean expired = false;
		boolean locked = false;
		synchronized (cached) {
			expired = cached.expired(expire);
			if (expired) {
				cache.remove(key);
				cached.notifyAll();
			}
			locked = cached.obj == LOCKED;
		}
		if (expired) {
			return getOrLock(key);
		}
		if (locked) {
			synchronized (cached) {
				try {
					cached.wait(10000);
				} catch (InterruptedException e) {
				}
				if (cached.obj != LOCKED && cached.obj != null) return (T)cached.obj;
			}
			return getOrLock(key);
		}
		return (T)cached.obj;
	}

	public void putAndUnlock(String key, T val) {
		CachedElement ncached = new CachedElement();
		ncached.obj = val;
		CachedElement cached = cache.putIfAbsent(key, ncached);
		//int acsize = 0;
		if (cached != null) {
			synchronized (cached) {
				cached.ts = System.currentTimeMillis();
				cached.obj = val;
				cached.notifyAll();
			}
		}
		if (cache.size() > this.max * 1.5) {
			//acsize = added.getAndSet(0);
			// First attempt, remove expireds
			this.clean(false);
			if (cache.size() > this.max * 1.5) {
				// Second attempt, remove randomly
				this.clean(true);
			}
			//added.addAndGet(acsize);
		}
	}

	protected void clean(boolean force) {
		Iterator<Entry<String, CachedElement>> iter = cache.entrySet().iterator();
		while (iter.hasNext()) {
			if (cache.size() <= this.max) break;
			Map.Entry<String, CachedElement> entry = iter.next();
			CachedElement ce = entry.getValue();
			if (!force && !ce.expired(expire)) continue;
			if (ce.obj == LOCKED) continue;
			synchronized (ce) {
				ce.notifyAll();
			}
			iter.remove();
		}
	}

	public void evictAndUnlock(String key) {
		CachedElement cached = cache.remove(key);
		if (cached != null) {
			synchronized (cached) {
				cached.ts = 0;
				cached.obj = null;
				cached.notifyAll();
			}
		}
	}

	public int size() {
		return this.cache.size();
	}

}
