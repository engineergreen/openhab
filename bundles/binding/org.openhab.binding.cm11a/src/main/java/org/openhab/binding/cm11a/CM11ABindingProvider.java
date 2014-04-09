/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cm11a;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

/**
 * @author Anthony Green
 * @since 1.5.0-SNAPSHOT
 */
public interface CM11ABindingProvider extends BindingProvider {
	/**
	 * Get the X10 Device code for the given item
	 * @param item
	 * @return
	 */
	public String getDeviceCode(String itemName);
}
