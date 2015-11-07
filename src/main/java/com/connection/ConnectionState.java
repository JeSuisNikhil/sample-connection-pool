package com.connection;

/**
 * An enumeration to keep track of the connection state.
 * 
 * @author nikhilagarwal
 */
public enum ConnectionState {
	CLOSED(1), DISPOSED(4), ERROR_OCCURED(3), OPEN(0), TIMED_OUT(2);

	private Integer state;

	ConnectionState(Integer state) {
		this.state = state;
	}

	public Integer intValue() {
		return this.state;
	}
}