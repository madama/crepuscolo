/*
 * Copyright 2008-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.etalia.crepuscolo.utils;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.etalia.crepuscolo.utils.annotations.NotNullable;
import net.etalia.crepuscolo.utils.annotations.ThreadSafe;


@ThreadSafe
public abstract class Strings {
	public static final Charset UTF8 = Charset.forName("UTF-8");
	
	/**
	 * @return true is the given string is empty (length 0) or null.
	 */
	public static boolean nullOrEmpty(String s) {
		return s == null || 0 == s.length();
	}
	
	/**
	 * @return true is the given string is NOT empty (length 0) and NOT null.
	 */
	public static boolean notNullOrEmpty(String s) {
		return ! nullOrEmpty(s);
	}
	
	/**
	 * @return null if the given string is already null or is blank, the same string otherwise
	 */
	public static String nullIfBlank(String s) {
		if (nullOrEmpty(s)) return null;
		return s;
	}
	
	/**
	 * @return true is the given string is blank (length 0 or composed only of spaces, tabs or returns) or null.
	 */
	public static boolean nullOrBlank(String s) {
		return s == null || "".equals(s.trim());
	}
	
	/**
	 * @return true is the given string is NOT blank (length 0 or composed only of spaces, tabs or returns) and NOT null.
	 */
	public static boolean notNullOrBlank(String s) {
		return ! nullOrBlank(s);
	}

	/**
	 * @param value
	 * @param defString
	 * @return Return the value if not null, otherwise the default string
	 */
	public static String defaultIfNull(String value, String defString) {
		return nullOrBlank(value) ? defString : value;
	}

	/**
	 * Joins the given objects (converted using .toString) using the separator.
	 * 
	 * @param separator The separator to use.
	 * @param strings The objects (converted using .toString) to join together.
	 * @return the joined string
	 */
	public static String join(@NotNullable String separator, Object... strings) {
		int len = strings.length;
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < len; i++) {
			sb.append(strings[i].toString());
			if(i < len - 1) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	/**
	 * @see #join(String, Object...) 
	 */
	public static String join(@NotNullable String separator, @NotNullable Iterable<?> collection) {
		return join(separator, collection.iterator());
	}

	/**
	 * @see #join(String, Object...) 
	 */
	public static String join(@NotNullable String separator, @NotNullable Iterator<?> itr) {
		StringBuilder sb = new StringBuilder();
		while(itr.hasNext()) {
			sb.append(itr.next().toString());
			if(itr.hasNext()) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}
	
	/**
	 * Splits the given string using the given regex, trimming the resulting tokens.
	 * @param str The string to split
	 * @param regex The regex to split with
	 * @return the list of resulting tokens, trimmed to remove whitespaces
	 */
	public static List<String> splitAndTrim(@NotNullable String str, @NotNullable String regex) {
		String[] arr = str.split(regex);
		LinkedList<String> l = new LinkedList<String>();
		for(String s : arr) {
			l.add(s.trim());
		}
		return l;
	}
	
	/**
	 * Joins the given path segments together, adding slashes where appropriate.
	 * @param segments A list of strings representing path segments.
	 * @return the segments joined with forward slash properly to form a valid path.
	 */
	public static String pathConcat(@NotNullable String... segments) {
		StringBuilder sb = new StringBuilder();
		boolean lastEndsWithSlash = false;
		boolean first = true;
		for(String s : segments) {
			if(first) {
				sb.append(s);
				first = false;
			} else {
				if((! lastEndsWithSlash) && (! s.startsWith("/"))) {
					sb.append("/");
					sb.append(s);
				} else if(lastEndsWithSlash && s.startsWith("/")) {
					sb.append(s.substring(1));
				} else {
					sb.append(s);
				}
			}
			if(s.endsWith("/")) {
				lastEndsWithSlash = true;
			}
		}
		return sb.toString();
	}
	
	/**
	 * @see #pathConcat(String...)
	 */
	public static String pathConcat(@NotNullable Collection<String> segments) {
		return pathConcat(segments.toArray(new String[segments.size()]));
	}	

	public static String F(@NotNullable String pattern, Object... arguments) {
		for(int i = 0; i < arguments.length; i++) {
			if(null == arguments[i]) throw new NullPointerException("arguments " + i + " is null");
  		}
		return MessageFormat.format(pattern, arguments);
	}

	public static <T> String J(String separator, @NotNullable List<T> someT) {
		StringBuilder result = new StringBuilder();
		for (T t : someT) {
			result.append(t);
			result.append(separator);
		}
		return result.toString().replaceAll(separator.concat("$"), "");
	}

	public static String J(String separator, String decorator, @NotNullable List<String> strings) {
		StringBuilder result = new StringBuilder();
		for (String string : strings) {
			result.append(decorator);
			result.append(string);
			result.append(decorator);
			result.append(separator);
		}
		return result.toString().replaceAll(separator.concat("$"), "");
	}

	public static String J(String separator, @NotNullable String... strings) {
		StringBuilder result = new StringBuilder();
		for (String string : strings) {
			result.append(string);
			result.append(separator);
		}
		return result.toString().replaceAll(separator.concat("$"), "");
	}

	public static boolean isUpperCase(String s) {
		for (int i = 0; i < s.length(); i++) {
			int ch = s.codePointAt(i);
			if (Character.isLetter(ch)) {
				if (!Character.isUpperCase(ch)) return false;
			}
		}
		return true;
	}
	
	public static boolean isLowerCase(String s) {
		for (int i = 0; i < s.length(); i++) {
			int ch = s.codePointAt(i);
			if (Character.isLetter(ch)) {
				if (!Character.isLowerCase(ch)) return false;
			}
		}
		return true;
	}
	
}
