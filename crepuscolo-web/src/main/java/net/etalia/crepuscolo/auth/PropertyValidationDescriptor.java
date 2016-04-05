package net.etalia.crepuscolo.auth;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.metadata.ConstraintDescriptor;

import net.etalia.crepuscolo.utils.Beans;
import net.etalia.crepuscolo.utils.ChainMap;
import net.etalia.crepuscolo.validation.RegexpTransformer;

public class PropertyValidationDescriptor extends ArrayList<Map<String, Object>> {

	public <T> PropertyValidationDescriptor(Class<T> clazz, T instance, PropertyDescriptor bprop, javax.validation.metadata.PropertyDescriptor vprop) {
		// Check for validation
		if (vprop != null) {
			Set<ConstraintDescriptor<?>> cnsts = vprop.getConstraintDescriptors();
			for (ConstraintDescriptor<?> cdescr : cnsts) {
				Map<String,Object> def = new HashMap<String, Object>();
				Map<String,Object> attrs = cdescr.getAttributes();
				Map<String,String[]> regexps = new HashMap<String, String[]>();
				List<Class> classes = new ArrayList<Class>(cdescr.getConstraintValidatorClasses());
				if (cdescr.getAnnotation() != null) {
					Object annotation = cdescr.getAnnotation();
					if (annotation instanceof Annotation) {
						classes.add(((Annotation) annotation).annotationType());
					}
				}
				for (Class vclass : classes) {
					RegexpTransformer trans = findRegexpTransformer(vclass);
					if (trans == null) continue;
					regexps.put(trans.getName(), trans.getRegexp(attrs));
				}
				if (regexps.size() > 0) {
					def.put("re", regexps);
					if (Beans.has(cdescr.getGroups())) {
						def.put("groups", toClassSimpleNames(cdescr.getGroups()));
					}
					if (Beans.has(cdescr.getPayload())) {
						def.put("payload", toClassSimpleNames(cdescr.getPayload()));
					}
					def.put("message", attrs.get("message"));
					def.put("type", cdescr.getAnnotation().annotationType().getSimpleName());
					this.add(def);
				}
			}
		}
		if (bprop != null) {
			RegexpTransformer trans = findRegexpTransformer(bprop.getPropertyType());
			if (trans != null) {
				this.add(
						new ChainMap("re", 
								new ChainMap<String[]>(trans.getName(), trans.getRegexp(Collections.EMPTY_MAP))
						).add("type", bprop.getPropertyType().getSimpleName()).add("message", "_NOT_VALID")
						);
			}
		}
	}

	private List<String> toClassSimpleNames(Collection<?> clzs) {
		List<String> ret = new ArrayList<String>(clzs.size());
		for (Object object : clzs) {
			if (object == null) continue;
			if (object instanceof Class) {
				ret.add(((Class<?>)object).getSimpleName());
			} else {
				ret.add(object.getClass().getSimpleName());
			}
		}
		return ret;
	}

	private static Map<Class,RegexpTransformer> regexpTransformersCache = new HashMap<Class, RegexpTransformer>();

	private static RegexpTransformer findRegexpTransformer(Class vclass) {
		RegexpTransformer cached = regexpTransformersCache.get(vclass);
		if (cached != null) return cached;
		Class<? extends RegexpTransformer> rtclazz = null;
		// Try in the same package
		try {
			rtclazz = (Class<? extends RegexpTransformer>) Class.forName(vclass.getName() + "RegexpTransformer");
		} catch (Exception e) {
		}
		// Try in the default package
		try {
			rtclazz = (Class<? extends RegexpTransformer>) Class.forName("net.etalia.crepuscolo.validation.regexps." + vclass.getSimpleName() + "RegexpTransformer");
		} catch (Exception e) {
		}
		
		if (rtclazz == null) return null;
		try {
			cached = rtclazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		regexpTransformersCache.put(vclass, cached);
		return cached;
	}

}
