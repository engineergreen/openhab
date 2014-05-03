package org.openhab.binding.cm11a.internal.modules;

import java.io.IOException;

import org.openhab.binding.cm11a.internal.InvalidAddressException;
import org.openhab.binding.cm11a.internal.X10Interface;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplianceModule extends AbstractX10Module  {

	public ApplianceModule(String address) {
		super(address);
	}

	Boolean currentState = null;
	Boolean desiredState = new Boolean(false);  // Arbitrary default starting state

	private static final Logger log = LoggerFactory.getLogger(ApplianceModule.class);


	@Override
	public void updateHardware(X10Interface x10Interface) throws IOException, InvalidAddressException {
		log.debug("Updating Appliance module: " + address + " State (current/desired): " + currentState + "/" + desiredState);
		if (!desiredState.equals(currentState)){
			if (desiredState) {
				x10Interface.sendFunction(address, X10Interface.FUNC_ON);
				currentState = true;
			} else {
				x10Interface.sendFunction(address, X10Interface.FUNC_OFF);
				currentState = false;
			}
		} else {
			log.trace("No need to update device: " + address + " as hardware already matches desired state");
		}
	}




	@Override
	public void processCommand(Command command) {
		if (OnOffType.ON.equals(command)){
			desiredState = Boolean.TRUE;
		} else if (OnOffType.OFF.equals(command)){
			desiredState = Boolean.FALSE;
		} else {
			log.error("Ignoring unknown command received for device: " + address);
		}

	}



}
