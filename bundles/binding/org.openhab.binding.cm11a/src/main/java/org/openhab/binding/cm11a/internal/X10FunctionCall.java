package org.openhab.binding.cm11a.internal;

/**
 * X10 Function to be called
 * @author anthony
 *
 */
public class X10FunctionCall {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + command;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		X10FunctionCall other = (X10FunctionCall) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (command != other.command)
			return false;
		return true;
	}

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
