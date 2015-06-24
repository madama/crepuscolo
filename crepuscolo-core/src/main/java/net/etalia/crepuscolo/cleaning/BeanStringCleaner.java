package net.etalia.crepuscolo.cleaning;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class BeanStringCleaner {

	protected final static Logger log = Logger.getLogger(BeanStringCleaner.class.getName());

	public static String clean(String str) {
		return clean(str,false);
	}
	
	public static String clean(String str, boolean keepNewlines) {
		if (str == null) return null;
		// Check for HTML removal
		if ((str.contains("<") && str.contains(">")) || str.contains("&")) {
			Element body = Jsoup.parse(str).body();
			body.tagName("pre");
			str = body.text();
		}
		if (keepNewlines) {
			// Collapse all double spaces (and similar chars, like tabs, but not newlines) into single plain spaces 
			str = str.replaceAll("[ \\t]+", " ");
		} else {
			// Remove all new lines
			str = str.replaceAll("\\n", " ");
			// Collapse all double spaces (and similar chars, like tabs) into single plain spaces 
			str = str.replaceAll("\\s+", " ");
		}
		// Trim
		str = str.trim();
		
		return str;
	}
	
	private static class PropEntry {
		String name;
		boolean perform = true;
		boolean keepNewlines = false;
		
		PropEntry(String name) {
			this.name = name;
		}
		
		PropEntry(String name, StringCleaning ann) {
			this(name);
			if (ann != null) {
				this.perform = ann.perform();
				this.keepNewlines = ann.keepNewLines();
			}
		}
	}
	
	private static Map<Class<?>,List<PropEntry>> cleanProperties = new ConcurrentHashMap<Class<?>, List<PropEntry>>();
	
	public static <T> void cleanBean(T bean) {
		Class<? extends Object> clazz = bean.getClass();
		List<PropEntry> props = cleanProperties.get(clazz);
		if (props == null) {
			props = new ArrayList<PropEntry>();
			try {
				BeanInfo bi = Introspector.getBeanInfo(clazz);
				PropertyDescriptor[] pdescs = bi.getPropertyDescriptors();
				for (PropertyDescriptor pd : pdescs) {
					// Only act on strings
					if (!pd.getPropertyType().equals(String.class)) continue;
					// Cannot act on read only or write only properties
					if (pd.getWriteMethod() == null) continue;
					if (pd.getReadMethod() == null) continue;
					// Consider annotation StringCleaning
					StringCleaning ann = pd.getReadMethod().getAnnotation(StringCleaning.class);

					PropEntry pe = new PropEntry(pd.getName(), ann);
					if (pe.perform) props.add(pe);
				}
			} catch (IntrospectionException e) {
				log.log(Level.WARNING, "Error while introspecting " + clazz.getName(), e);
			}
			cleanProperties.put(clazz, props);
		}
		if (props.size() == 0) return;
		
		for (PropEntry pe: props) {
			try {
				String val = BeanUtils.getSimpleProperty(bean, pe.name);
				if (val == null) continue;
				val = clean(val, pe.keepNewlines);
				if (val.length() == 0) val = null;
				BeanUtils.setProperty(bean, pe.name, val);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error while string cleaning " + clazz.getName() + "." + pe.name, e);
			}
		}
		
		return;
	}
	
	private static Map<Class<?>,List<String>> recurseProperties = new ConcurrentHashMap<Class<?>, List<String>>();
	
	public static <T> void recurseCleanBean(T bean) {
		recurseCleanBean(bean, new HashSet<Object>());
	}
	
	private static <T> void recurseCleanBean(T bean, Set<Object> dones) {
		if (dones.contains(bean)) return;
		cleanBean(bean);
		dones.add(bean);
		Class<? extends Object> clazz = bean.getClass();
		List<String> props = recurseProperties.get(clazz);
		if (props == null) {
			props = new ArrayList<String>();
			if (!clazz.isAnnotationPresent(StringCleaning.class)) {
				try {
					BeanInfo bi = Introspector.getBeanInfo(clazz);
					PropertyDescriptor[] pdescs = bi.getPropertyDescriptors();
					for (PropertyDescriptor pd : pdescs) {
						// Only act on lists or non primitives
						Class<?> type = pd.getPropertyType();
						if (
							!Iterable.class.isAssignableFrom(type) && ( 
								type.isPrimitive()
								|| type.getName().startsWith("java.lang")
								|| !type.getPackage().getName().startsWith(clazz.getPackage().getName())
							)) continue;
						// Cannot act on write only properties
						if (pd.getReadMethod() == null) continue;
						// Skip getters annotated with NoStringCleaning
						if (pd.getReadMethod().isAnnotationPresent(StringCleaning.class)) continue;
						
						props.add(pd.getName());
					}
				} catch (IntrospectionException e) {
					log.log(Level.WARNING, "Error while introspecting " + clazz.getName(), e);
				}
			}
			recurseProperties.put(clazz, props);
		}
		if (props.size() == 0) return;
		
		for (String pname : props) {
			try {
				Object val = PropertyUtils.getProperty(bean, pname);
				if (val == null) continue;
				if (val instanceof Iterable) {
					for (Object subbean : (Iterable)val) {
						if (!subbean.getClass().getPackage().getName().startsWith(clazz.getPackage().getName())) continue;
						recurseCleanBean(subbean, dones);
					}
				} else {
					recurseCleanBean(val, dones);
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Error while string cleaning " + clazz.getName() + "." + pname, e);
			}
		}
		
		return;
	}

	public static <T> Map<T,String> cleanMap(Map<T,String> src, boolean keepNewlines) {
		return int_cleanMap(src, keepNewlines, true);
	}

	public static <T> Map<T,String> cleanMap(Map<T,String> src) {
		return cleanMap(src, false);
	}	

	public static <T> Map<T,String> int_cleanMap(Map<T,String> src, boolean keepNewlines, boolean retry) {
		for (Map.Entry<T, String> entry : src.entrySet()) {
			String value = entry.getValue();
			if (value == null) continue;
			value = clean(value, keepNewlines);
			try {
				entry.setValue(value);
			} catch (Exception e) {
				if (!retry) throw new IllegalStateException(e);
				HashMap<T, String> alt = new HashMap<T, String>(src);
				return int_cleanMap(alt, keepNewlines, false);
			}
		}
		return src;
	}

}
