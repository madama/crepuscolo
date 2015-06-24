package net.etalia.crepuscolo.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class EmbedWebIPC {
	
	private static boolean server = false;;
	private static boolean oncore = false;
	
	public static void initServer() {
		server = true;
		oncore = true;
		EmbedWebIPCCore.registerServer(EmbedWebIPC.class);
	}
	
	private static Map<String, EmbedWebIPC> registered = new HashMap<String, EmbedWebIPC>();
	
	public static void register(Class<?> clazz, EmbedWebIPC ipc) {
		if (!oncore) {
			EmbedWebIPCCore.registerClient(EmbedWebIPC.class);
			oncore = true;
		}
		ipc.myclazz = clazz.getName();
		registered.put(ipc.myclazz, ipc);
	}
	
	public static Object received(String clazz, String message, byte[] payload) throws Exception {
		EmbedWebIPC ipc = registered.get(clazz);
		if (ipc == null) throw new IllegalStateException("Cannot find a local IPC for " + clazz);
		ipc.receiving = message;
		try {
			return ipc.received(message, payload);
		} finally {
			ipc.receiving = null;
		}
	}
	
	
	private String myclazz;
	private String receiving = null;
	
	public abstract Object received(String message, byte[] payload) throws Exception;

	public Object send(String message) {
		return send(message, null);
	}
	
	public Object send(String message, byte[] payload) {
		if (receiving != null && receiving.equals(message)) return null;
		try {
			return EmbedWebIPCCore.broadcast(server, myclazz, message, payload);
		} catch (Exception e) {
			throw new IllegalStateException("Error sending message '" + message + "' to " + myclazz, e);
		}
	}
	
	public <T extends Serializable> Object sendSerialize(String message, T payload) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream daos = new ObjectOutputStream(bos);
			daos.writeObject(payload);
			daos.close();
			bos.close();
			return send(message, bos.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Error sending message '" + message + "' to " + myclazz, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T deserialize(byte[] payload) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(payload);
			ObjectInputStream dais = new ObjectInputStream(bis);
			Object ret = dais.readObject();
	        dais.close();
			return (T)ret;
		} catch (Exception e) {
			throw new IllegalStateException("Error deserializing payload for " + myclazz, e);
		}
	}
}
