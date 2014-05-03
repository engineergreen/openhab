/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cm11a.internal;

import org.openhab.binding.cm11a.CM11ABindingProvider;
import org.openhab.binding.cm11a.internal.modules.AbstractX10Module;
import org.openhab.binding.cm11a.internal.modules.ApplianceModule;
import org.openhab.binding.cm11a.internal.modules.LampModule;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Anthony Green
 * @since 1.5.0-SNAPSHOT
 */
public class CM11AGenericBindingProvider extends AbstractGenericBindingProvider implements CM11ABindingProvider {

	private static final Logger logger = 
			LoggerFactory.getLogger(CM11AGenericBindingProvider.class);
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "cm11a";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		AbstractX10Module module;
		
		if (X10Interface.validateAddress(bindingConfig)){
			if (item instanceof DimmerItem) {
				module = new LampModule(bindingConfig);
			} else {
				module = new ApplianceModule(bindingConfig); 
			}
			addBindingConfig(item, module);
			logger.debug("Succesfully added item: " + item.getName() + " for X10 module: " + module.getAddress());
		} else {
			throw new BindingConfigParseException("Invalid X10 Device code for item: " + item.getName() + " in " + context);
		}		
	}
	
	


	
	@Override
	public AbstractX10Module getModule(String itemName) {
		return ((AbstractX10Module) bindingConfigs.get(itemName));
	}
	
	
}
