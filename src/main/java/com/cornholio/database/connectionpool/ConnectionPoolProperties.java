package com.cornholio.database.connectionpool;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The connection pool properties that are needed to initialize the connection pool.
 */
public class ConnectionPoolProperties {

	private static final String CONNECTION_POOL_PROPERTY_CONNECTION_TIME_OUT = "CONNECTION_TIME_OUT";
	private static final String CONNECTION_POOL_PROPERTY_MAX_IDLE_SIZE = "MAX_IDLE_SIZE";
	private static final String CONNECTION_POOL_PROPERTY_MAX_SIZE = "MAX_SIZE";
	private static final String CONNECTION_POOL_PROPERTY_MIN_SIZE = "MIN_SIZE";
	private static final String CONNECTION_POOL_PROPERTY_TIME_BETWEEN_POOL_MAINTENANCE = "TIME_BETWEEN_POOL_MAINTENANCE";
	private static final String CONNECTION_POOL_PROPERTY_WAIT_TIME_OUT = "WAIT_TIME_OUT";
	private static final String CONNECTION_PROPERTIES_FILE_LOCATION = "./connection-pool.properties";

	private Properties properties;

	public ConnectionPoolProperties() throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream(this.getClass().getClassLoader().getResource(CONNECTION_PROPERTIES_FILE_LOCATION).getPath()));
	}

	public ConnectionPoolProperties(String fileLocation) throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream(this.getClass().getClassLoader().getResource(fileLocation).getPath()));
	}

	public Long getConnectionTimeOut() {
		return Long.valueOf(this.getProperties().getProperty(CONNECTION_POOL_PROPERTY_CONNECTION_TIME_OUT));
	}

	public Integer getMaxIdle() {
		return Integer.valueOf(this.getProperties().getProperty(CONNECTION_POOL_PROPERTY_MAX_IDLE_SIZE));
	}

	public Integer getMaxSize() {
		return Integer.valueOf(this.getProperties().getProperty(CONNECTION_POOL_PROPERTY_MAX_SIZE));
	}

	public Integer getMinSize() {
		return Integer.valueOf(this.getProperties().getProperty(CONNECTION_POOL_PROPERTY_MIN_SIZE));
	}

	public Properties getProperties() {
		return properties;
	}

	public Long getTimeBetweenPoolMaintenance() {
		return Long.valueOf(this.getProperties().getProperty(CONNECTION_POOL_PROPERTY_TIME_BETWEEN_POOL_MAINTENANCE));
	}

	public Long getWaitTimeOut() {
		return Long.valueOf(this.getProperties().getProperty(CONNECTION_POOL_PROPERTY_WAIT_TIME_OUT));
	}
}