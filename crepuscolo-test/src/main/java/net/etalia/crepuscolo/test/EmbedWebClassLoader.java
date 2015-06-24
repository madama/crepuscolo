package net.etalia.crepuscolo.test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.jetty.webapp.WebAppClassLoader;

public class EmbedWebClassLoader extends WebAppClassLoader {

	public EmbedWebClassLoader(ClassLoader parent, Context context) throws IOException {
		super(parent, context);
		initPath();
	}

	public EmbedWebClassLoader(Context context) throws IOException {
		super(context);
		initPath();
	}

	private void initPath() {
		ClassLoader base = Thread.currentThread().getContextClassLoader();
		addJarsFrom(base);
	}

	public void addJarsFrom(ClassLoader cl) {
		if (cl instanceof URLClassLoader) {
			URL[] urLs = ((URLClassLoader) cl).getURLs();
			for (URL url : urLs) {
				this.addURL(url);
			}
		}
		ClassLoader par = cl.getParent();
		if (par != null) addJarsFrom(par);
	}
	
	
	

	

}
