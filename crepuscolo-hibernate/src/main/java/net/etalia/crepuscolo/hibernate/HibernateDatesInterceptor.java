package net.etalia.crepuscolo.hibernate;

import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

import net.etalia.crepuscolo.domain.Stored;
import net.etalia.crepuscolo.utils.Time;

public class HibernateDatesInterceptor extends EmptyInterceptor {

	private Time time = Time.getDefaultInstance();

	public void setTime(Time time) {
		this.time = time;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		boolean ret = false;
		if (entity instanceof Stored) {
			int lastModifiedIndex = ArrayUtils.indexOf(propertyNames, "lastModified");
			int creationDateIndex = ArrayUtils.indexOf(propertyNames, "creationDate");
			if (previousState != null)
				currentState[creationDateIndex] = previousState[creationDateIndex];
			currentState[lastModifiedIndex] = time.currentTimeMillis();
			ret = true;
		}
		return ret;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		boolean ret = false;
		if (entity instanceof Stored) {
			int lastModifiedIndex = ArrayUtils.indexOf(propertyNames, "lastModified");
			int creationDateIndex = ArrayUtils.indexOf(propertyNames, "creationDate");
			state[creationDateIndex] = time.currentTimeMillis();
			state[lastModifiedIndex] = state[creationDateIndex];
			ret = true;
		}
		return ret;
	}

}
