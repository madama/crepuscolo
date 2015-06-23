package net.etalia.crepuscolo.domain;

import net.etalia.crepuscolo.check.Pointcuts;
import net.etalia.crepuscolo.services.CreationService;

public aspect EntityCheck {

	// Check if Entity.setId() was called by a appropriate place
	declare error :
		!Pointcuts.withSudo()
		&& call(* Entity+.setId(..))
		&& (!Pointcuts.insideDomain())
		&& (!Pointcuts.insideTest())
		&& !(within(CreationService+))
		
		: "Cannot call setId directly, use CreationService.load";

	declare error : 
		!Pointcuts.withSudo()
		&& call(Entity+.new(..))
		&& (!Pointcuts.insideDomain())
		&& (!Pointcuts.insideTest())
		&& !(within(CreationService+))
		
		: "Cannot instantiate a domain class directly, use CreationService.newInstance";

	declare warning :
		(
				call(Entity+.new(..))
				||
				call(* Entity+.setId(..))
		)
		&& Pointcuts.insideTest()
		&& !Pointcuts.withSudo()
		
		: "This is permitted in test, but will not work in real code cause CreationService should be used instead";

	declare error : 
		!Pointcuts.withSudo()
		&& call(* Stored+.setCreationDate(..))
		&& (!Pointcuts.insideDomain())
		&& (!Pointcuts.insideTest())
		&& !(within(CreationService+))
		
		: "Cannot call setCreationDate directly, it is for exclusive use of Hibernate";

	declare warning :
		call(* Stored+.setCreationDate(..))
		&& Pointcuts.insideTest()
		&& !Pointcuts.withSudo()
		
		: "This is permitted in test, but will not work in real code cause setCreationDate is for exclusive use of Hibernate";

}
