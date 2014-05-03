package org.openhab.binding.cm11a.internal.modules;

import java.io.IOException;

import org.openhab.binding.cm11a.internal.InvalidAddressException;
import org.openhab.binding.cm11a.internal.X10Interface;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class LampModule extends AbstractX10Module {

	public LampModule(String address) {
		super(address);
	}


	private static final Logger log = LoggerFactory.getLogger(LampModule.class);

	private static int DIM_LEVELS = 22;

	/**
	 * Current brightness level of lamp. 0-22.  Null indicates unknown.
	 */
	protected Integer currentLevel;
	protected Integer desiredLevel = 22; // Arbitrary start value.  Should be overwritten on first update.
	protected Boolean currentState;

	Integer outputNode;

	@Override
	public void updateHardware(X10Interface x10Interface) throws IOException, InvalidAddressException {	
		int targetLevel = desiredLevel;
		log.debug("Updating Lamp module: " + address + " State (current): " + currentState + "; Level (desired/current): " + targetLevel + "/" + currentLevel);


		if (!Boolean.valueOf(targetLevel > 0).equals(currentState)) {

			if (targetLevel > 0) {
				log.trace("Current state doesn't match desired state.  Turning device " + address +" ON");
				x10Interface.sendFunction(this.address, X10Interface.FUNC_ON);
				currentState = true;
				currentLevel = DIM_LEVELS; // THese go to full brightness when first turned on
			} else {
				log.trace("Current state doesn't match desired state.  Turning device " + address +" OFF");
				x10Interface.sendFunction(this.address, X10Interface.FUNC_OFF);
				currentState = false;
			}
		}

		if (targetLevel > 0) {
			// If we don't know what level the lamp is already at, we must dim it to zero,
			// first to make sure we have a known starting point.
			if (currentLevel == null) {
				log.trace("Existing level for device " + address +" is unknown.  Dimming to known point (zero)");
				x10Interface.sendFunction(this.address, X10Interface.FUNC_DIM, 22);
				currentLevel = 0;
			}

			int levelChange = targetLevel - currentLevel;

			/* If going to maximum or minimum, levels, send the maximum number of dims/brights commands.
			 * This is because the
			 */
			if (desiredLevel >= DIM_LEVELS) {
				levelChange = DIM_LEVELS;
			} else if (desiredLevel <= 0) {
				levelChange = 0-DIM_LEVELS;
			}

			log.trace("Changing lamp " + this.address + " level by: " + levelChange);
			if (levelChange > 0) {
				x10Interface.sendFunction(this.address, X10Interface.FUNC_BRIGHT, levelChange);
			} else if (levelChange < 0) {
				x10Interface.sendFunction(this.address, X10Interface.FUNC_DIM, Math.abs(levelChange));
			}
		}
		currentLevel = targetLevel;

	}



	@Override
	public void processCommand(Command command) {
		if (OnOffType.ON.equals(command)){
			desiredLevel = DIM_LEVELS;

		} else if (OnOffType.OFF.equals(command)){
			desiredLevel = 0;

		} else if (command instanceof PercentType){
			PercentType perc = (PercentType) command;
			desiredLevel = Math.round((perc.floatValue() / PercentType.HUNDRED.floatValue()) * DIM_LEVELS);


		} else if (IncreaseDecreaseType.INCREASE.equals(command)){
			desiredLevel = Math.min(desiredLevel + 1,DIM_LEVELS);
		} else if (IncreaseDecreaseType.DECREASE.equals(command)){
			desiredLevel = Math.max(desiredLevel - 1,0);
		} else {
			log.error("Ignoring unknown command received for device: " + address);
		}

	}
}
