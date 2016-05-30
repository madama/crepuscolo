package net.etalia.crepuscolo.auth;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import net.etalia.crepuscolo.check.CheckAspect;
import net.etalia.crepuscolo.check.CheckMode;
import net.etalia.crepuscolo.check.CheckNoChecks;
import net.etalia.crepuscolo.check.CheckerFactory;
import net.etalia.crepuscolo.domain.DummyBean;
import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.test.BaseTestSpring;
import net.etalia.jalia.spring.JaliaParametersFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

public class SerializationAuthTest {

	private CrepuscoloObjectMapper om = null;

	@Before
	public void setupAspect() {
		JaliaParametersFilter.clean();
		CheckAspect.aspectOf().setCheckerFactory(new CheckerFactory());
		om = new CrepuscoloObjectMapper();
		om.setCheckerFactory(new CheckerFactory());
	}

	@After
	public void clearMockCheck() {
		MockCheckChecker.reset();
	}

	@Test
	public void serializeCheck() throws Exception {
		DummyBean db = new DummyBean();
		db.setAge(10);
		db.setEmail("email@mail.com");
		db.setName("Simone");

		String str = om.writeValueAsString(db);
		System.out.println(str);
		assertTrue(str.contains("age"));

		MockCheckChecker.reset();
		MockCheckChecker.checkFail.add("getterCheck");

		str = om.writeValueAsString(db);
		System.out.println(str);
		assertFalse(str.contains("age"));
	}

	@Test
	public void deserializeCheck() throws Exception {

		String json = "{'age':10,'email':'email@email.com','name':'Simone'}";
		DummyBean db = om.readValue(json.replace('\'', '"'), DummyBean.class);
		
		assertThat(db.getAge(), equalTo(10));

		MockCheckChecker.reset();
		MockCheckChecker.checkFail.add("setterCheck");
		
		// Access from outside should be granted (for example from hibernate)
		db.setAge(20);
		assertThat(db.getAge(), equalTo(20));

		db = om.readValue(json.replace('\'', '"'), DummyBean.class);
		assertThat(db.getAge(), equalTo(0));
	}

	@Test
	@Ignore
	@CheckMode("getterCheck")
	public void modeSerializationCheck() throws Exception {
		DummyBean db = new DummyBean();
		db.setAge(10);
		db.setEmail("email@mail.com");
		db.setName("Simone");

		String str = om.writeValueAsString(db);
		System.out.println(str);
		assertFalse(str.contains("age"));
	}

	@Test
	@CheckNoChecks
	public void nochecksTest() throws Exception {
		DummyBean db = new DummyBean();
		db.setAge(10);
		db.setEmail("email@mail.com");
		db.setName("Simone");

		MockCheckChecker.reset();
		MockCheckChecker.checkFail.add("getterCheck");

		String str = om.writeValueAsString(db);
		System.out.println(str);
		assertTrue(str.contains("age"));
	}

}
