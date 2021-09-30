package com.alcatel.as.service.metatype;

import java.util.Map;

/**
 * Interface to access all property descriptors under one PID for one bundle
 */
public interface PropertiesDescriptor 
{

	/**
	 * Gets a map of property name to property descriptor.
	 */
	Map<String, PropertyDescriptor> getProperties();

	/**
	 * Gets the name of the bundle this PropertiesDescriptor belongs to.
	 */
	String getBundleName();

	/**
	 * Gets the symbolic name of the bundle this PropertiesDescriptor belongs to.
	 */
	String getBundleSymbolicName();

	/**
	 * Gets the version of the bundle this PropertiesDescriptor belongs to.
	 */
	String getBundleVersion();

	/**
	 * Gets the PID associated with this PropertiesDescriptor.
	 * 
	 * @return The PID.
	 */
	String getPid();

	/**
	 * returns a unique ID pointing to this PropertiesDescriptor.
	 * 
	 * @return The ID.
	 */
	String getId();

	/**
	 * Gets a copy of these properties for use at runtime by a component
	 * instance.
	 * 
	 * @param platform the platform name
	 * @param group the group name
	 * @param component the component name
	 * @param instance the instance name
	 */
	InstanceProperties instantiate(String platform, String group, String component, String instance);
}
