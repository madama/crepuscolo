package net.etalia.crepuscolo.test.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(value="/schema")
public class HsqlSchemaCreator extends BaseSchemaCreator {

	private boolean dropSchema = true;

	@Override
	@RequestMapping(value = "/recreate", method = RequestMethod.POST)	
	public @ResponseStatus(HttpStatus.NO_CONTENT) void recreateSchema() throws SQLException, IOException {
		Connection connection = datasource.getConnection();
		if (dropSchema) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				stmt.execute("DROP SCHEMA PUBLIC CASCADE");
			} finally {
				if (stmt != null) stmt.close();
				if (!connection.getAutoCommit()) connection.commit();
				connection.close();
			}
		}
		super.recreateSchema();
	}

	public void setDropSchema(boolean dropSchema) {
		this.dropSchema = dropSchema;
	}

}
