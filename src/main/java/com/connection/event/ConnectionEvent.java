package com.connection.event;

import java.sql.Connection;

/**
 * An connection event class.
 * 
 * @author nikhilagarwal
 */
public class ConnectionEvent {

	private Connection connection;

	public ConnectionEvent(Connection connection) {
		super();
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}