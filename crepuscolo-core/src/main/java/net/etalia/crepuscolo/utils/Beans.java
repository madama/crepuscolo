package net.etalia.crepuscolo.utils;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class Beans {

	public static <T> void configure(T bean, String conf) {
		if (Strings.nullOrBlank(conf)) return;
		if (conf.startsWith("[")) conf = conf.substring(1,conf.length() - 1);
		if (Strings.nullOrBlank(conf)) return;
		BeanWrapper bw = new BeanWrapperImpl(bean);
		List<String> split = Strings.splitAndTrim(conf, ",");
		for (String pair : split) {
			String[] kv = pair.split("=");
			bw.setPropertyValue(kv[0].trim(), kv[1].trim());
		}
	}

	public static boolean has(Object obj) {
		return obj != null;
	}

	public static boolean has(String str) {
		return Strings.notNullOrBlank(str);
	}

	public static boolean has(Boolean b) {
		return b != null && b.booleanValue();
	}

	public static boolean has(Number num) {
		return num != null && num.doubleValue() != 0;
	}

	public static boolean has(Collection<?> coll) {
		return coll != null && coll.size() > 0;
	}

}
