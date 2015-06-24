package net.etalia.crepuscolo.todolist.webtest;

import java.util.Map;

import net.etalia.crepuscolo.codec.Digester;
import net.etalia.crepuscolo.json.CrepuscoloObjectMapper;
import net.etalia.crepuscolo.mvc.Caller;
import net.etalia.crepuscolo.mvc.Response;
import net.etalia.crepuscolo.services.CreationService;
import net.etalia.crepuscolo.test.BaseTestSpring;
import net.etalia.crepuscolo.test.EmbedWeb;
import net.etalia.crepuscolo.test.db.SchemaCreator;
import net.etalia.crepuscolo.todolist.controller.TodolistAPI;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "testweb.appctx.xml" })
public class BaseWebTest extends BaseTestSpring {

	protected Caller<TodolistAPI> api;

	private Caller<SchemaCreator> schemaCreator;

	@Autowired
	protected CreationService creation;

	@Autowired
	protected CrepuscoloObjectMapper eom;
	
	private boolean restartServer = false;
	private static EmbedWeb server;

	
	@Before
	@SuppressWarnings("unchecked")
	public void init() throws Exception {
		api = (Caller<TodolistAPI>) applicationContext.getBean("apiCaller");
		schemaCreator = (Caller<SchemaCreator>) applicationContext.getBean("schemaCreatorCaller");
		if (server == null) {
			server = EmbedWeb.forTest("net/etalia/crepuscolo/todolist/web");
			//server.setPort(8085);
			server.start();
			restartServer = false;
		}
	}

	@After
	public void destroy() throws Exception {
		if (restartServer && server != null) {
			server.stop();
			server = null;
		} else {
			schemaCreator.service().recreateSchema();
			Response<Void> resp = schemaCreator.voidMethod().accept(204).execute();
			Assert.assertEquals(204, resp.getStatusCode());
		}
	}

	@AfterClass
	public static void shutDown() throws Exception {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	protected void restartServerAtEnd() {
		restartServer = true;
	}

	protected String md5(String value) {
		return new Digester().md5(value).toBase64UrlSafeNoPad();
	}

	protected String[] _p(String... values) {
		if (values.length == 1 && values[0].indexOf(',') != -1) {
			return values[0].split(",");
		}
		return values;
	}

	/*
	 * Proxy methods
	 */
	// JSON Map
	protected Map<String, Object> _getAsMap(String authorization, Object method, String[] properties) {
		return api.method(method).withFields(properties).setHeader("Authorization", authorization).execute().asMap();
	}

}
