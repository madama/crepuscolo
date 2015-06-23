package net.etalia.crepuscolo.check;

import net.etalia.crepuscolo.services.StorageService;
import net.etalia.crepuscolo.utils.annotations.Sudo;

public aspect Pointcuts {

	public pointcut insideDomain() : within(net.etalia.domain..*);

	public pointcut insideTest() : within((@(org.junit..*) *)) || withincode(@(org.junit..*) * *(..));

	public pointcut flowStorage() : cflow(execution(* StorageService+.*(..)));

	public pointcut flowTest() : cflow(execution(* *.*(..)) && insideTest());

	public pointcut withSudo() : within(@Sudo *) || withincode(@Sudo * *(..)) || withincode(@Sudo new(..)) || call(@Sudo * *.*(..)) || call(@Sudo *.new(..));

}
