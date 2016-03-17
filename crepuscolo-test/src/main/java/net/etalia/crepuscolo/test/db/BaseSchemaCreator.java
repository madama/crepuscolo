package net.etalia.crepuscolo.test.db;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * Basic implementation of {@link SchemaCreator}, that works using a {@link DataSource} and DDL text (sql) files.
 * 
 * It also implements the default {@link #setPrepareBefore(boolean)} strategy.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
public class BaseSchemaCreator implements SchemaCreator, ApplicationContextAware, InitializingBean {

	protected final static Logger log = Logger.getLogger(BaseSchemaCreator.class.getName());

	protected List<String> files = null;
	protected DataSource datasource = null;
	protected ApplicationContext applicationContext;
	protected boolean prepareBefore;

	@Override
	public void setFiles(List<String> files) {
		this.files = files;
	}

	public void setDatasource(DataSource datasource) {
		this.datasource = datasource;
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.applicationContext = ctx;
	}

	/**
	 * Default implementation, simply executes the files on a connection obtained from the {@link DataSource},
	 * using {@link #executeFiles(Connection, String)} and the default SQL delimiter ";".  
	 */
	public void recreateSchema() throws SQLException, IOException {
		Connection connection = datasource.getConnection();
		try {
			executeFiles(connection, ";");
		} finally {
			if (!connection.getAutoCommit()) connection.commit();
			connection.close();
		}
	}

	/**
	 * "Executes" the text SQL files given in configuration via {@link #setFiles(List)} on the database.
	 * @param connection The connection to use 
	 * @param goDelimiter The delimiter for statement, it is ";" by default in SQL, but some other database may use other delimiters.
	 * @throws SQLException
	 * @throws IOException
	 */
	protected void executeFiles(Connection connection, String goDelimiter) throws SQLException, IOException {
		Statement stmt = connection.createStatement();
		try {
			for (String file : this.files) {
				Resource res = applicationContext.getResource(file);
				List<Closeable> close = new ArrayList<Closeable>();
				try {
					InputStream stream = res.getInputStream();
					close.add(stream);
					InputStreamReader isr = new InputStreamReader(stream);
					close.add(isr);
					BufferedReader br = new BufferedReader(isr);
					close.add(br);
					
					String line = null;
					StringBuilder allcommand = new StringBuilder();
					while ((line = br.readLine()) != null) {
						line = line.trim();
						if (line.length() == 0) continue;
						if (line.startsWith("--")) continue;
						if (line.startsWith("#")) continue;
						allcommand.append(line);
						allcommand.append('\n');
						if (line.contains(goDelimiter)) {
							log.fine("Execute: " + allcommand.toString());
							stmt.executeUpdate(allcommand.toString());
							allcommand = new StringBuilder();
						}
					}
				} finally {
					for (Closeable closeable : close) {
						try {
							closeable.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} finally {
			stmt.close();
		}
	}

	public void setPrepareBefore(boolean prepareBefore) {
		this.prepareBefore = prepareBefore;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.prepareBefore) this.recreateSchema();
	}

}
