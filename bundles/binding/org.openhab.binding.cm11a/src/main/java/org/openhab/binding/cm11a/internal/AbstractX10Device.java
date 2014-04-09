package org.openhab.binding.cm11a.internal;

import java.io.IOException;

/**
 * Base class for X10 device drivers.
 * 
 *  <p>Drivers for X10 devices are a little different to conventional choco devies as they must hook into 
 *  the general X10 interface class.  Also, communication with X10 devices is handled entirely asynchronously.  
 *  The slow speed of X10 means that the hardware for these devices will often not keep up with all of the updates.
 *  The master X10 interface class (X10Interface) schedules the updating of the hardware with the current value.
 *  </p>  
 * @author anthony
 *
 */
public abstract class AbstractX10Device {
	
	protected X10Interface x10Interface;
	protected String address;
	
	


	/**
	 * Will be called by the X10Interface when it is ready for this X10 device to use the X10 bus.  
	 * Child classes should override this method with the specific process necessary to update the 
	 * hardware with the latest data. 
	 * 
	 * <p>Warning: This will be called in a different thread.  It must be thread safe.</p> 
	 * 
	 * <p>Retries in the event of interface problems will be handled by the X10Interface.  If a comms 
	 * problem occurs and the method throws an exception, this device will be rescheduled again later.</p>
	 */
	abstract public void updateHardware() throws IOException;
	
	
	public X10Interface getX10Interface() {
		return x10Interface;
	}


	public void setX10Interface(X10Interface x10Interface) {
		this.x10Interface = x10Interface;
	}


	public String getAddress() {
		return address;
	}


	public void setAddress(String deviceAddress) {
		this.address = deviceAddress;
	}
	
	
}
