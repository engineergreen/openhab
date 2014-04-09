package org.openhab.binding.cm11a.internal;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class LM12U extends AbstractX10Device {

	private static final Logger log = LoggerFactory.getLogger(LM12U.class);
	
	private static int DIM_LEVELS = 22;
	
	/**
	 * Current brightness level of lamp. 0-22.  Null indicates unknown.
	 */
	private Integer dimLevel;
	
	Integer outputNode;

	@Override
	public void updateHardware() throws IOException {
	/*	try {
			// Make sure new dim level is in range.
			int newLevel = outputNode;
			newLevel = Math.max(0, newLevel);
			newLevel = Math.min(DIM_LEVELS, newLevel);
			
			// If we don't know what level the lamp is already at, we must dim it to zero,
			// first to make sure we have a known starting point.
			if (dimLevel == null) {
				x10Interface.sendFunction(this.address, X10Interface.FUNC_DIM, 22);
				dimLevel = 0;
			}
			
			int levelChange = newLevel - dimLevel;
			
			// If going to maximum or minimum, levels, send maximum dims/brights to reset state
			if (newLevel >= DIM_LEVELS) {
				levelChange = DIM_LEVELS;
			} else if (newLevel <= 0) {
				levelChange = 0-DIM_LEVELS;
			}
			
			if (levelChange > 0) {
				log.trace("Changing lamp " + this.address + " level by: " + levelChange);
				x10Interface.sendFunction(this.address, X10Interface.FUNC_BRIGHT, levelChange);
			} else if (levelChange < 0) {
				x10Interface.sendFunction(this.address, X10Interface.FUNC_DIM, Math.abs(levelChange));
			}
			
			
			dimLevel = newLevel;
			
		} catch (InvalidAddressException e){
			log.error("Invalid X10 address: " + address);
		}*/
	}

	
	public void update(Observable node, Object src) {
		//x10Interface.scheduleHWUpdate(this);
	}
	
	

}
