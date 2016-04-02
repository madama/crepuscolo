package net.etalia.crepuscolo.mvc;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import net.etalia.crepuscolo.domain.DummyBean;

public interface FakeApi {
	
	@RequestMapping(value="/bean/last", method=RequestMethod.GET)
	public DummyBean getLastBean();

	@RequestMapping(value="/bean/{id}", method=RequestMethod.GET)
	public @ResponseBody DummyBean getBean(@PathVariable("id") String id);

	@RequestMapping(value="/bean/{id}", method=RequestMethod.DELETE)
	public boolean deleteBean(@PathVariable("id") String id);
	
	@RequestMapping(value="/beans/search", method=RequestMethod.GET)
	public List<DummyBean> searchByName(@RequestParam("name") String name);

	@RequestMapping(value="/bean/{id}", method=RequestMethod.POST)
	public DummyBean updateBean(@PathVariable("id") String id, @RequestBody DummyBean bean);

	@RequestMapping(value="/rndfail/{a}", method=RequestMethod.GET)
	public @ResponseBody Map<String,String> randomlyFail(@PathVariable("a") String a, @RequestParam("b") String b);

	@RequestMapping(value="/rndfailPut", method=RequestMethod.PUT)
	public @ResponseBody Map<String,String> randomlyFailPut(@RequestBody Map<String,String> map);

}
