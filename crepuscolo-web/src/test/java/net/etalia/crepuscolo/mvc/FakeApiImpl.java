package net.etalia.crepuscolo.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import net.etalia.crepuscolo.domain.DummyBean;
import net.etalia.crepuscolo.utils.ChainMap;
import net.etalia.crepuscolo.utils.HttpException;
import net.etalia.crepuscolo.utils.annotations.Sudo;

@Controller
public class FakeApiImpl implements FakeApi {

	@Override
	public DummyBean getLastBean() {
		return mockDummyBean();
	}

	@Override
	@Sudo
	@RequestMapping(value="/bean/{id:.*}", method=RequestMethod.GET)
	public @ResponseBody DummyBean getBean(@PathVariable("id") String id) {
		DummyBean bean = mockDummyBean(id);
		return bean;
	}

	@Override
	public boolean deleteBean(String id) {
		return true;
	}

	@Override
	public List<DummyBean> searchByName(String name) {
		List<DummyBean> ret = new ArrayList<DummyBean>();
		ret.add(mockDummyBean());
		ret.add(mockDummyBean());
		return ret;
	}

	@Override
	@Sudo
	public DummyBean updateBean(String id, DummyBean bean) {
		bean.setId(id);
		return bean;
	}

	@Sudo
	private static DummyBean mockDummyBean() {
		DummyBean bean = new DummyBean();
		bean.setEmail("foo@bar.baz");
		bean.setName("Foo Bar");
		bean.setAge(37);
		return bean;
	}

	@Sudo
	private static DummyBean mockDummyBean(String id) {
		DummyBean ret = mockDummyBean();
		ret.setId(id);
		return ret;
	}

	public static void assertBean(DummyBean bean) {
		assertNotNull("Bean was null", bean);
		assertEquals("Wrong name", "Body text", bean.getName());
		assertEquals("Wrong lang", "en", bean.getEmail());
		assertEquals("Wrong subtitle", "subtitle", bean.getAge());
	}

	private int cnt = 0;

	@Override
	public @ResponseBody Map<String, String> randomlyFail(@PathVariable("a") String a, @RequestParam("b") String b) {
		if (a.equals("fail")) throw new HttpException().property("fail", true).setRetries(3);
		cnt++;
		if (cnt % 3 != 0) throw new HttpException().property("test", b).setRetries(3);
		return new ChainMap<String>(a, b);
	}

	@Override
	public @ResponseBody Map<String, String> randomlyFailPut(@RequestBody Map<String, String> map) {
		if (map.containsKey("fail")) throw new HttpException().property("test", map).setRetries(3);
		cnt++;
		if (cnt % 3 != 0) throw new HttpException().property("test", map).setRetries(3);
		return map;
	}

}
