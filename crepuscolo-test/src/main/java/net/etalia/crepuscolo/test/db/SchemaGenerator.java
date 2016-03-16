package net.etalia.crepuscolo.test.db;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * use:
 * public static void main(String[] args) throws Exception {
 *   SchemaGenerator gen = new SchemaGenerator("net.etalia.crepuscolo.domain");
 *   gen.setPath("src/main/sql/");
 *   gen.generate(Dialect.MYSQL5);
 * }
 * @author daniele
 *
 */
public class SchemaGenerator {

	private Configuration cfg;
	private String path = "";

	public SchemaGenerator(String packageName) throws Exception {
		cfg = new Configuration();
		cfg.setProperty("hibernate.hbm2ddl.auto", "create");
		for (Class<Object> clazz : getClasses(packageName)) {
			cfg.addAnnotatedClass(clazz);
		}
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Method that actually creates the file.
	 * 
	 * @param dbDialect to use
	 */
	public void generate(Dialect dialect) {
		cfg.setProperty("hibernate.dialect", dialect.getDialectClass());
		SchemaExport export = new SchemaExport(cfg);
		export.setDelimiter(";");
		export.setOutputFile(path + "ddl_" + dialect.name().toLowerCase() + ".sql");
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
	public static enum Dialect {
		HSQL("org.hibernate.dialect.HSQLDialect"),
		MYSQL5("org.hibernate.dialect.MySQL5InnoDBDialect"),
		MYSQL57("org.hibernate.dialect.MySQL57InnoDBDialect");
		private String dialectClass;
		private Dialect(String dialectClass) {
			this.dialectClass = dialectClass;
		}
		public String getDialectClass() {
			return dialectClass;
		}
	}

}
