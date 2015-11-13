package com.cornholio.database.connectionpool.mock;

import com.cornholio.database.connectionpool.sample.SampleConnectionConstants;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;

import static org.easymock.EasyMock.*;

/**
 * This mocks a context implementation. The purpose is to allow the tests to get data sources and create connections with some basic operations.
 * 
 * @author nikhilagarwal
 */
public class MockInitialContext {

	private static Logger logger;
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

	private static Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger(MockInitialContext.class.getSimpleName());
		}
		return logger;
	}

	public Object lookup(String jndiName) throws NamingException {
		// i know.. i know.. somebody gonna get hurt real bad! I didn't want to spend too much time perfecting a mock data source
		return context.lookup(SampleConnectionConstants.SAMPLE_JNDI_NAME);
	}
}