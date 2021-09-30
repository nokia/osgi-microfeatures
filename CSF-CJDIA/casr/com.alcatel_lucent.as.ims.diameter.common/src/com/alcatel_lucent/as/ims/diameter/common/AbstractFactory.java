package com.alcatel_lucent.as.ims.diameter.common;

import com.alcatel_lucent.as.ims.Version;

/**
 * The Abstract Factory.
 */
public class AbstractFactory {

	private Version _version;

	protected AbstractFactory(Version version) {
		_version = version;
	}

	/**
	 * /** Gets the supported version.
	 * 
	 * @return The supported version.
	 */
	public Version getVersion() {
		return _version;
	}

}
