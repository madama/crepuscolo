package net.etalia.crepuscolo.utils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.etalia.crepuscolo.domain.Authenticable;


/**
 * Tells the custom AspectJ errors to skip the check cause I know what I'm doing.
 * <p>
 * This can be used on a method that call something prohibited OR on a method that can BE CALLED
 * despite prohibitions.
 * </p>
 * <p>
 * For example, it's prohibited by {@link EntityCheck} to call a {@link Entity} subclass
 * constructor directly. Now suppose a class like {@link Authenticable} has the following constructor :
 * <pre>
 *   public User(User another) {
 *     // Do something
 *   }
 * </pre>
 * And we want to call it from a service method somewher, we can annotate the service method with
 * {@link Sudo} :
 * <pre>
 * public class ServiceX {
 *   @Sudo
 *   public void doSomethingProhibited() {
 *     User u = new User(otherUser);
 *     //...
 *   }
 * }
 * </pre>
 * If however that constructor is explicitly declared to be called, despite the normal prohibitions
 * on constructors, then it's better to place the Sudo annotation on the constructor itself,
 * instead of polluting everywhere it's used.
 * </p> 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
public @interface Sudo {

}
