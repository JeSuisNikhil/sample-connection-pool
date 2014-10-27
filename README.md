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

## Folder Structure
```
├── README.md
├── pom.xml
└── src
    ├── main
    │   └── java
    │       └── com
    │           ├── connection
    │           │   ├── ConnectionState.java (Enumeration. E.g. closed, open, timed_out etc.)
    │           │   ├── event
    │           │   │   ├── ConnectionEvent.java
    │           │   │   └── ConnectionEventListener.java  (Listens for connection events like close, error, timeout
    │           │   │       etc. The connection pool holds a reference to this. The actions for these events are
    │           │   │       implemented by the pool)
    │           │   └── impl
    │           │       ├── AbstractConnectionDecorator.java
    │           │       └── PooledConnectionImpl.java (A decorated connection with additional features such as a
    │           │           timeout timer task, synchronized methods for setting connection state et al)    
    │           └── connectionpool
    │               ├── ConnectionPool.java (Interface with two methods)
    │               ├── ConnectionPoolBuilder.java (Loads properties and builds a connection pool)
    │               ├── ConnectionPoolImpl.java (Implements a connection pool with a event listener, maintenance
    │                   timer task, synchronized methods to release and get connections)
    │               └── ConnectionPoolProperties.java (Loader class for properties)
    └── test
        ├── java
        │   └── com
        │       └── connectionpool
        │           ├── mock
        │           │   └── MockInitialContext.java (Mocking the initial context to simulate a app server's ability
        │           │       to give us a data source and perform some basic sql operations on it)
        │           ├── sample
        │           │   ├── SampleConnectionConstants.java
        │           │   ├── SampleConnectionPoolFactory.java (Singleton. could be in the main implementation if I
        │           │       didn't need touse a mock initial context)
        │           │   ├── SampleConnectionUtil.java
        │           │   ├── SampleConsumer.java (A consumer requests connections and pretends to do something with
        │           │       it)
        │           │   └── SampleErroneousConsumer.java  (A consumer requests connections and pretends to do
        │           │       something with it but messes things up instead and causes a sql error)
        │           └── test
        │               └── TestConnectionPoolImpl.java (9 tests that test various success/error scenarios)
        └── resources
            ├── connection-pool.properties
            └── log4j.properties
```
## Things to do
1. Remove synchronized keyword from the implementation. Use locks and conditions.
2. Make the connection factory independent of a concrete initial context implementation and move the factory to main source folders
3. Make the tests more comprehensive by actually simulating database operations (maybe using a mock database)
4. Add additional configurable properties such as unused timeout, age timeout, purge policy
5. Additional features to make this scalable from a distributed computing perspective

## Disclaimer:
1. I am not responsible for anything you do with this code. I don't care who, what, why, when, where or how.
2. If this does help, mention this repo! I don't expect you to, but it'll be nice if you do :)

## Have fun! :)
