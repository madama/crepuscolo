package net.etalia.crepuscolo.test.db;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class SchemaGenerator {

	private Configuration cfg;

	public static void main(String[] args) throws Exception {
		SchemaGenerator gen = new SchemaGenerator("net.etalia.crepuscolo.domain");
		gen.generate(Dialect.MYSQL5);
	}

	public SchemaGenerator(String packageName) throws Exception {
		cfg = new Configuration();
		cfg.setProperty("hibernate.hbm2ddl.auto", "create");
		for (Class<Object> clazz : getClasses(packageName)) {
			cfg.addAnnotatedClass(clazz);
		}
	}

	/**
	 * Method that actually creates the file.
	 * 
	 * @param dbDialect to use
	 */
	protected void generate(Dialect dialect) {
		cfg.setProperty("hibernate.dialect", dialect.getDialectClass());
		SchemaExport export = new SchemaExport(cfg);
		export.setDelimiter(";");
		export.setOutputFile("ddl_" + dialect.name().toLowerCase() + ".sql");
		export.execute(true, false, false, false);
	}

	/**
	 * Utility method used to fetch Class list based on a package name.
	 * 
	 * @param packageName
	 *            (should be the package containing your annotated beans.
	 */
	@SuppressWarnings("unchecked")
	private List<Class<Object>> getClasses(String packageName) throws Exception {
		List<Class<Object>> classes = new ArrayList<Class<Object>>();
		File directory = null;
		try {
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			if (cld == null) {
				throw new ClassNotFoundException("Can't get class loader.");
			}
			String path = packageName.replace('.', '/');
			URL resource = cld.getResource(path);
			if (resource == null) {
				throw new ClassNotFoundException("No resource for " + path);
			}
			directory = new File(resource.getFile());
		} catch (NullPointerException x) {
			throw new ClassNotFoundException(packageName + " (" + directory
					+ ") does not appear to be a valid package");
		}
		if (directory.exists()) {
			String[] files = directory.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".class")) {
					// removes the .class extension
					classes.add((Class<Object>) Class.forName(packageName + '.'
							+ files[i].substring(0, files[i].length() - 6)));
				}
			}
		} else {
			throw new ClassNotFoundException(packageName
					+ " is not a valid package");
		}

		return classes;
	}

	/**
	 * Holds the classnames of hibernate dialects for easy reference.
	 */
	protected static enum Dialect {
		MYSQL5("org.hibernate.dialect.MySQL5InnoDBDialect"),
		HSQL("org.hibernate.dialect.HSQLDialect");
		private String dialectClass;
		private Dialect(String dialectClass) {
			this.dialectClass = dialectClass;
		}
		public String getDialectClass() {
			return dialectClass;
		}
	}

}
