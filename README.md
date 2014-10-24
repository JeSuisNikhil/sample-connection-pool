# A Sample Connection Pool 

This is a basic connection pool implementation

##Notes
The ConnectionPool has been implemented by the ConnectionPoolImpl class.

The configuration is available to be modified in the connection-pool.properties file.

I have used log4j to log traces, infos, warnings and errors. This is also configurable using the log4j.properties.

Standard mvn compile, mvn test-compile and mvn test commands should work.

## Assumptions
There will always be a jndi name based data source associated with a connection pool (the data source is also mocked).

To release the connection a thread must call the connection.close() method. The threads should usually not have access to the ConnectionPool.releaseConnection(Connection) method. But even if they do, the releaseConnection method will internally call the connection.close() method. There is a "close" connection event that is triggered which will attempt to recycle the connection.

## Features
Actively close and reap connections that have been leased for some configurable amount of time. I have added this feature as a private TimerTask implementation inside the PooledConnectionImpl class. The method timeout() implements this logic. This is configurable through the <CONNECTION_TIME_OUT> property in the connection-pool.properties. The timer starts when the ConnectionPoolImpl.getConnection() method is called.

Periodically checking that available connections are still useable and removing those that are not. I have added this feature as a private TimerTask implementation inside the ConnectionPoolImpl class. The method maintainConnectionPool() implements this logic. The feature is configurable in the sense that you can call the method ConnectionPoolImpl.setAutoMaintain(boolean) to turn this maintenance thread on or off. You can also configure the time interval between maintenance calls throught the TIME_BETWEEN_POOL_MAINTENANCE property in the connection-pool.properties.

PooledConnectionEventListener: I have implemented a connection event listener that listens for connection events like "close", "error" and "timeout". If any of these events occur then the connection pool attempts to recycle the connection.
ConnectionState: To help with the event handling, I added a connection state the PooledConnectionImpl class. It tracks connections states like "closed", "open", "timed_out" and "error_occured".

MAX_IDLE_SIZE: The pool can produce <MAX_SIZE> number of connections. But when these connections are being released back into the pool only <MAX_IDLE_SIZE> number of connections will be accepted back into the pool. The remaining will be disposed.

MAX_SIZE: The pool can produce at most <MAX_SIZE> number of connections before asking the threads to wait for a connection.

WAIT_TIME_OUT: When threads are waiting on the connection pool to give it a connection when one becomes available, the "wait" times out and throws and SQL exception.

MIN_SIZE: The connection pool is initialized to <MIN_SIZE> number of connections. The maintainConnectionPool() method also ensures that there are always atleast <MIN_SIZE> number of connections available in the connection pool.

