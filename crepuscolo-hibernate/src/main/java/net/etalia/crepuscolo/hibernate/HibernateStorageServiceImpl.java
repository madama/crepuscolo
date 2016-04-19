package net.etalia.crepuscolo.hibernate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.services.StorageService;
import net.etalia.crepuscolo.services.ValidationFailedException;
import net.etalia.crepuscolo.services.ValidationService;
import net.etalia.crepuscolo.utils.ChainMap;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class HibernateStorageServiceImpl implements StorageService {

	private SessionFactory sessionFactory;
	private ValidationService validation;

	@Transactional
	@Override
	public <T extends BaseEntity> T load(Class<T> clz, String id) {
		return (T) getCurrentSession().get(clz, id);
	}

	@Transactional
	@Override
	public <T extends BaseEntity> T load(Class<T> clz, Map<String, ?> andCriteria) {
		Criteria criteria = getCurrentSession().createCriteria(clz);
		for (String k : andCriteria.keySet()) {
			criteria.add(Restrictions.and(Restrictions.eq(k, andCriteria.get(k))));
		}
		return (T) criteria.uniqueResult();
	}


	@Transactional
	@Override
	public <T extends BaseEntity> T load(String query, Map<String, ?> criteria) {
		Query q = getCurrentSession().createQuery(query);
		setParameters(criteria, q);
		return (T) q.uniqueResult();
	}

	@Transactional(readOnly=true,propagation=Propagation.REQUIRES_NEW)
	@Override
	public <T extends BaseEntity> T loadStored(Class<T> clz, String id) {
		return (T) getCurrentSession().get(clz, id);
	}

	private void setParameters(Map<String, ?> criteria, Query q) {
		if (criteria == null) return;
		for (String k : criteria.keySet()) {
			Object v = criteria.get(k);
			if (v instanceof String) {
				q.setString(k, (String) v);
			} else {
				try {
					q.setParameter(k, v);
				} catch (Throwable t) {
					q.setString(k, v.toString());
				}
			}
		}
	}

	@Transactional(noRollbackFor={ValidationFailedException.class})
	@Override
	public <T extends BaseEntity> T save(T obj) {
		return save(obj, false);
	}

	@Transactional(noRollbackFor={ValidationFailedException.class})
	@Override
	public <T extends BaseEntity> T save(T obj, boolean andFlush) {
		Set<ConstraintViolation<BaseEntity>> violations = validation.validate(obj);
		if (!violations.isEmpty()) {
			throw new ValidationFailedException(violations);
		}
		Session session = getCurrentSession();
		session.saveOrUpdate(obj);
		if (andFlush) session.flush();
		return obj;
	}

	@Override
	public <T extends BaseEntity> List<T> list(String query,Map<String,?> criteria) {
		return list(query, criteria, null, null);
	}

	@Override
	public <T extends BaseEntity> List<T> list(String query) {
		return list(query, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation=Propagation.SUPPORTS)
	@Override
	public <T extends BaseEntity> List<T> list(String query, Map<String, ?> criteria, Integer offset, Integer length) {
		Query q = getCurrentSession().createQuery(query);
		if (criteria != null)
			setParameters(criteria, q);
		if (offset != null)
			q.setFirstResult(offset);
		if (length != null)
			q.setMaxResults(length);
		return q.list();
	}

	@Override
	public void delete(BaseEntity obj) {
		getCurrentSession().delete(obj);
	}

	@Override
	public <T extends BaseEntity> boolean delete(Class<T> clazz, String id) {
		Query q = getCurrentSession().createQuery("DELETE " + clazz.getName() + " WHERE id=:id");
		setParameters(new ChainMap<String>("id", id), q);
		return q.executeUpdate() != 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T rawQuery(String query, Map<String, ?> criteria) {
		Query q = getCurrentSession().createQuery(query);
		setParameters(criteria, q);
		return (T) q.uniqueResult();
	}

	@Override
	public List<?> rawQueryList(String query, Map<String, ?> criteria) {
		return rawQueryList(query, criteria, null, null);
	}

	@Override
	public List<? extends Object> rawQueryList(String query, Map<String, ?> criteria, Integer offset, Integer length) {
		Query q = getCurrentSession().createQuery(query);
		setParameters(criteria, q);
		if (offset != null) {
			q.setFirstResult(offset);
		}
		if (length != null)
			q.setMaxResults(length);
		return q.list();
	}

	@Override
	public int rawUpdate(String query, Map<String, ?> criteria) {
		Query q = getCurrentSession().createQuery(query);
		setParameters(criteria, q);
		return q.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T nativeQuery(String query, Map<String, ?> criteria) {
		Query q = getCurrentSession().createSQLQuery(query);
		setParameters(criteria, q);
		return (T) q.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<? extends Object> nativeQueryList(String query, Map<String, ?> criteria) {
		Query q = getCurrentSession().createSQLQuery(query);
		setParameters(criteria, q);
		return q.list();
	}

	public Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	//DI
	public void setSessionFactory(SessionFactory sf) {
		this.sessionFactory = sf;
	}
	public void setValidationService(ValidationService vs) {
		this.validation = vs;
	}

	@Override
	public boolean isPersisted(BaseEntity object) {
		EntityEntry entry = ((SessionImplementor)getCurrentSession()).getPersistenceContext().getEntry(object);
		if (entry == null) return false;
		return entry.isExistsInDatabase();
	}

}
