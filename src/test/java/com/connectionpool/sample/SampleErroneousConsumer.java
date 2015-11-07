package com.connectionpool.sample;

import com.connection.impl.PooledConnectionImpl;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * This one will make the connection inValid. This is a sample consumer thread. Used for testing only. Each instance of this thread will do the
 * following:
 * <ol>
 * <li>Get a connection</li>
 * <li>Wait for a given threadWaitTime milliseconds</li>
 * <li>Call a connection method which will result in an error</li>
 * <li>Close the connection</li>
 * </ol>
 *
 * @author nikhilagarwal
 */
public class SampleErroneousConsumer implements Runnable {

	private static final String LOG_THREAD_END = "Ending thread: ";
	private static final String LOG_THREAD_START = "Starting thread: ";
	private static Logger logger;
	private PooledConnectionImpl connection;
	private Long threadWaitTime;

	public SampleErroneousConsumer(final PooledConnectionImpl connection, final Long threadWaitTime) {
		super();
		this.connection = connection;
		this.threadWaitTime = threadWaitTime;
	}

	private static Logger getLogger() {
		if (SampleErroneousConsumer.logger == null) {
			SampleErroneousConsumer.logger = Logger.getLogger(SampleErroneousConsumer.class.getSimpleName());
		}
		return SampleErroneousConsumer.logger;
	}

	// a main method to test the Sample Consumer
	public static void main(final String... args) {
		SampleErroneousConsumer consumer;
		try {
			consumer = new SampleErroneousConsumer((PooledConnectionImpl) SampleConnectionUtil.getConnection(), 20000L);
			consumer.run();
		} catch (final SQLException e) {
			SampleErroneousConsumer.getLogger().log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Simulated connection processing. It actually just waits around for threadWaitTime. Here it also invalidates the connection simulating a bad
	 * consumer.
	 * 
	 * @throws InterruptedException
	 * @throws SQLException
	 */
	private void doSomething() throws InterruptedException, SQLException {
		Thread.sleep(this.getThreadWaitTime());
		this.getConnection().inValidate();
	}

	public PooledConnectionImpl getConnection() {
		return this.connection;
	}

	public void setConnection(final PooledConnectionImpl connection) {
		this.connection = connection;
	}

	public Long getThreadWaitTime() {
		return this.threadWaitTime;
	}

	public void setThreadWaitTime(final Long threadWaitTime) {
		this.threadWaitTime = threadWaitTime;
	}

	@Override
	public void run() {
		try {
			if (SampleErroneousConsumer.getLogger().isInfoEnabled()) {
				SampleErroneousConsumer.getLogger().log(Level.INFO, SampleErroneousConsumer.LOG_THREAD_START + this.hashCode());
			}

			this.doSomething();

			this.getConnection().close();

			if (SampleErroneousConsumer.getLogger().isInfoEnabled()) {
				SampleErroneousConsumer.getLogger().log(Level.INFO, SampleErroneousConsumer.LOG_THREAD_END + this.hashCode());
			}
		} catch (final Exception e) {
			SampleErroneousConsumer.getLogger().log(Level.ERROR, e.getMessage(), e);
		}
	}
}