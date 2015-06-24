package net.etalia.crepuscolo.test.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Creates a schema on a db, useful for running spring integrated tests that need a clean db.
 * 
 * See {@link BaseSchemaCreator} for details of the base implementation.
 *  
 * @author Simone Gianni <simoneg@apache.org>
 */
public interface SchemaCreator {

	/**
	 * Creates the schema on the DB. Details about what db on what datasource depend on the implementation.
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	@RequestMapping(value = "/recreate", method = RequestMethod.POST)
	public @ResponseStatus(HttpStatus.NO_CONTENT) void recreateSchema() throws SQLException, IOException;

	/**
	 * Determines which files are used for database creation.
	 * @param files A list of files (usually Spring {@link Resource}s) to use to create the schema.
	 */
	public void setFiles(List<String> files);

	/**
	 * Whether to create the database as soon as this bean is instantiated, or defer to later when the {@link #recreateSchema()} is called.
	 * 
	 * This is useful cause sometimes some other beans in the context assume the datbase is already there when they are
	 * instantiated, even before any method is called, so the database must be ready during instantiation, using 
	 * Spring's depends-on or similar configurations.
	 * 
	 * @param prepare true is {@link #recreateSchema()} must be called automatically during bean instantiation, false otherwise.
	 */
	public void setPrepareBefore(boolean prepare);

}
