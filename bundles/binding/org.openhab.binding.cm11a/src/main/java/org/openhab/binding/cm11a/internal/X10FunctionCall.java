package org.openhab.binding.cm11a.internal;

/**
 * X10 Function to be called
 * @author anthony
 *
 */
public class X10FunctionCall {
	public X10FunctionCall(String address, int command, int dims) {
		super();
		this.address = address;
		this.command = command;
		this.dims = dims;
	}

	/**
	 * Address of device to action call on
	 */
	public String address;
	
	/**
	 * Command to be executed
	 */
	public int command;
	
	/**
	 * DIMS/BRIGHT amount.  Zero if not applicable.
	 */
	public int dims = 0;
}
