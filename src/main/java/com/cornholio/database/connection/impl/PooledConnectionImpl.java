package com.cornholio.database.connection.impl;

import com.cornholio.database.connection.ConnectionState;
import com.cornholio.database.connection.event.ConnectionEvent;
import com.cornholio.database.connection.event.ConnectionEventListener;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A wrapper around connection/connection decorator. This connection implementation has overridden methods to keep track of the connection state and
 * connection events. The connection state is maintained so that connections may be recycled. Connection events like timeouts and connection close are
 * handled.
 * 
 * @author nikhilagarwal
 */
public class PooledConnectionImpl extends AbstractConnectionDecorator {
	private static final String LOG_MESSAGE_CONNECTION_INVALIDATED = "Connection invalidated";
	private static Logger logger;
	private ConnectionEventListener connectionEventListener;
	private ConnectionState connectionState;
	private TimerTask timerTask;
	public PooledConnectionImpl(Connection connection) {
		this.setConnection(connection);
		this.setConnectionState(ConnectionState.OPEN);
		this.setTimerTask(new PooledConnectionTimerTask());
	}

	private static Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger(PooledConnectionImpl.class.getSimpleName());
		}
		return logger;
	}

	/**
	 * Doesn't really close the connection. Just mark's it as closed so that it can be recycled. Only the connection pool can close the connection.
	 * The dispose method is the one you're looking for.
	 */
	@Override
	public synchronized void close() throws SQLException {

		// don't close the connection. just mark the state as closed and put it in the pool
		if (this.isOpen()) {
			this.setConnectionState(ConnectionState.CLOSED);
			ConnectionEvent event = new ConnectionEvent(this);
			this.getConnectionEventListener().connectionClosed(event);
		}
	}

	/**
	 * Actually close the connection. Mark it as disposed so that this is not recycled.
	 *
	 * @throws SQLException
	 */
	public synchronized void dispose() throws SQLException {
		if (!this.isOpen()) {
			this.setConnectionState(ConnectionState.DISPOSED);
			if (this.getTimerTask() != null) {
				this.getTimerTask().cancel();
			}
			this.getConnection().close();
			this.setConnection(null);
		}
	}

	public ConnectionEventListener getConnectionEventListener() {
		return connectionEventListener;
	}

	public void setConnectionEventListener(ConnectionEventListener connectionEventListener) {
		this.connectionEventListener = connectionEventListener;
	}

	private ConnectionState getConnectionState() {
		return connectionState;
	}

	private void setConnectionState(ConnectionState connectionState) {
		this.connectionState = connectionState;
	}

	private TimerTask getTimerTask() {
		return timerTask;
	}

	private void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}

	/**
	 * Invalidates the connection
	 *
	 * @throws SQLException
	 */
	public synchronized void inValidate() throws SQLException {
		if (!ConnectionState.ERROR_OCCURED.equals(this.getConnectionState())) {
			getLogger().log(Level.WARN, LOG_MESSAGE_CONNECTION_INVALIDATED);
			this.setConnectionState(ConnectionState.ERROR_OCCURED);
			ConnectionEvent event = new ConnectionEvent(this);
			this.getConnectionEventListener().connectionErrorOccurred(event);
		}
	}

	@Override
	public synchronized boolean isClosed() {
		return ConnectionState.CLOSED.equals(this.getConnectionState());
	}

	public synchronized boolean isDisposed() {
		return ConnectionState.DISPOSED.equals(this.getConnectionState());
	}

	public synchronized boolean isOpen() {
		return ConnectionState.OPEN.equals(this.getConnectionState());
	}

	public synchronized boolean isTimedOut() {
		return ConnectionState.TIMED_OUT.equals(this.getConnectionState());
	}

	@Override
	public synchronized boolean isValid(int timeout) throws SQLException {
		return !ConnectionState.ERROR_OCCURED.equals(this.getConnectionState());
	}

	/**
	 * Mark the connection as opened. Should use the overloaded open method instead with the timeout delay
	 *
	 * @throws SQLException
	 */
	public synchronized void open() throws SQLException {
		if (!this.isOpen()) {
			this.setConnectionState(ConnectionState.OPEN);
		}
	}

	/**
	 * Mark the connection as open and set a timeout on it. That timeout will be used to close the connection is it has been in use by a thread for
	 * too long.
	 *
	 * @param delay
	 * @throws SQLException
	 */
	public synchronized void open(Long delay) throws SQLException {
		if (!this.isOpen()) {
			this.setConnectionState(ConnectionState.OPEN);
		}
		this.setTimerTask(new PooledConnectionTimerTask());
		this.startTimer(delay);

	}

	/**
	 * Start the connection time out timer.
	 *
	 * @param delay
	 */
	private void startTimer(Long delay) {
		if (this.getTimerTask() != null) {
			Timer timer = new Timer(true);
			timer.schedule(this.getTimerTask(), delay);
		}
	}

	/**
	 * Marks the connection as timed out. The pooled connection even listener (@see
	 * com.connectionpool.ConnectionPoolImpl.PooledConnectionEventListener) is the one that handles the timeout.
	 *
	 * @throws SQLException
	 */
	private synchronized void timeout() throws SQLException {
		if (this.isOpen()) {
			this.setConnectionState(ConnectionState.TIMED_OUT);
			ConnectionEvent event = new ConnectionEvent(this);
			this.getConnectionEventListener().connectionTimedOut(event);
		}
	}

	/**
	 * This is the connection time out timer task. When a connection is opened, this thread is scheduled to run after a delay of <CONNECTION_TIME_OUT>
	 * milliseconds. It will check if the connection is open and if so will mark the connection as timed out.
	 *
	 * @author nikhilagarwal
	 */
	private class PooledConnectionTimerTask extends TimerTask {

		public void run() {
			try {
				timeout();
			} catch (SQLException e) {
				getLogger().log(Level.ERROR, e.getMessage(), e);
			}
		}
	}
}