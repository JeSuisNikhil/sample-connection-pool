package com.cornholio.database.connectionpool.sample;

import com.cornholio.database.connectionpool.ConnectionPool;
import com.cornholio.database.connectionpool.ConnectionPoolBuilder;
import com.cornholio.database.connectionpool.ConnectionPoolProperties;
import com.cornholio.database.connectionpool.mock.MockInitialContext;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * This is a connection pool factory. Used for testing only. This class pretends to have a connection pool mapped to a data source.
 * 
 * @author nikhilagarwal
 */
public class SampleConnectionPoolFactory {
	private static final String LOG_INVALID_CONNECTION_PROPERTIES = "Invalid Connection Properties";
	// loggers and messages
	private static final String LOG_NEW_CONNECTION_POOL = "New connection pool created";
	private static volatile Hashtable<String, ConnectionPool> connections;
	private static volatile SampleConnectionPoolFactory instance;
	private static Logger logger;

	/**
	 * Private constructor to make it a singleton.
	 */
	private SampleConnectionPoolFactory() {
		super();
	}

	public static SampleConnectionPoolFactory getInstance() {
		if (instance == null) {
			synchronized (SampleConnectionPoolFactory.class) {
				if (instance == null) {
					instance = new SampleConnectionPoolFactory();
					connections = new Hashtable<>();
				}
			}
		}

		return instance;
	}

	private static Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger(SampleConnectionPoolFactory.class.getSimpleName());
		}
		return logger;
	}

	/**
	 * Helper method. Uses the connection pool builder to build a connection.
	 * 
	 * @param dataSourceJndi
	 * @param connectionPoolProperties
	 * @return Connection Pool
	 * @throws SQLException
	 */
	private ConnectionPool buildConnectionPool(String dataSourceJndi, ConnectionPoolProperties connectionPoolProperties) throws SQLException {
		ConnectionPool connectionPool = null;
		DataSource dataSource = null;
		try {
			dataSource = (DataSource) new MockInitialContext().lookup(dataSourceJndi);
			connectionPool =
					new ConnectionPoolBuilder().connectionTimeOut(connectionPoolProperties.getConnectionTimeOut()).dataSource(dataSource)
							.maxIdle(connectionPoolProperties.getMaxIdle()).maxSize(connectionPoolProperties.getMaxSize())
							.minSize(connectionPoolProperties.getMinSize())
							.timeBetweenPoolMaintenance(connectionPoolProperties.getTimeBetweenPoolMaintenance())
							.waitTimeOut(connectionPoolProperties.getWaitTimeOut()).build();
		} catch (NamingException e) {
			getLogger().log(Level.ERROR, LOG_INVALID_CONNECTION_PROPERTIES, e);
		}
		return connectionPool;
	}

	/**
	 * Get a connection pool associated with the given data source name
	 * 
	 * @param dataSourceJndi
	 * @return the connection pool associated with the data source
	 * @throws SQLException
	 *             if the connection pool could not be found/initialized
	 */
	public ConnectionPool getConnectionPool(String dataSourceJndi) throws SQLException {

		// if the connection pool doesn't already exist, create one
		ConnectionPool connectionPool = connections.get(dataSourceJndi);
		if (connectionPool == null) {
			synchronized (dataSourceJndi) {
				if (connectionPool == null) {
					connectionPool = buildConnectionPool(dataSourceJndi, getConnectionPoolProperties());
					connections.put(dataSourceJndi, connectionPool);
					if (getLogger().isInfoEnabled()) {
						getLogger().log(Level.INFO, LOG_NEW_CONNECTION_POOL);
					}
				}
			}
		}

		return connectionPool;
	}

	/**
	 * Gets connection pool properties
	 */
	private ConnectionPoolProperties getConnectionPoolProperties() {
		// get all connection pool properties
		ConnectionPoolProperties connectionPoolProperties = null;
		try {
			connectionPoolProperties = new ConnectionPoolProperties();
		} catch (IOException e) {
			getLogger().log(Level.ERROR, LOG_INVALID_CONNECTION_PROPERTIES, e);
		}
		return connectionPoolProperties;
	}
}