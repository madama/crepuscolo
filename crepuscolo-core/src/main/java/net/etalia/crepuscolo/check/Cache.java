package net.etalia.crepuscolo.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the value returned by this getter can be cached and does not need to
 * be computed each time.
 * <p>
 * This is to be used on getters that are not actually simple accessors but instead compute their
 * values based on some complex evaluation.
 * </p><p>
 * When placed on a getter, the expires and max parameters specify how to size and the evict policy
 * of the underlying cache.
 * </p><p>
 * When placed on a setter, specifies that the setter can be used for immediate eviction and replacement
 * of the cached value for the getter.
 * </p><p>
 * The expires parameters takes a string like the following : "1 year 2 months 1 week 3 days 5 hours 6 minutes 10 seconds"
 * or in a compressed version "1 y 2 mo 1 w 3 d 5 h 6 m".
 * </p><p>
 * The max parameter specifies a maximum number of elements to be kept in cache.
 * </p><p>
 * For more details on how these parameters are used, see {@link SimpleCache}.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Cache {

	String expires() default "0";
	
	int max() default -1;
	
}
