package net.etalia.crepuscolo.json;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.etalia.crepuscolo.check.Checker;
import net.etalia.crepuscolo.check.CheckerFactory;
import net.etalia.crepuscolo.check.GetterCheckPoint;
import net.etalia.crepuscolo.check.Transformer;
import net.etalia.crepuscolo.services.AuthService;
import net.etalia.crepuscolo.services.ServiceHack;
import net.etalia.jalia.JsonClassData;
import net.etalia.jalia.annotations.JsonGetter;

/**
 * Extends the {@link JsonClassData} to implement :
 * - Checkers on getters
 * - Cleaning of defaults for client sending
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
public class CheckersJsonClassData extends JsonClassData {

	private CheckerFactory checkerFactory;

	protected CheckersJsonClassData(JsonClassData other, CheckerFactory checkerFactory, boolean client) {
		super(other);
		this.checkerFactory = checkerFactory;
		if (client) {
			// Remove getters that doesn't have setters or explicit annotation and set them as on demand only
			for (Iterator<Entry<String, Method>> iter = this.getters.entrySet().iterator(); iter.hasNext();) {
				Entry<String, Method> entry = iter.next();
				//if (!entry.getValue().isAnnotationPresent(Transient.class)) {
					if (this.setters.containsKey(entry.getKey())) continue;
					if (entry.getValue().isAnnotationPresent(JsonGetter.class)) continue;
					Class<?> returnType = entry.getValue().getReturnType();
					if (Collection.class.isAssignableFrom(returnType)) continue;
					if (Map.class.isAssignableFrom(returnType)) continue;
				//}
				this.ondemand.put(entry.getKey(), entry.getValue());
				iter.remove();
			}
		}
	}
	
	@Override
	public Object getValue(String name, Object obj) {
		
		Checker checker = null;
		GetterCheckPoint cp = null;
		Object ret = super.getValue(name, obj);
		if (checkerFactory != null) {
			Method getter = super.getters.get(name);
			if (getter == null) getter = super.ondemand.get(name);
			if (getter == null) return null;
			checker  = checkerFactory.getFor(getter);
			cp = new GetterCheckPoint(obj, getter);
			cp.setAuthService(ServiceHack.getInstance().getBean(AuthService.class)); //TODO: try to avoid this...
			if (checker.check(cp) != 0) {
				//log.trace("... unauthorized: " + propertyName);
				return null;
			}
			if (checker != null && checker instanceof Transformer) {
				return ((Transformer) checker).transform(cp, ret);
			}		
		}
		return ret;
	}
	
	@Override
	public boolean setValue(String name, Object nval, Object tgt, boolean force) {
		Checker checker = null;
		GetterCheckPoint cp = null;
		if (checkerFactory != null) {
			Method setter = super.setters.get(name);
			if (setter == null) return false;
			checker  = checkerFactory.getFor(setter);
			cp = new GetterCheckPoint(tgt, setter);
			cp.setAuthService(ServiceHack.getInstance().getBean(AuthService.class)); //TODO: try to avoid this...
			if (checker.check(cp) != 0) {
				//log.trace("... unauthorized: " + propertyName);
				return false;
			}
			if (checker != null && checker instanceof Transformer) {
				nval = ((Transformer) checker).transform(cp, nval);
			}		
		}
		return super.setValue(name, nval, tgt, force);		
	}

	public void clearDefaults() {
		this.defaults.clear();
	}

}
