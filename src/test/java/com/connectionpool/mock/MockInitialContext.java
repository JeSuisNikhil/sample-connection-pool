package com.connectionpool.mock;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;

import java.sql.Connection;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.connectionpool.sample.SampleConnectionConstants;

/**
 * This mocks a context implementation. The purpose is to allow the tests to get data sources and create connections with some basic operations.
 * 
 * @author nikhilagarwal
 */
public class MockInitialContext {

	private static Logger logger;

	private static Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger(MockInitialContext.class.getSimpleName());
		}
		return logger;
	}

	Context context;

	public MockInitialContext() {
		super();
		try {
			context = createMock(Context.class);
			DataSource dataSource = createMock(DataSource.class);
			Connection connection = createMock(Connection.class);
			expect(connection.isValid(0)).andReturn(Boolean.TRUE).anyTimes();
			expect(context.lookup(SampleConnectionConstants.SAMPLE_JNDI_NAME)).andReturn(dataSource).anyTimes();
			expect(dataSource.getConnection()).andReturn(connection).anyTimes();
			connection.close();
			expectLastCall().anyTimes();
			replay(connection);
			replay(dataSource);
			replay(context);
		} catch (Exception e) {
			getLogger().log(Level.ERROR, e.getMessage(), e);
		}
	}

	public Object lookup(String jndiName) throws NamingException {
		// i know.. i know.. somebody gonna get hurt real bad! I didn't want to spend too much time perfecting a mock data source
		return context.lookup(SampleConnectionConstants.SAMPLE_JNDI_NAME);
	}
}