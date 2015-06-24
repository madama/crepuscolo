package net.etalia.crepuscolo.test;

import java.lang.reflect.Method;

public class EmbedWebIPCCore {

	private static Class<?> server;
	private static Class<?> client;
	
	private static Method serverMethod;
	private static Method clientMethod;
	
	public static void registerServer(Class<?> clazz) {
		server = clazz;
		serverMethod = findMethod(server);
	}
	
	public static void registerClient(Class<?> clazz) {
		client = clazz;
		clientMethod = findMethod(client);
	}
	
	private static Method findMethod(Class<?> clazz) {
		try {
			return clazz.getMethod("received", String.class, String.class, byte[].class);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot find method received on local EmbedWebIPC", e);
		}
	}
	
	public static Object broadcast(boolean toClient, String clazz, String message, byte[] payload) throws Exception {
		if (clientMethod == null || serverMethod == null) return null;
		Method other = toClient ? clientMethod : serverMethod;
		return other.invoke(null, clazz, message, payload);
	}

	public static void removeServer() {
		server = null;
		serverMethod = null;
	}
	
}
