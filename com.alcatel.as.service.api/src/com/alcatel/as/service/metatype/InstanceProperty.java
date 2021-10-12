// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metatype;

import java.util.Map;
import java.util.Collection;

/**
 * interface to access one property instantiated for a component and its values
 * for each instance of a component
 */
public interface InstanceProperty {

	/**
	 * Generic accessor for meta information on this property descriptor
	 * 
	 * @param key The key.
	 * @return The attribute.
	 */
	String getAttribute(String key);

	/**
	 * Generic accessor for meta information on this property descriptor.
	 * 
	 * @return The attributes.
	 */
	Map<String, Object> getAttributes();

	/**
	 * Gets the PID this property descriptor belongs to.
	 * 
	 * @return The PID.
	String getPid();
	 */

	/**
	 * Gets the name of this property.
	 * 
	 * @return The name.
	 */
	String getName();

	/**
	 * Adds a new component instance.
	 * 
	 * @param instance The component to be added.
	 */
	void addInstance(String instance);

	/**
	 * Removes a component instance.
	 * 
	 * @param instance The component to be added.
	 */
	void removeInstance(String instance);

	/**
	 * List instances of this component, with overriden values.
	 */
	Map<String, String> instanceValues();

	/**
	 * Sets the value for this property.
	 * 
	 * @param instance the instance name, null means all instances that do not
	 *          override the common value.
	 * @param value The new value.
	 */
	void setValue(String instance, String value);

	/**
	 * Gets the value for this property.
	 * 
	 * @param instance The instance name, null means all instances that do not
	 *          override the common value.
	 * @return The value.
	 */
	String getValue(String instance);

	/**
	 * Gets the value for one key of a filedata property in java.util.Properties format.
	 * 
	 * @param instance The instance name, null means all instances that do not
	 *          override the common value.
	 * @param key the key within the Properties data.
	 * @return The value within the Properties data.
	 */
        String getPropertiesValue(String instance, String key) throws Exception;

	/**
	 * Sets the value for one key of a filedata property in java.util.Properties format.
	 * 
	 * @param instance The instance name, null means all instances that do not
	 *          override the common value.
	 * @param key the key within the Properties data.
	 * @param value the value to set in the Properties data.
	 * @return The previous value for this key within the Properties data.
	 */
        String setPropertiesValue(String instance, String key, String value) throws Exception;

	/**
	 * Sets the value for this property back to its default value (Factory
	 * settings).
	 * 
	 * @param instance the instance name. null means all instances that do not
	 *          override the common value
	 */
	void resetDefaultValue(String instance);

	/* dispatch value up to the desired level
	void updateComponent();
	void updateGroup();
	void updatePlatform();
	// old style "doneWithProperties".. 
	void commitProperties();*/
}
