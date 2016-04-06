package net.etalia.crepuscolo.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class ChainedInterceptor implements Interceptor {

	private List<Interceptor> chain = new ArrayList<Interceptor>();

	public void setChain(List<Interceptor> chain) {
		this.chain = chain;
	}

	public void addToChain(Interceptor interceptor) {
		if (interceptor != null) {
			this.chain.add(interceptor);
		}
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		for (Interceptor interceptor : chain) {
			ret |= interceptor.onLoad(entity, id, state, propertyNames, types);
		}
		return ret;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		for (Interceptor interceptor : chain) {
			ret |= interceptor.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
		}
		return ret;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		for (Interceptor interceptor : chain) {
			ret |= interceptor.onSave(entity, id, state, propertyNames, types);
		}
		return ret;
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		for (Interceptor interceptor : chain) {
			interceptor.onDelete(entity, id, state, propertyNames, types);
		}
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
		for (Interceptor interceptor : chain) {
			interceptor.onCollectionRecreate(collection, key);
		}
	}

	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
		for (Interceptor interceptor : chain) {
			interceptor.onCollectionRemove(collection, key);
		}
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		for (Interceptor interceptor : chain) {
			interceptor.onCollectionUpdate(collection, key);
		}
	}

	@Override
	public void preFlush(Iterator entities) throws CallbackException {
		for (Interceptor interceptor : chain) {
			interceptor.preFlush(entities);
		}
	}

	@Override
	public void postFlush(Iterator entities) throws CallbackException {
		for (Interceptor interceptor : chain) {
			interceptor.postFlush(entities);
		}
	}

	@Override
	public Boolean isTransient(Object entity) {
		for (Interceptor interceptor : chain) {
			Boolean ret = interceptor.isTransient(entity);
			if (ret != null) return ret;
		}
		return null;
	}

	@Override
	public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		for (Interceptor interceptor : chain) {
			int[] ret = interceptor.findDirty(entity, id, currentState, previousState, propertyNames, types);
			if (ret != null) return ret;
		}
		return null;
	}

	@Override
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
		for (Interceptor interceptor : chain) {
			Object ret = interceptor.instantiate(entityName, entityMode, id);
			if (ret != null) return ret;
		}
		return null;
	}

	@Override
	public String getEntityName(Object object) throws CallbackException {
		for (Interceptor interceptor : chain) {
			String ret = interceptor.getEntityName(object);
			if (ret != null) return ret;
		}
		return null;
	}

	@Override
	public Object getEntity(String entityName, Serializable id) throws CallbackException {
		for (Interceptor interceptor : chain) {
			Object ret = interceptor.getEntity(entityName, id);
			if (ret != null) return ret;
		}
		return null;
	}

	@Override
	public void afterTransactionBegin(Transaction tx) {
		for (Interceptor interceptor : chain) {
			interceptor.afterTransactionBegin(tx);
		}
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		for (Interceptor interceptor : chain) {
			interceptor.beforeTransactionCompletion(tx);
		}
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		for (Interceptor interceptor : chain) {
			interceptor.afterTransactionCompletion(tx);
		}
	}

	@Override
	public String onPrepareStatement(String sql) {
		for (Interceptor interceptor : chain) {
			String ret = interceptor.onPrepareStatement(sql);
			if (ret != null) return ret;
		}
		return null;
	}

}
