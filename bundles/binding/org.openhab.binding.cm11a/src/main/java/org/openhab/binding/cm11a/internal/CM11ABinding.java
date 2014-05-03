/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cm11a.internal;

import gnu.io.NoSuchPortException;

import java.util.Dictionary;

import org.openhab.binding.cm11a.CM11ABindingProvider;
import org.openhab.binding.cm11a.internal.modules.AbstractX10Module;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author Anthony Green
 * @since 1.5.0-SNAPSHOT
 */
public class CM11ABinding extends AbstractBinding<CM11ABindingProvider> implements ManagedService {

	private static final Logger logger = 
			LoggerFactory.getLogger(CM11ABinding.class);

	protected X10Interface x10iface  = null;
	protected String serialPort = null;

	public CM11ABinding() {
	}


	public void activate() {
		if (serialPort != null) {
			try {
				initialise();
			} catch (ConfigurationException e) {
				//Nothing to do, error logged elsewhere.
			}
		}
		logger.debug("CM11A Binding has been activated");
	}

	public void deactivate() {
		if(x10iface !=null){
			x10iface.close();
			x10iface = null;
		}
		logger.debug("CM11A Binding has been deactivated");
	}

	protected void initialise () throws ConfigurationException {
		if (x10iface != null) {
			deactivate();
		}
		try {
			x10iface = new X10Interface(serialPort);
			x10iface.setDaemon(true);
			x10iface.start();
			logger.info("Initialised CM11A X10 interface on: " + serialPort);
		} catch (NoSuchPortException e) {
			x10iface = null;
			logger.error("CM11A Connection failed: No such serial port: " + serialPort);
			throw new ConfigurationException("port", "No such serial port");
		}

	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
		
		for (CM11ABindingProvider provider : this.providers) {
			if (provider.providesBindingFor(itemName)) {
				AbstractX10Module deviceForCommand = provider.getModule(itemName);
				deviceForCommand.processCommand(command);
				x10iface.scheduleHWUpdate(deviceForCommand);
			}
		}
		
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		if (config != null) {
			if (!config.get("serialPort").equals(serialPort)) {
				logger.debug("New configuration string received: " + config.get("serialPort"));
				deactivate();
				serialPort = (String) config.get("serialPort");
				if (serialPort != null) {
					initialise();
				} else {
					throw new ConfigurationException("port", "No serial port configuration defined");
				}
			}			
		}
	}


}
