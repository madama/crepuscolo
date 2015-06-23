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

import static net.etalia.crepuscolo.utils.Strings.nullOrBlank;
import static net.etalia.crepuscolo.utils.Strings.nullOrEmpty;

import java.lang.reflect.Constructor;
import java.util.Collection;

import net.etalia.crepuscolo.utils.annotations.ThreadSafe;

@ThreadSafe
public enum Check {
	nullpointer(NullPointerException.class), 
	illegalstate(IllegalStateException.class),
	illegalargument(IllegalArgumentException.class),
	runtime(RuntimeException.class),
	unsupportedoperation(UnsupportedOperationException.class);

	private Class<? extends RuntimeException> exClass;
	private Constructor<? extends RuntimeException> exStringCtor;
	private Check(Class<? extends RuntimeException> ex) {
		exClass = ex;
		try {
			exStringCtor = exClass.getDeclaredConstructor(new Class<?>[] {String.class});
		} catch (Exception e) {
			throw new IllegalStateException();
		}
	}

	private RuntimeException buildEx(String reason) {
		try {
			if (exStringCtor != null) {
				return exStringCtor.newInstance(reason);
			}
			return exClass.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException();
		}
	}

	private RuntimeException buildEx() {
		try {
			if (exStringCtor != null) {
				return exStringCtor.newInstance();
			}
			return exClass.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException();
		}
	}

	public static void notNulls(String reason, Object... objs) throws NullPointerException {
		for(Object o : objs) {
			notNull(reason, o);
		}
	}
	
	public static void oneNotNull(String reason, Object... objs) throws NullPointerException {
		for (Object o : objs) {
			if (o != null) return;
		}
		notNull(reason, null);
	}

	public static <T> T notNull(String reason, T o) {
		if(o == null) {
			throw nullpointer.buildEx(reason);
		}
		return o;
	}

	public static <T> T notNull(T o) {
		if(o == null) {
			throw nullpointer.buildEx();
		}
		return o;
	}

	public void assertNull(String reason, Object o) {
		if(o != null) {
			throw buildEx(reason);
		}
	}

	public void assertEquals(String reason, Object o1, Object o2) {
		if(o1 == null || o2 == null || !o1.equals(o2)) {
			throw buildEx(reason);
		}
	}

	public <T> T assertNotNull(String reason, T o) {
		if(o == null) {
			throw buildEx(reason);
		}
		return o;
	}

	public <T> Collection<T> notNullOrEmpty(String reason, Collection<T> coll) {
		assertFalse(reason, coll == null || coll.size() == 0);
		return coll;
	}

	public String notNullOrEmpty(String reason, String s) {
		assertFalse(reason, nullOrEmpty(s));
		return s;
	}

	public String notNullOrBlank(String reason, String s) {
		assertFalse(reason, nullOrBlank(s));
		return s;
	}

	public <T> T fail(String reason) {
		throw buildEx(reason);
	}

	public <T> T assertTrue(String reason, boolean cond) {
		if(! cond) {
			fail(reason);
		}
		return null;
	}

	public <T> T assertFalse(String reason, boolean cond) {
		if(cond) {
			fail(reason);
		}
		return null;
	}

	public static <T> T ifNull(T o, T defaultVal) {
		return o == null ? defaultVal : o;
	}

	public static String ifNullOrEmpty(String s, String defaultVal) {
		return nullOrEmpty(s) ? defaultVal : s;
	}

	public static String ifNullOrBlank(String s, String defaultVal) {
		return nullOrBlank(s) ? defaultVal : s;
	}

}
