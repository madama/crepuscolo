package net.etalia.crepuscolo.utils;

import java.lang.reflect.Method;

public class HibernateUnwrapper {

	private static Class<?> hproxyClazz = null;
	private static Method unwrapGetInitializer = null;
	private static Method unwrapGetInstance = null;

	static {
		try {
			hproxyClazz = Class.forName("org.hibernate.proxy.HibernateProxy");
			unwrapGetInitializer = hproxyClazz.getDeclaredMethod("getHibernateLazyInitializer");
			Class<?> lazyInitializer = Class.forName("org.hibernate.proxy.LazyInitializer");
			unwrapGetInstance = lazyInitializer.getDeclaredMethod("getImplementation");
		} catch (Exception e) {
			hproxyClazz = null;
			unwrapGetInitializer = null;
			unwrapGetInstance = null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T unwrap(T obj) {
		if (obj == null) return null;
		if (hproxyClazz == null) return obj;
		if (hproxyClazz.isInstance(obj)) {
			try {
				Object initializer = unwrapGetInitializer.invoke(obj);
				return (T)unwrapGetInstance.invoke(initializer);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return obj;
	}

}
