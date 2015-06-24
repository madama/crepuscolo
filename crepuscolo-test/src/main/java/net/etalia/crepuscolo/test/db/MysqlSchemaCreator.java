package net.etalia.crepuscolo.test.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySql specific implementation of {@link SchemaCreator}. It supports dropping and recreating
 * the entire database before executing the DDL files, so that if the DDL does not contain DROP TABLE
 * instructions they can be used without problems.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
public class MysqlSchemaCreator extends BaseSchemaCreator{

	private String dbname;
	private boolean dropDb = false;

	@Override
	public void recreateSchema() throws SQLException, IOException {
		Connection connection = datasource.getConnection();
		if (dropDb) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				stmt.execute("DROP DATABASE " + dbname);
			} catch (Exception e) {
				// Can ignore this, the db could not be there
			} finally {
				if (stmt != null) stmt.close();
			}
			try {
				stmt = connection.createStatement();
				stmt.execute("CREATE DATABASE " + dbname);
				stmt.execute("USE " + dbname);
			} finally {
				if (!connection.getAutoCommit()) connection.commit();
				connection.close();
				stmt.close();
				stmt = null;
			}
		}
		super.recreateSchema();
	}

	/**
	 * Whether to drop and recreate the DB before executing DDL files. If set to true, 
	 * {@link #setDatabaseName(String)} must be set with the right MySql database name.
	 * @param dropDb true if DROP DATABASE and CREATE DATABASE must be used before DDL execution, false otherwise.
	 */
	public void setDropDb(boolean dropDb) {
		this.dropDb = dropDb;
	}

	/**
	 * The name of the database to drop and re-create if {@link #setDropDb(boolean)} is set to true.
	 * @param name
	 */
	public void setDatabaseName(String name) {
		this.dbname = name;
	}

}
