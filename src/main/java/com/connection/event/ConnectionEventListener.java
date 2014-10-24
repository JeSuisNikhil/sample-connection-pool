package com.connection.event;

import java.sql.SQLException;

/**
 * This will track the connection closed, error and time out events.
 * 
 * @author nikhilagarwal
 */
public interface ConnectionEventListener {

	public void connectionClosed(ConnectionEvent event) throws SQLException;

	public void connectionErrorOccurred(ConnectionEvent event) throws SQLException;

	public void connectionTimedOut(ConnectionEvent event) throws SQLException;

}