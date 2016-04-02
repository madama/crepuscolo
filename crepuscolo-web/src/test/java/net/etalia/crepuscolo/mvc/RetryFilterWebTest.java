package net.etalia.crepuscolo.mvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import net.etalia.crepuscolo.json.JsonHttpExceptionHandler;
import net.etalia.crepuscolo.mvc.httpclient.HttpClientCaller;
import net.etalia.crepuscolo.mvc.httpclient.HttpClientHelper;
import net.etalia.crepuscolo.test.EmbedWeb;
import net.etalia.crepuscolo.utils.ChainMap;

@Ignore
public class RetryFilterWebTest {

	@Test
	public void test() throws Exception {
		EmbedWeb ew = EmbedWeb.create()
		.addService("fake", FakeApi.class, new FakeApiImpl())
		.addBean("jsonException", new JsonHttpExceptionHandler())
		.start();

		try {
			HttpClientCaller<FakeApi> caller = new HttpClientCaller<>(FakeApi.class);
			caller.setBaseUrl(ew.getBaseUrl() + "/fake/");
			caller.setHttpClient(new HttpClientHelper().createDefaultClient(10, 10000000));
			
			{
				Map<String, String> ret = caller.method(caller.service().randomlyFail("test", "1")).execute().cast();
				assertThat(ret, hasEntry("test", "1"));
			}
			
			try {
				Map<String, String> ret = caller.method(caller.service().randomlyFail("fail", "1")).execute().cast();
				fail("Should throw exception");
			} catch (Exception e) {
				assertThat(e, instanceOf(JsonedException.class));
				JsonedException je = (JsonedException) e;
				//assertThat(je.getProperties(), hasEntry(equalTo("stack"), notNullValue()));
				assertThat(je.getProperties(), hasEntry("fail", (Object)true));
			}
			
			{
				Map<String, String> ret = caller.method(caller.service().randomlyFailPut(new ChainMap<String>("test", "1"))).accept(200).execute().cast();
				assertThat(ret, hasEntry("test", "1"));
			}
			
			try {
				Map<String, String> ret = caller.method(caller.service().randomlyFailPut(new ChainMap<String>("fail", "1"))).accept(200).execute().cast();
				fail("Should throw exception");
			} catch (Exception e) {
				assertThat(e, instanceOf(JsonedException.class));
				JsonedException je = (JsonedException) e;
				//assertThat(je.getProperties(), hasEntry(equalTo("stack"), notNullValue()));
				assertThat(je.getProperties(), hasEntry(equalTo("test"), notNullValue()));
				assertThat((Map<String,String>)je.getProperties().get("test"), hasEntry("fail", "1"));
			}
			
			
		} finally {
			ew.stop();
		}
	}

}
