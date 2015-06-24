package net.etalia.crepuscolo.check;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple checker to perform AND between many checkers.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
public class ChAndChecker implements Checker, Transformer {

	/**
	 * Checkers to check in AND inside this AndChecker instance
	 */
	protected List<Checker> checkers = new ArrayList<Checker>();

	public ChAndChecker() {
		
	}

	public ChAndChecker(List<Checker> checkers) {
		this.checkers = checkers;
	}

	public void addChecker(Checker checker) {
		checkers.add(checker);
	}

	public List<Checker> getCheckers() {
		return checkers;
	}

	public void setValue(CheckerAnnotation[] annotations) {
		for (CheckerAnnotation ann : annotations) {
			this.addChecker(CheckerFactory.buildChecker(ann));
		}
	}

	@Override
	public int check(CheckPoint p) {
		for (Checker check : checkers) {
			int code = check.check(p); 
			if (code != 0) return code;
		}
		return 0;
	}

	@Override
	public Object transform(CheckPoint p, Object obj) {
		for (Checker check : checkers) {
			if (check instanceof Transformer) {
				obj = ((Transformer) check).transform(p, obj);
			}
		}
		return obj;
	}

}
