package net.etalia.crepuscolo.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Utility class to start up an embedded jetty, with servlets configured for mocked
 * services, to test the full functionality of http apis.
 * 
 * It can be used declaratively or programmatically. 
 * 
 * The declarative way consist in creating a /testName/web resource folder (in src/test/resources),
 * and place there everything needed (at least a WEB-INF/web.xml file, probably also an applicationContext.xml
 * file, eventually name-servlet.xml files).
 * 
 * The declarative way is as simple as :
 * 
 * <pre>
 *   EmbedWeb.forTest("name").start();
 * </pre>
 * 
 * Where "name" should correspond to a /name/web/ folder.
 * 
 * 
 * The programmatic way instead takes care of those files using default files.
 * 
 * To use it this way :
 * 
 * <pre>
 *   EmbedWeb.create().addService("apiname", ApiInterface.class, apiMock).start();
 * </pre>
 * 
 * "apiname" is used for the url, so the api will be accessible on http://localhost:8085/apiname/ .
 * 
 * The port can itself be configured using setPort().
 * 
 * 
 *   
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
public class EmbedWeb {

	private int port = 0;

	private WebAppContext wac;

	private Server server;

	private File mockFolder = null;
	private List<File> toDelete = new ArrayList<File>();
	private Map<String, Object> beans = new HashMap<String, Object>();

	private boolean sealed = false;
	private boolean injected = false;

	public static EmbedWeb forTest(String testName) throws Exception {
		EmbedWeb ret = new EmbedWeb();
		ret.wac = new WebAppContext();
		File tempDir = File.createTempFile("webmock", "test");
		tempDir.delete();
		tempDir.mkdir();
		ret.mockFolder = tempDir;
		ret.toDelete.add(tempDir);
		PathMatchingResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
		Resource resourceDir = patternResolver.getResource(testName);
		File srcDir = resourceDir.getFile();
		FileUtils.copyDirectory(srcDir, tempDir);
		ret.fixSystemClasses();
		ret.wac.setWar(tempDir.getAbsolutePath());
		return ret;
	}

	public static EmbedWeb create() throws Exception {
		EmbedWeb ret = new EmbedWeb();
		ret.wac = new WebAppContext();
		File tempfile = File.createTempFile("webmock", "test");
		tempfile.delete();
		tempfile.mkdir();
		ret.mockFolder = tempfile;
		ret.toDelete.add(tempfile);
		File webinf = new File(tempfile, "WEB-INF");
		webinf.mkdir();
		FileUtils.copyInputStreamToFile(EmbedWeb.class.getResourceAsStream("/test/web/WEB-INF/web.xml"), new File(webinf, "web.xml"));
		FileUtils.copyInputStreamToFile(EmbedWeb.class.getResourceAsStream("/test/web/WEB-INF/applicationContext.xml"), new File(webinf, "applicationContext.xml"));
		ret.fixSystemClasses();
		ret.wac.setWar(tempfile.getAbsolutePath());
		return ret;
	}

	public void fixSystemClasses() {
		ArrayList<String> scs = new ArrayList<String>(Arrays.asList(wac.getSystemClasses()));
		scs.add(EmbedWebIPCCore.class.getName());
		scs.add("com.mchange.");
		scs.add("com.mysql.");
		scs.add("com.amazonaws.");
		scs.add("org.mockito.");
		scs.add("net.etalia.test.MockSQS");
		wac.setSystemClasses(scs.toArray(new String[scs.size()]));
	}

	public EmbedWeb addSystemClass(String name) {
		String[] scs = wac.getSystemClasses();
		String[] nscs = new String[scs.length + 1];
		System.arraycopy(scs, 0, nscs, 0, scs.length);
		nscs[scs.length] = name;
		wac.setSystemClasses(nscs);
		return this;
	}

	public EmbedWeb seal() throws Exception {
		if (injected) throw new IllegalStateException("Cannot seal an EmbedWeb where services or beans have been added");
		EmbedWebClassLoader cl = new EmbedWebClassLoader(this.getClass().getClassLoader(), wac);
		wac.setClassLoader(cl);
		this.sealed = true;
		return this;
	}

	public EmbedWeb addFile(String resourceName) throws Exception {
		assertNotNull("Cannot add files on an EmbedWeb created with forTest", mockFolder);
		String relpath = resourceName.substring(resourceName.indexOf("/web/") + 5);
		FileUtils.copyInputStreamToFile(EmbedWeb.class.getResourceAsStream(resourceName), new File(mockFolder, relpath));
		return this;
	}

	public EmbedWeb addService(String name, Class<?> apiInterface, Object apiMock) throws Exception {
		if (sealed) throw new IllegalStateException("Cannot add services or beans to a sealed EmbedWeb");
		List<String> lines = IOUtils.readLines(EmbedWeb.class.getResourceAsStream("/test/web/WEB-INF/servlet.xml"));
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			line = line.replaceAll("\\{name\\}", name);
			line = line.replaceAll("\\{interface\\}", apiInterface.getName());
			beans.put(name, apiMock);
			lines.set(i, line);
		}
		FileUtils.writeLines(new File(mockFolder, "WEB-INF/" + name + "-servlet.xml"), lines);
		ServletHolder holder = new ServletHolder(name, DispatcherServlet.class);
		wac.addServlet(holder, "/" + name + "/*");
		this.injected = true;
		return this;
	}

	public EmbedWeb addBean(String name, Object bean) {
		if (sealed) throw new IllegalStateException("Cannot add services or beans to a sealed EmbedWeb");
		beans.put(name, bean);
		this.injected = true;
		return this;
	}

	public EmbedWeb start() throws Exception {
		EmbedWebIPCCore.removeServer();
		if (!this.injected) {
			this.seal();
			Class<?> serverIpc = wac.getClassLoader().loadClass(EmbedWebIPC.class.getName());
			serverIpc.getMethod("initServer").invoke(null);
		}
		final AtomicReference<Throwable> startException = new AtomicReference<Throwable>();
		Thread t = new Thread() {
			public void run() {
				try {
					server = new Server();
					
					LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10);
					ExecutorThreadPool pool = new ExecutorThreadPool(5, 10, 1000, TimeUnit.SECONDS, queue);
					server.setThreadPool(pool);
					
					if (beans.size() > 0) {
						// Register a servlet that will inject bean definitions inside the application context
						
						Filter filter = new Filter() {
							
							boolean done = false;
							
							@Override
							public void init(FilterConfig filterConfig) throws ServletException {
								WebApplicationContext appctx = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());
								ConfigurableListableBeanFactory bf = ((ConfigurableApplicationContext)appctx).getBeanFactory();
								for (Entry<String, Object> entry : beans.entrySet()) {
									bf.registerSingleton(entry.getKey(), entry.getValue());
								}
							}
	
							@Override
							public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
								chain.doFilter(request, response);
							}
	
							@Override
							public void destroy() {
							}
							
						};
						FilterHolder fh = new FilterHolder(filter);
						wac.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));
					}
					
					server.setHandler(wac);
					if (port == 0) {
						port = 8086;
						// Search for a free port
						while (port < 9000) {
							try {
								Connector connector=new SelectChannelConnector();
								connector.setPort(port);
								server.setConnectors(new Connector[]{connector});
								server.start();
								break;
							} catch (Exception e) {
								port++;
							}
						}
						if (port == 9000) {
							throw new IllegalStateException("Cannot find a free port between 8086 and 9000???");
						}
					} else {
						server = new Server(port);
						server.start();
					}
					
					System.setProperty("embedWebPort", String.valueOf(port));
					
				} catch (Throwable e) {
					// TODO return this
					e.printStackTrace();
					try {
						wac.stop();
					} catch (Exception e2) {}
					try {
						server.stop();
					} catch (Exception e2) {}
					startException.set(e);
				}
			}
		};
		
		t.start();
		t.join();
		
		Throwable exc = startException.get();
		if (exc != null)
			throw new IllegalStateException("Cannot start embedded web server", exc); 
		
		return this;
	}

	public void stop() throws Exception {
		wac.stop();
		server.stop();
		
		for (File f : toDelete) {
			FileUtils.deleteDirectory(f);
		}

		int cnt = 0;
		while (server.isRunning() && cnt < 10) {
			server.stop();
			Thread.sleep(1000);
			cnt++;
		}
	}

	public EmbedWeb setPort(int port) {
		this.port = port;
		return this;
	}

	public String getBaseUrl() {
		return "http://localhost:" + port;
	}

}