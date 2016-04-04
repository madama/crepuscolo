package net.etalia.crepuscolo.hibernate;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.type.Type;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.etalia.crepuscolo.check.CheckNoAuth;
import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.ChangeNotification;
import net.etalia.crepuscolo.queue.SendQueue;
import net.etalia.crepuscolo.queue.SendQueue.SendBatch;

public class HibernateQueueInterceptor extends EmptyInterceptor implements ApplicationContextAware {

	private Log log = LogFactory.getLog(HibernateQueueInterceptor.class);

	private SendQueue<ChangeNotification<? extends BaseEntity>> queue = null;

	private SessionFactory sessionFactory = null;
	private ApplicationContext ctx = null;

	private WeakHashMap<Transaction, List<ChangeNotification<? extends BaseEntity>>> notifications = new WeakHashMap<>();
	private WeakHashMap<Transaction, SendBatch<ChangeNotification<? extends BaseEntity>>> batches = new WeakHashMap<>();
	private WeakHashMap<Transaction, Boolean> nosync = new WeakHashMap<>();

	public void nosync() {
		SessionFactory sf = getSessionFactory();
		if (sf == null) return;
		Transaction tx = sf.getCurrentSession().getTransaction();
		if (tx == null) throw new IllegalStateException("No transaction is active");
		nosync.put(tx, Boolean.TRUE);
		notifications.remove(tx);
		batches.remove(tx);
	}

	public void setQueue(SendQueue<ChangeNotification<? extends BaseEntity>> queue) {
		this.queue = queue;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		if (log.isDebugEnabled()) {
			log.debug("Flush dirty " + id + " : " + entity);
		}
		if (entity instanceof BaseEntity) {
			notify((BaseEntity) entity, ChangeNotification.Type.UPDATED);
		}
		return false;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (log.isDebugEnabled()) {
			log.debug("Save " + id + " : " + entity);
		}
		if (entity instanceof BaseEntity) {
			notify((BaseEntity) entity, ChangeNotification.Type.ADDED);
		}
		return false;
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (log.isDebugEnabled()) {
			log.debug("Delete " + id + " : " + entity);
		}
		if (entity instanceof BaseEntity) {
			notify((BaseEntity) entity, ChangeNotification.Type.DELETED);
		}
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("Collection update " + key + " : " + collection);
		}
		if (collection instanceof AbstractPersistentCollection) {
			Object entity = ((AbstractPersistentCollection) collection).getOwner();
			if (entity instanceof BaseEntity) {
				notify((BaseEntity) entity, ChangeNotification.Type.UPDATED);
			}
		}
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		if (tx.wasRolledBack()) return;
		
		List<ChangeNotification<? extends BaseEntity>> list = notifications.get(tx);
		if (list == null) return;
		
		SendBatch<ChangeNotification<? extends BaseEntity>> batch = getBatch();
		if (batch != null) {
			for (ChangeNotification<? extends BaseEntity> not : list) {
				batch.put(not);
			}
			batch.prepare();
		}
	}

	@Override
	@CheckNoAuth
	public void afterTransactionCompletion(Transaction tx) {
		SendBatch<ChangeNotification<? extends BaseEntity>> batch = null;
		synchronized (batches) {
			batch = batches.remove(tx);
		}
		if (batch == null) return;
		if (tx.wasRolledBack()) return;

		batch.send();
	}
	
	private SessionFactory getSessionFactory() {
		if (this.ctx == null) return null;
		if (this.sessionFactory != null) return this.sessionFactory;
		this.sessionFactory = this.ctx.getBean(SessionFactory.class);
		return this.sessionFactory;
	}

	@CheckNoAuth
	public void notify(BaseEntity entity, ChangeNotification.Type type) {
		if (queue == null) return;

		Class<? extends BaseEntity> clazz = computeSendClass(entity);
		if (clazz == null) return;

		ChangeNotification<BaseEntity> req = new ChangeNotification<BaseEntity>(type, clazz, entity);
		processNotification(req);
		if (getSessionFactory() == null) {
			SendBatch<ChangeNotification<? extends BaseEntity>> batch = queue.startBatch();
			batch.put(req);
			batch.send();
		} else {
			List<ChangeNotification<? extends BaseEntity>> list = getNotificationList();
			if (list != null) {
				if (type == ChangeNotification.Type.DELETED) {
					for (Iterator<ChangeNotification<? extends BaseEntity>> iterator = list.iterator(); iterator.hasNext();) {
						ChangeNotification<? extends BaseEntity> not = iterator.next();
						if (not.getInstance().getId().equals(entity.getId())) {
							iterator.remove();
						}
					}
				}
				list.add(req);
			}
		}
	}

	private SendBatch<ChangeNotification<? extends BaseEntity>> getBatch() {
		Transaction tx = sessionFactory.getCurrentSession().getTransaction();
		Boolean nos = nosync.get(tx);
		if (nos != null && nos) return null;
		SendBatch<ChangeNotification<? extends BaseEntity>> batch = null;
		synchronized (batches) {
			batch = batches.get(tx);
			if (batch == null) {
				batch = queue.startBatch();
				batches.put(tx, batch);
			}
		}
		return batch;
	}

	private List<ChangeNotification<? extends BaseEntity>> getNotificationList() {
		Transaction tx = sessionFactory.getCurrentSession().getTransaction();
		Boolean nos = nosync.get(tx);
		if (nos != null && nos) {
			log.info("Not syncing cause nosync is active");
			return null;
		}
		List<ChangeNotification<? extends BaseEntity>> ret = null;
		synchronized (notifications) {
			ret = notifications.get(tx);
			if (ret == null) {
				ret = new LinkedList<>();
				notifications.put(tx, ret);
			}
		}
		return ret;
	}

	@CheckNoAuth
	public void directSend(BaseEntity entity, ChangeNotification.Type type) {
		Class<? extends BaseEntity> clazz = computeSendClass(entity);
		if (clazz == null) return;
		ChangeNotification<BaseEntity> req = new ChangeNotification<BaseEntity>(type, clazz, entity);
		SendBatch<ChangeNotification<? extends BaseEntity>> quick = queue.startBatch();
		quick.put(req);
		quick.send();
	}

	protected Class<? extends BaseEntity> computeSendClass(BaseEntity entity) {
		return entity.getClass();
	}

	protected void processNotification(ChangeNotification<BaseEntity> notification) {
	}

}
