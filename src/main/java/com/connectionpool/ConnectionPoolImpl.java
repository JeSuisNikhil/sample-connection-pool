package com.connectionpool;

import com.connection.event.ConnectionEvent;
import com.connection.event.ConnectionEventListener;
import com.connection.impl.PooledConnectionImpl;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The max number of connections available in the connection pool is configured by MAX_IDLE_SIZE. But pool can always keep produce connections until
 * the total connection count reaches MAX_SIZE. A connection pool will be initialized with MIN_SIZE number of connection.
 * 
 * @author nikhilagarwal
 */
public class ConnectionPoolImpl implements ConnectionPool {

	// loggers and messages
	private static final String LOG_MESSAGE_CONNECTION_CLOSED = "Connection closed";
	private static final String LOG_MESSAGE_CONNECTION_DISPOSED = "Connection disposed. Total Connections Active: ";
	private static final String LOG_MESSAGE_CONNECTION_ERROR_OCCURED = "Connection error occurred";
	private static final String LOG_MESSAGE_CONNECTION_LIMIT_REACHED = "Connection limit reached";
	private static final String LOG_MESSAGE_CONNECTION_POOL_MAINTENANCE_END = "Ending connection pool maintenance";
	private static final String LOG_MESSAGE_CONNECTION_POOL_MAINTENANCE_INVALID_FOUND = "Invalid connection found";
	private static final String LOG_MESSAGE_CONNECTION_POOL_MAINTENANCE_START = "Starting connection pool maintenance";
	private static final String LOG_MESSAGE_CONNECTION_RECYCLED = "Connection recycled";
	private static final String LOG_MESSAGE_CONNECTION_TAKEN = "Connection taken";
	private static final String LOG_MESSAGE_CONNECTION_TIMED_OUT = "Connection timed out";
	private static final String LOG_MESSAGE_CONNECTION_WAIT_TIMED_OUT = "Connection wait timed out";
	private static final String LOG_MESSAGE_NEW_CONNECTION_ESTABLISHED = "New connection established. Total Connections Active: ";
	private static final String LOG_MESSAGE_UNKNOWN_ERROR = "Unknown error.\n";
	private static Logger logger;
	private BlockingQueue<PooledConnectionImpl> availableConnections;
	private Long connectionTimeOut;
	private DataSource dataSource;
	private Integer maxIdle;
	private Integer maxSize;
	private Integer minSize;
	private PooledConnectionEventListener pooledConnectionEventListener;
	private Long timeBetweenPoolMaintenance;
	private TimerTask timerTask;
	private Integer totalConnectionCount;
	private Long waitTimeOut;
	/**
	 * Constructor
	 */
	ConnectionPoolImpl(ConnectionPoolBuilder builder) throws SQLException {
		super();
		this.setConnectionTimeOut(builder.getConnectionTimeOut());
		this.setDataSource(builder.getDataSource());
		this.setMaxIdle(builder.getMaxIdle());
		this.setMaxSize(builder.getMaxSize());
		this.setMinSize(builder.getMinSize());
		this.setTimeBetweenPoolMaintenance(builder.getTimeBetweenPoolMaintenance());
		this.setWaitTimeOut(builder.getWaitTimeOut());
		this.initializeConnectionPool();
	}

	private static Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger(ConnectionPoolImpl.class.getSimpleName());
		}
		return logger;
	}

	private synchronized void addConnectionToPool(PooledConnectionImpl poolconnectionImpl) {
		this.getAvailableConnections().offer(poolconnectionImpl);
		this.incTotalConnectionCount();
	}

	private void decTotalConnectionCount() {
		this.totalConnectionCount--;
	}

	/**
	 * Removes the connection from the pool and closes the connection (for real!)
	 *
	 * @param connection
	 * @throws SQLException
	 */
	void disposeConnection(PooledConnectionImpl connection) throws SQLException {
		this.removeConnectionFromPool(connection);
		connection.dispose();
	}

	public BlockingQueue<PooledConnectionImpl> getAvailableConnections() {
		return availableConnections;
	}

	private void setAvailableConnections(BlockingQueue<PooledConnectionImpl> availableConnections) {
		this.availableConnections = availableConnections;
	}

	@Override
	public synchronized PooledConnectionImpl getConnection() throws SQLException {
		PooledConnectionImpl connection = null;
		try {
			// if there are no available connections then
			if (this.getAvailableConnections().isEmpty()) {

				// check if the total number of connections floating in the system exceed the max pool size
				if (this.getTotalConnectionCount() < this.getMaxSize()) {

					// if not then add a new connection
					this.addConnectionToPool(newConnection());
					if (getLogger().isTraceEnabled()) {
						getLogger().log(Level.TRACE, LOG_MESSAGE_NEW_CONNECTION_ESTABLISHED + this.getTotalConnectionCount());
					}
				} else {
					getLogger().log(Level.WARN, LOG_MESSAGE_CONNECTION_LIMIT_REACHED);
				}
			}

			// ask for a connection and wait for the connection time out if the total connection limit is reached
			connection = this.getAvailableConnections().poll(this.getWaitTimeOut(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			getLogger().log(Level.ERROR, LOG_MESSAGE_UNKNOWN_ERROR, e);
		}

		// if queue.poll timed out then the connection will be null. Throw an exception
		if (connection == null) {
			SQLException e = new SQLException(LOG_MESSAGE_CONNECTION_WAIT_TIMED_OUT);
			getLogger().log(Level.ERROR, e.getMessage(), e);
			throw e;
		}

		// mark the connection open and set a connection time out on it
		connection.open(this.getConnectionTimeOut());
		if (getLogger().isTraceEnabled()) {
			getLogger().log(Level.TRACE, LOG_MESSAGE_CONNECTION_TAKEN);
		}
		return connection;
	}

	public Long getConnectionTimeOut() {
		return connectionTimeOut;
	}

	public void setConnectionTimeOut(Long connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}

	private DataSource getDataSource() {
		return dataSource;
	}

	private void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private Integer getMaxIdle() {
		return maxIdle;
	}

	private void setMaxIdle(Integer maxIdle) {
		this.maxIdle = maxIdle;
	}

	private Integer getMaxSize() {
		return maxSize;
	}

	private void setMaxSize(Integer maxSize) {
		this.maxSize = maxSize;
	}

	private Integer getMinSize() {
		return minSize;
	}

	private void setMinSize(Integer minSize) {
		this.minSize = minSize;
	}

	public PooledConnectionEventListener getPooledConnectionEventListener() {
		return pooledConnectionEventListener;
	}

	public void setPooledConnectionEventListener(PooledConnectionEventListener pooledConnectionEventListener) {
		this.pooledConnectionEventListener = pooledConnectionEventListener;
	}

	public Long getTimeBetweenPoolMaintenance() {
		return timeBetweenPoolMaintenance;
	}

	public void setTimeBetweenPoolMaintenance(Long timeBetweenPoolMaintenance) {
		this.timeBetweenPoolMaintenance = timeBetweenPoolMaintenance;
	}

	public TimerTask getTimerTask() {
		return timerTask;
	}

	public void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}

	public Integer getTotalConnectionCount() {
		return totalConnectionCount;
	}

	private void setTotalConnectionCount(Integer availableConnectionCount) {
		this.totalConnectionCount = availableConnectionCount;
	}

	private Long getWaitTimeOut() {
		return waitTimeOut;
	}

	private void setWaitTimeOut(Long connectionTimeOut) {
		this.waitTimeOut = connectionTimeOut;
	}

	private void incTotalConnectionCount() {
		this.totalConnectionCount++;
	}

	/**
	 * Initialize the connection pool:
	 * <ol>
	 * <li>Set the count of floating connections to 0</li>
	 * <li>Instantiate the blocking queue</li>
	 * <li>Instantiate connections</li>
	 * </ol>
	 *
	 * @throws SQLException
	 */
	private void initializeConnectionPool() throws SQLException {

		// set the available connections count to 0
		this.setTotalConnectionCount(0);

		this.setPooledConnectionEventListener(new PooledConnectionEventListener());

		// initialize the pool size to Max Idle. If there are more connections that are being released than the MAX_IDLE_SIZE the pool will dispose
		BlockingQueue<PooledConnectionImpl> availableConnections = new ArrayBlockingQueue<>(this.getMaxIdle(), Boolean.TRUE);
		this.setAvailableConnections(availableConnections);

		this.initializeConnections();
	}

	/**
	 * Find the minimum size of the connection pool and instantiate as many connections. Also increment the total connections count.
	 *
	 * @throws SQLException
	 */
	private synchronized void initializeConnections() throws SQLException {
		while (this.getAvailableConnections().size() < this.getMinSize()) {
			this.addConnectionToPool(newConnection());
		}
	}

	private synchronized void maintainConnectionPool() throws SQLException {
		for (PooledConnectionImpl temp : this.getAvailableConnections()) {
			if (!temp.isValid(0)) {
				if (getLogger().isInfoEnabled()) {
					getLogger().log(Level.INFO, LOG_MESSAGE_CONNECTION_POOL_MAINTENANCE_INVALID_FOUND);
				}
				disposeConnection(temp);
			}
		}

		// initialize the connections so that the min_size is maintained
		initializeConnections();
	}

	/**
	 * Gets a new connection from the data source and sets the pooled connection even listener to it.
	 *
	 * @return a new PooledConnectionImpl
	 * @throws SQLException
	 */
	private PooledConnectionImpl newConnection() throws SQLException {
		PooledConnectionImpl pooledConnection = new PooledConnectionImpl(this.getDataSource().getConnection());
		pooledConnection.setConnectionEventListener(this.getPooledConnectionEventListener());
		return pooledConnection;
	}

	/**
	 * Recycles the connection if the connection pool releases the connection or a consumer closes it.
	 *
	 * @param connection
	 * @throws SQLException
	 */
	private synchronized void recycleConnection(PooledConnectionImpl connection) throws SQLException {

		// if the max idle pool size is exceeded then
		if (!this.getAvailableConnections().offer(connection)) {

			// dispose of the connection (for good!)
			connection.dispose();

			// decrease the count of the total number of connections floating in the system
			decTotalConnectionCount();
			if (getLogger().isTraceEnabled()) {
				getLogger().log(Level.TRACE, LOG_MESSAGE_CONNECTION_DISPOSED + this.getTotalConnectionCount());
			}
		} else {
			if (getLogger().isTraceEnabled()) {
				getLogger().log(Level.TRACE, LOG_MESSAGE_CONNECTION_RECYCLED);
			}
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		// just releases the connection and marks it closed. The connection is actually closed when you call the dispose method
		connection.close();
	}

	/**
	 * Removes the connection from connection pool and decreases the connection count.
	 *
	 * @param
	 */
	synchronized void removeConnectionFromPool(PooledConnectionImpl connection) {
		this.getAvailableConnections().remove(connection);
		this.decTotalConnectionCount();
	}

	public void setAutoMaintain(boolean isAutoMaintain) {
		if (isAutoMaintain && this.getTimeBetweenPoolMaintenance() > 0) {
			this.setTimerTask(new PooledConnectionMaintenanceTimerTask());
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(this.getTimerTask(), this.getTimeBetweenPoolMaintenance(), this.getTimeBetweenPoolMaintenance());
		} else {
			if (this.getTimerTask() != null) {
				this.getTimerTask().cancel();
				this.setTimerTask(null);
			}
		}
	}

	/**
	 * When a connection.close method is called or if there is a timeout or an error event in the connection, the methods implemented by this class
	 * are called. I am using this primarily to actively connections that have been leased out beyond the connection time out.
	 *
	 * @author nikhilagarwal
	 */
	private class PooledConnectionEventListener implements ConnectionEventListener {

		@Override
		public void connectionClosed(ConnectionEvent event) throws SQLException {
			if (getLogger().isTraceEnabled()) {
				getLogger().log(Level.TRACE, LOG_MESSAGE_CONNECTION_CLOSED);
			}
			recycleConnection((PooledConnectionImpl) event.getConnection());
		}

		@Override
		public void connectionErrorOccurred(ConnectionEvent event) throws SQLException {
			getLogger().log(Level.WARN, LOG_MESSAGE_CONNECTION_ERROR_OCCURED);
			recycleConnection((PooledConnectionImpl) event.getConnection());
		}

		@Override
		public void connectionTimedOut(ConnectionEvent event) throws SQLException {
			getLogger().log(Level.WARN, LOG_MESSAGE_CONNECTION_TIMED_OUT);
			recycleConnection((PooledConnectionImpl) event.getConnection());
		}
	}

	/**
	 * The timer task to maintain the connection pool connections
	 *
	 * @author nikhilagarwal
	 */
	private class PooledConnectionMaintenanceTimerTask extends TimerTask {

		@Override
		public void run() {
			try {
				if (getLogger().isTraceEnabled()) {
					getLogger().log(Level.TRACE, LOG_MESSAGE_CONNECTION_POOL_MAINTENANCE_START);
				}

				maintainConnectionPool();

				if (getLogger().isTraceEnabled()) {
					getLogger().log(Level.TRACE, LOG_MESSAGE_CONNECTION_POOL_MAINTENANCE_END);
				}
			} catch (SQLException e) {
				getLogger().log(Level.ERROR, e.getMessage(), e);
			}
		}
	}
}