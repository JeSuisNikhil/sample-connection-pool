package com.cornholio.database.connectionpool;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * This is the connection pool builder. The purpose is to build a connection pool instance using configuration properties.
 * 
 * @author nikhilagarwal
 */
public class ConnectionPoolBuilder {

	private Long connectionTimeOut;
	private DataSource dataSource;
	private Integer maxIdle;
	private Integer maxSize;
	private Integer minSize;
	private Long timeBetweenPoolMaintenance;

	private Long waitTimeOut;

	public ConnectionPoolBuilder() {
		super();
	}

	public ConnectionPool build() throws SQLException {
		return new ConnectionPoolImpl(this);
	}

	public ConnectionPoolBuilder connectionTimeOut(Long connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
		return this;
	}

	public ConnectionPoolBuilder dataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}

	public Long getConnectionTimeOut() {
		return connectionTimeOut;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public Integer getMaxIdle() {
		return maxIdle;
	}

	public Integer getMaxSize() {
		return maxSize;
	}

	public Integer getMinSize() {
		return minSize;
	}

	public Long getTimeBetweenPoolMaintenance() {
		return timeBetweenPoolMaintenance;
	}

	public Long getWaitTimeOut() {
		return waitTimeOut;
	}

	public ConnectionPoolBuilder maxIdle(Integer maxIdle) {
		this.maxIdle = maxIdle;
		return this;
	}

	public ConnectionPoolBuilder maxSize(Integer maxSize) {
		this.maxSize = maxSize;
		return this;
	}

	public ConnectionPoolBuilder minSize(Integer minSize) {
		this.minSize = minSize;
		return this;
	}

	public ConnectionPoolBuilder timeBetweenPoolMaintenance(Long timeBetweenPoolMaintenance) {
		this.timeBetweenPoolMaintenance = timeBetweenPoolMaintenance;
		return this;
	}

	public ConnectionPoolBuilder waitTimeOut(Long connectionTimeOut) {
		this.waitTimeOut = connectionTimeOut;
		return this;
	}
}