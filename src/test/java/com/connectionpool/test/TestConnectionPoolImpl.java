package com.connectionpool.test;

import com.connection.impl.PooledConnectionImpl;
import com.connectionpool.ConnectionPoolImpl;
import com.connectionpool.ConnectionPoolProperties;
import com.connectionpool.sample.SampleConnectionPoolFactory;
import com.connectionpool.sample.SampleConnectionUtil;
import com.connectionpool.sample.SampleConsumer;
import com.connectionpool.sample.SampleErroneousConsumer;
import junit.framework.Assert;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit tests
 *
 * @author nikhilagarwal
 */
public class TestConnectionPoolImpl {

	private static Logger logger;

	private static Logger getLogger() {
		if (TestConnectionPoolImpl.logger == null) {
			TestConnectionPoolImpl.logger = Logger.getLogger(TestConnectionPoolImpl.class.getSimpleName());
		}
		return TestConnectionPoolImpl.logger;
	}

	@Before
	public void setUp() throws NamingException {
	}

	/**
	 * Tests the connection time out feature. Starts a thread that will take 1000 milliseconds more than the connection time out and lets the time out
	 * even be called. Test is successful if the pooledConnection is timed out.
	 */
	@Test
	public void testConnectionTimeOut() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testConnectionTimeOut");
		try {
			final String randomJndiName = "testConnectionTimeOut";

			// deliberately make the sample consumer run for 1000 milliseconds more than connection time out
			final Long threadWaitTime = new ConnectionPoolProperties().getConnectionTimeOut() + 1000L;
			final PooledConnectionImpl pooledConnection = (PooledConnectionImpl) SampleConnectionUtil.getConnection(randomJndiName);
			final SampleConsumer consumer = new SampleConsumer(pooledConnection, threadWaitTime);
			consumer.run();
			Assert.assertTrue(pooledConnection.isTimedOut());

		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testConnectionTimeOut");
	}

	/**
	 * Test that connection pool can issue one connection.
	 */
	@Test
	public void testGettingOneConnection() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testGettingOneConnection");
		try {
			final String randomJndiName = "testGettingOneConnection";
			final PooledConnectionImpl pooledConnection = (PooledConnectionImpl) SampleConnectionUtil.getConnection(randomJndiName);
			Assert.assertTrue(pooledConnection.isOpen());
			pooledConnection.close();
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testGettingOneConnection");
	}

	/**
	 * Tests the maxIdleSize connection property. Test creates a connection pool, acquires maxSize number of connections and then releases all of
	 * them. Connection pool should keep only <max idle size> number of connections and discard the rest. Test is successful if the number of
	 * connections available after all connections have been released is equal to the maxIdleSize. maxSize should be greater than maxIdleSize for an
	 * effective test.
	 */
	@Test
	public void testMaxIdleSize() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testMaxIdleSize");
		try {
			final String randomJndiName = "testMaxIdleSize";
			final List<Connection> connectionList = new ArrayList<>();
			final ConnectionPoolProperties connectionPoolProperties = new ConnectionPoolProperties();
			final int maxIdleSize = connectionPoolProperties.getMaxIdle();
			final int maxSize = connectionPoolProperties.getMaxSize();

			final ConnectionPoolImpl connectionPoolImpl =
					(ConnectionPoolImpl) SampleConnectionPoolFactory.getInstance().getConnectionPool(randomJndiName);
			for (int i = 0; i < maxSize; i++) {
				connectionList.add(connectionPoolImpl.getConnection());
			}

			for (int i = 0; i < maxSize; i++) {
				connectionList.get(i).close();
			}

			Assert.assertTrue(connectionPoolImpl.getAvailableConnections().size() == maxIdleSize);
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testMaxIdleSize");
	}

	/**
	 * Tests the maxSize connection pool property. Test attempts to get more than maxSize number of connections. Test is successful if a connection
	 * wait time out exception is thrown.
	 */
	@Test
	public void testMaxSize() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testMaxSize");
		try {
			final String randomJndiName = "testMaxSize";
			final ConnectionPoolProperties connectionPoolProperties = new ConnectionPoolProperties();
			final Integer maxSize = connectionPoolProperties.getMaxSize();
			final ConnectionPoolImpl connectionPoolImpl =
					(ConnectionPoolImpl) SampleConnectionPoolFactory.getInstance().getConnectionPool(randomJndiName);
			try {
				// get maxSize + 1 number of connections to induce a connection wait scenario
				for (int i = 0; i < (maxSize + 1); i++) {
					connectionPoolImpl.getConnection();
				}// fail test if no exception was thrown
				Assert.fail("Connection Wait Time Out Exception was not thrown");
			} catch (final SQLException e) {
				Assert.assertTrue(connectionPoolImpl.getTotalConnectionCount().equals(maxSize));
			}

		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testMaxSize");
	}

	/**
	 * Tests the minSize connection pool property feature. Test initializes a connection pool and checks the total number of connections. Test is
	 * successful if the number of connections equals minSize.
	 */
	@Test
	public void testMinSize() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testMinSize");
		try {
			final String randomJndiName = "testMinSize";
			final ConnectionPoolProperties connectionPoolProperties = new ConnectionPoolProperties();
			final Integer minSize = connectionPoolProperties.getMinSize();
			final ConnectionPoolImpl connectionPoolImpl =
					(ConnectionPoolImpl) SampleConnectionPoolFactory.getInstance().getConnectionPool(randomJndiName);

			Assert.assertTrue(minSize.equals(connectionPoolImpl.getTotalConnectionCount()));
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testMinSize");
	}

	/**
	 * Tests a simulated production scenario where the number of connections being requested exceeds the maxSize. The connection pool puts the threads
	 * requesting for connections in a queue if a connection is not available. As connections are closed and released into the pool the connection
	 * pool gives the connections to the threads in the queue. Test is successful if all threads finish execution with out connection wait time out
	 * exceptions.
	 */
	@Test
	public void testMultiConnectionPoolManagement() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testMultiConnectionPoolManagement");
		try {
			final String randomJndiName = "testMultiConnectionPoolManagement";
			final List<Thread> threads = new ArrayList<>();
			final ConnectionPoolProperties connectionPoolProperties = new ConnectionPoolProperties();
			final Integer maxSize = connectionPoolProperties.getMaxSize();
			final Long waitTimeOut = connectionPoolProperties.getWaitTimeOut();

			final ConnectionPoolImpl connectionPool =
					(ConnectionPoolImpl) SampleConnectionPoolFactory.getInstance().getConnectionPool(randomJndiName);

			for (int i = 0; i < (2 * maxSize); i++) {
				final SampleConsumer consumer = new SampleConsumer(connectionPool.getConnection(), waitTimeOut);
				final Thread thread = new Thread(consumer);
				threads.add(thread);
				thread.start();
				Thread.sleep(waitTimeOut / maxSize);
			}

			for (final Thread thread : threads) {
				if (thread.isAlive()) {
					thread.join();
				}
			}
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testMultiConnectionPoolManagement");
	}

	/**
	 * Tests that the setAutoMaintain works. To make this test fail comment the line connectionPool.setAutoMaintain(Boolean.TRUE); in this method.
	 * Test creates maxIdleSize number of connections and deliberately induces a connection error through a bad consumer (SampleErroneousConsumer).
	 * After issuing all connections the test waits until the maintenance thread runs and clears the connection that had an error. Test is successful
	 * if the total number of available connections is equal to maxIdleSize - 1.
	 */
	@Test
	public void testMultiConnectionPoolManagementWithMaintenance() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testMultiConnectionPoolManagementWithMaintenance");
		try {
			final String dataSourceJndi = "testMultiConnectionPoolManagementWithMaintenance";
			final List<Thread> threads = new ArrayList<>();
			final ConnectionPoolProperties connectionPoolProperties = new ConnectionPoolProperties();
			final Integer maxSize = connectionPoolProperties.getMaxSize();
			final Long waitTimeOut = connectionPoolProperties.getWaitTimeOut();
			final Long timeBetweenMainteanceIntervals = connectionPoolProperties.getTimeBetweenPoolMaintenance();
			final Integer maxIdleSize = connectionPoolProperties.getMaxIdle();

			final ConnectionPoolImpl connectionPool =
					(ConnectionPoolImpl) SampleConnectionPoolFactory.getInstance().getConnectionPool(dataSourceJndi);

			// this starts the maintenance thread
			connectionPool.setAutoMaintain(Boolean.TRUE);

			// this will ensure that the pool size is at it's max idle size
			for (int i = 0; i < maxSize; i++) {
				final SampleConsumer consumer = new SampleConsumer(connectionPool.getConnection(), waitTimeOut);
				final Thread thread = new Thread(consumer);
				threads.add(thread);
				thread.start();
				Thread.sleep(waitTimeOut / maxSize);
			}

			for (final Thread thread : threads) {
				if (thread.isAlive()) {
					thread.join();
				}
			}

			// this will make sure that one of the connections has become invalid
			final SampleErroneousConsumer consumer = new SampleErroneousConsumer(connectionPool.getConnection(), waitTimeOut);
			consumer.run();

			// this will ensure that if the maintenance thread has started then run() method will execute
			Thread.sleep(timeBetweenMainteanceIntervals);

			// once the maintenance thread has run the connection pool size should reduce by one. So it will be at (max_idle_size - 1)
			Assert.assertTrue(connectionPool.getAvailableConnections().size() == (maxIdleSize - 1));
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testMultiConnectionPoolManagementWithMaintenance");
	}

	/**
	 * Tests a simple connection pool/thread scenario. Test instantiates a connection pool, instantiates a thread, makes the thread request a
	 * connection and hold it until just before connection time out, releases the connection. Test is successful if there are no exceptions/errors and
	 * if the pooledConnection is closed after the thread has completed its execution.
	 */
	@Test
	public void testRunningOneConnection() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testRunningOneConnection");
		try {
			final String randomJndiName = "testRunningOneConnection";
			final Long threadWaitTime = new ConnectionPoolProperties().getConnectionTimeOut() - 1000L;
			final PooledConnectionImpl pooledConnection = (PooledConnectionImpl) SampleConnectionUtil.getConnection(randomJndiName);
			final SampleConsumer consumer = new SampleConsumer(pooledConnection, threadWaitTime);
			consumer.run();
			Assert.assertTrue(pooledConnection.isClosed());
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
		}
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testRunningOneConnection");
	}

	/**
	 * Tests the connection wait time out feature. Test deliberately creates threads that asks for more than maxSize number of connections. Each
	 * thread holds the connection for 2000 milliseconds more than connection wait time. Test is successful if a connection wait time out exception is
	 * thrown.
	 */
	@Test
	public void testWaitTimeOut() {
		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Starting testWaitTimeOut");
		try {
			final String randomJndiName = "testWaitTimeOut";
			final Long threadWaitTime = new ConnectionPoolProperties().getWaitTimeOut() + 2000L;
			final ConnectionPoolProperties connectionPoolProperties = new ConnectionPoolProperties();
			final int maxSize = connectionPoolProperties.getMaxSize();
			for (int i = 0; i < maxSize; i++) {
				final SampleConsumer consumer = new SampleConsumer(SampleConnectionUtil.getConnection(randomJndiName), threadWaitTime);
				new Thread(consumer).start();
			}

			final SampleConsumer consumer = new SampleConsumer(SampleConnectionUtil.getConnection(randomJndiName), threadWaitTime);
			consumer.run();

			// test has failed if no exceptions were thrown
			Assert.fail("Connection wait time out exception now thrown");
		} catch (final SQLException e) {
			Assert.assertTrue(e.getMessage().equals("Connection wait timed out"));
		} catch (final Exception e) {
			TestConnectionPoolImpl.getLogger().log(Level.ERROR, e.getMessage(), e);
			Assert.fail();
		}

		TestConnectionPoolImpl.getLogger().log(Level.INFO, "Ending testWaitTimeOut");
	}
}