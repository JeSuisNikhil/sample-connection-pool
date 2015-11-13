package com.cornholio.database.connection.event;

import java.sql.SQLException;

/**
 * This will track the connection closed, error and time out events.
 * 
 * @author nikhilagarwal
 */
public interface ConnectionEventListener {

	void connectionClosed(ConnectionEvent event) throws SQLException;

	void connectionErrorOccurred(ConnectionEvent event) throws SQLException;

	void connectionTimedOut(ConnectionEvent event) throws SQLException;

}