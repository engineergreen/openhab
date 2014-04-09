package org.openhab.binding.cm11a.internal;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AM12U extends AbstractX10Device  {

	private static final Logger log = LoggerFactory.getLogger(AM12U.class);

	Boolean outputNode;
	
	@Override
	public void updateHardware() throws IOException {
	/*	try {
			if (outputNode.booleanValue()){
				x10Interface.sendFunction(address, X10Interface.FUNC_ON);
			} else {
				x10Interface.sendFunction(address, X10Interface.FUNC_OFF);	
			}
		} catch (InvalidAddressException e){
			log.error("Invalid X10 address: " + address);
		}*/
	}

	
	
	public void update(Observable node, Object src) {
		/*x10Interface.scheduleHWUpdate(this);*/
	}
	
	

}
