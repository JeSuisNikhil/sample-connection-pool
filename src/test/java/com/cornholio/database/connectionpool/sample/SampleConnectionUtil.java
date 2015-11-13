package com.cornholio.database.connectionpool.sample;

import com.cornholio.database.connectionpool.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A not so typical connection utility class. Used for testing only.
 *
 * @author nikhilagarwal
 */
public class SampleConnectionUtil {

	/**
	 * Get a connection object
	 *
	 * @throws SQLException
	 */
	public static Connection getConnection() throws SQLException {
		SampleConnectionPoolFactory connectionFactory = SampleConnectionPoolFactory.getInstance();
		String jndiName = SampleConnectionConstants.SAMPLE_JNDI_NAME;

		ConnectionPool connectionPool = connectionFactory.getConnectionPool(jndiName);
		return connectionPool.getConnection();
	}

	/**
	 * Get a connection object
	 *
	 * @throws SQLException
	 */
	public static Connection getConnection(String jndiName) throws SQLException {
		SampleConnectionPoolFactory connectionFactory = SampleConnectionPoolFactory.getInstance();
		ConnectionPool connectionPool = connectionFactory.getConnectionPool(jndiName);
		return connectionPool.getConnection();
	}
}