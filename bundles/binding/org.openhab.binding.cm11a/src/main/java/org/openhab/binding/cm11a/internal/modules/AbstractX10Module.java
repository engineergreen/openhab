package org.openhab.binding.cm11a.internal.modules;

import java.io.IOException;

import org.openhab.binding.cm11a.internal.InvalidAddressException;
import org.openhab.binding.cm11a.internal.X10Interface;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.types.Command;

/**
 * Base class for X10 device drivers.
 * 
 *  <p>Communication with X10 devices is handled entirely asynchronously.  
 *  The slow speed of X10 means that the hardware for these devices will often not keep up with all of the updates.
 *  So driver classes for each type of device may need to do de-duplication.
 *  The master X10 interface class (X10Interface) schedules the updating of the hardware with the current value.
 *  </p>  
 * @author anthony
 *
 */
public abstract class AbstractX10Module implements BindingConfig {
	
	public AbstractX10Module(String address) {
		super();
		this.address = address;
	}

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
	abstract public void updateHardware(X10Interface x10Interface) throws IOException,InvalidAddressException;
	
	/**
	 * Process an Openhab command.
	 * 
	 * <p>This will be called when this binding received an openhab command for this device.  
	 * The child classes should update their local state, so that later when the updateHardware() method is called 
	 * by the x10Interface thread, it is able to then update the real hardware to match this state.<p>
	 * 
	 * @param command OpenHAB command
	 */
	abstract public void processCommand (Command command);

	public String getAddress() {
		return address;
	}


	public void setAddress(String deviceAddress) {
		this.address = deviceAddress;
	}
	
	
}
