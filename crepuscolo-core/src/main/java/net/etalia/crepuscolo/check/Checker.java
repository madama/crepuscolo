package net.etalia.crepuscolo.check;

/**
 * Interface for an authorization checker.
 * 
 * <p>
 * A checker is responsible of checking a single condition on the given {@link CheckPoint}.
 * Multiple checks can be implemented combining more checkers.
 * </p>
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
public interface Checker {

	public int check(CheckPoint p);

}

