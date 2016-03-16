package net.etalia.crepuscolo.check;

import java.lang.reflect.Method;
import java.util.Arrays;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.utils.SimpleCache;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public aspect CachingAspect {

	private SimpleCache<SimpleCache<Object>> caches = new SimpleCache<>(Integer.MAX_VALUE, Long.MAX_VALUE/2);
	
	private StringBuilder keyBase(JoinPoint tjp, Method getter) {
		StringBuilder key = new StringBuilder();
		key.append(tjp.getSignature().getDeclaringTypeName());
		Object instance = tjp.getThis();
		if (instance != null) {
			if (instance instanceof BaseEntity) {
				key.append('-');
				key.append(((BaseEntity) instance).getId());
			} else {
				throw new IllegalStateException("Cannot cache instance method " + tjp + " because class is not a Persistent class");
			}
		} else {
			key.append("-static");
		}
		key.append('-');
		key.append(getter.getName());
		return key;
	}
	
	private void keyArguments(JoinPoint tjp, StringBuilder key, boolean full) {
		Object[] args = tjp.getArgs();
		if (args.length == 0) return;
		int end = args.length;
		if (!full) end--;
		for (int i = 0; i < end; i++) {
			key.append('-');
			Object obj = args[i];
			if (obj == null) {
				key.append("null");
			} else {
				key.append(obj.toString());
			}
		}
	}
	
	private SimpleCache<Object> getCache(JoinPoint tjp, StringBuilder key, Method getter) {
		String ks = key.toString();
		SimpleCache<Object> got = caches.getOrLock(ks);
		if (got == null) {
			try {
				Cache cacheAnn = getter.getAnnotation(Cache.class);
				if (cacheAnn == null) throw new IllegalStateException("Cannot find annotation @Cache on " + getter);
				got = new SimpleCache<Object>(cacheAnn.max(),parseExpires(cacheAnn.expires()));
				caches.putAndUnlock(ks, got);
			} catch (Throwable t) {
				caches.evictAndUnlock(ks);
				throw new IllegalStateException("Error creating cache for " + tjp,t);
			}
		}
		return got;
	}
	
	public static long parseExpires(String exp) {
		String[] split = exp.split(" ");
		
		long ret = 0;
		long lastVal = -1;
		for (int i = 0; i < split.length; i++) {
			if (lastVal == -1) {
				try {
					lastVal = Long.parseLong(split[i]);
				} catch (NumberFormatException e) {
					throw new IllegalStateException("Invalid number " + split[i]);
				}
			} else {
				String acm = split[i].toLowerCase();
				if (acm.startsWith("s")) {
					lastVal *= 1000;
				} else if (acm.startsWith("mo")) {
					lastVal *= 30 * 24 * 60 * 60_000l;
				} else if (acm.startsWith("m")) {
					lastVal *= 60_000l;
				} else if (acm.startsWith("h")) {
					lastVal *= 60 * 60_000l;
				} else if (acm.startsWith("d")) {
					lastVal *= 24 * 60 * 60_000l;
				} else if (acm.startsWith("w")) {
					lastVal *= 7 * 24 * 60 * 60_000l;
				} else if (acm.startsWith("y")) {
					lastVal *= 365 * 24 * 60 * 60_000l;
				} else {
					throw new IllegalStateException("Invalid time component " + split[i]);
				}
				ret += lastVal;
				lastVal = -1;
			}
		}
		if (lastVal != -1) ret += lastVal;
		
		return ret;
	}
	
	Object around() :
		execution(@Cache * *.get*(..))
	{
		Method getter = ((MethodSignature)thisJoinPointStaticPart.getSignature()).getMethod();
		StringBuilder key = keyBase(thisJoinPoint, getter);
		SimpleCache<Object> cache = getCache(thisJoinPoint, key, getter);
		keyArguments(thisJoinPoint, key, true);
		String ks = key.toString(); 
		Object ret = cache.getOrLock(ks);
		if (ret == null) {
			boolean expunge = true;
			try {
				ret = proceed();
				cache.putAndUnlock(ks, ret);
				expunge = false;
			} finally {
				if (expunge) cache.evictAndUnlock(ks);
			}
		}
		return ret;
	}

	void around() :
		execution(@Cache void *.set*(..))
	{
		Method setter = ((MethodSignature)thisJoinPointStaticPart.getSignature()).getMethod();
		Class<?>[] stypes = setter.getParameterTypes();
		Class<?>[] gtypes = new Class<?>[stypes.length - 1];
		System.arraycopy(stypes, 0, gtypes, 0, gtypes.length);
		Method getter;
		try {
			getter = thisJoinPointStaticPart.getSignature().getDeclaringType().getDeclaredMethod("g" + setter.getName().substring(1), gtypes);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot find cached getter method " + thisJoinPointStaticPart.getClass().getName() + ".g" + setter.getName().substring(1) + "(" + Arrays.toString(gtypes) + ")", e);
		}
		StringBuilder key = keyBase(thisJoinPoint, getter);
		SimpleCache<Object> cache = getCache(thisJoinPoint, key, getter);
		keyArguments(thisJoinPoint, key, false);
		String ks = key.toString();
		Object[] args = thisJoinPoint.getArgs();
		Object val = args[args.length - 1];
		cache.putAndUnlock(ks, val);
	}
	
}
