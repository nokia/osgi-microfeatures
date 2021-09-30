package com.alcatel.as.service.metatype;

import java.util.Map;

/**
 * Accessor interface for the runtime properties of one component for one pid.
 * Gives access to values for every instance of this component.
 */
public interface InstanceProperties {

	/** returns a map of property name to InstanceProperty */
	Map<String, InstanceProperty> getProperties();

	/** adds a new component instance */
	InstanceProperties addInstance(String instance_name);

	/** 
         * removes a component instance 
         * @return the remaining properties
         */
	InstanceProperties removeInstance(String instance_name);

	/** returns the PID for this set of properties */
	String getPid();

	/** returns the platform name of this component */
	String getPlatform();

	/** returns the group name of this component */
	String getGroup();

	/** returns the name of this component */
	String getComponent();

	/** returns the descriptor ID for this pid */
	String getDescriptorId();
}
