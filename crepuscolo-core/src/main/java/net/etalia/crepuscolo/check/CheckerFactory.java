package net.etalia.crepuscolo.check;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CheckerFactory {

	protected final static Log log = LogFactory.getLog(CheckerFactory.class);
	
	private Map<Method, Checker> cache = new HashMap<Method, Checker>();
	
	public Checker getFor(Method m) {
		if (CheckAspect.aspectOf().isSkipping()) return new ChAndChecker();
		Checker ret = cache.get(m);
		if (ret != null) return ret;
		
		log.trace("Building checker for method " + m);
		
		ret = new ChAndChecker();
		
		{
			Annotation[] anns = m.getAnnotations();
			for (Annotation annotation : anns) {
				Checker ch = buildChecker(annotation);
				if (ch == null) continue;
				// Add the checker to returned AndChecker
				((ChAndChecker)ret).addChecker(ch);
			}
		}
		
		{
			Annotation[][] paramAnn = m.getParameterAnnotations();
			int i = 0;
			for (Annotation[] anns : paramAnn) {
				for (Annotation annotation : anns) {
					Checker ch = buildChecker(annotation);
					if (ch == null) continue;
					// Wrap it in a parameter->instance checker
					ch = new ParameterWrappingChecker(ch,i);
					// Add the checker to returned AndChecker
					((ChAndChecker)ret).addChecker(ch);
				}
				i++;
			}
		}
		
		cache.put(m, ret);
		return ret;
	}
	
	public static Checker buildChecker(Annotation annotation) {
		CheckerAnnotation chann = null;
		Class<? extends Annotation> implann = null;
		if (annotation instanceof CheckerAnnotation) {
			chann = (CheckerAnnotation) annotation;
			implann = chann.value();
		} else {
			chann = annotation.annotationType().getAnnotation(CheckerAnnotation.class);
			implann = annotation.annotationType();
		}
		if (chann == null) return null;
		
		log.trace("Instantiating checker for " + chann);
		
		// Instantiate the checker
		Checker ch;
		Class<? extends Checker> chclazz;
		try {
			chclazz = (Class<? extends Checker>) Class.forName(implann.getName() + "Checker");
			ch = chclazz.newInstance();
		} catch (Exception e1) {
			log.error("Error instantiating checker " + e1.getMessage());
			throw new IllegalArgumentException(e1);
		}
		
		// Init with CheckerAnnotation pairs
		String[] init = chann.init();
		for (int i = 0; i < init.length;) {
			String k = init[i++];
			String v = init[i++];
			try {
				BeanUtils.setProperty(ch, k, v);
			} catch (Exception e) {
				log.warn("Error processing property \"" + k + "\" from " + chann + " " + e.getMessage());
			}
		}
		
		if (chann != annotation) {
			// Copy all properties from annotation to checker
			Method[] methods = annotation.annotationType().getDeclaredMethods();
			for (Method getm : methods) {
				if (getm.getParameterTypes().length > 0) continue;
				try {
					Object val = getm.invoke(annotation);
					BeanUtils.setProperty(ch, getm.getName(), val);
				} catch (Exception e) {
					log.error("Error processing property \"" + getm.toString() + "\" on " + chclazz + " " + e.getMessage());
				}
			}
		}
		
		return ch;
	}
}
