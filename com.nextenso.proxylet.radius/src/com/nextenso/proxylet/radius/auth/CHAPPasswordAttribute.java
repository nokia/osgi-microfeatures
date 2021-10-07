package com.nextenso.proxylet.radius.auth;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusUtils;

/**
 * This class encapsulates a CHAP-Password attribute.
 * <p/>
 * 
 * This attribute is not formatted like a regular Radius attribute: it includes
 * an extra field called <i>CHAP identifier</i>.
 */
public class CHAPPasswordAttribute
		extends RadiusAttribute {

	private int _identifier;

	/**
	 * Constructs a new CHAPPasswordAttribute given the CHAP identifier.
	 * 
	 * @param id The CHAP identifier.
	 */
	public CHAPPasswordAttribute(int id) {
		super();
		setType(RadiusUtils.CHAP_PASSWORD);
		_identifier = id;
	}

	/**
	 * Gets the CHAP identifier.
	 * 
	 * @return The identifier.
	 */
	public int getCHAPIdentifier() {
		return _identifier;
	}

	/**
	 * Returns a String representation of the attribute and its values.
	 * 
	 * @return the String representation.
	 */
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder(super.toString());
		buff.append(" [chap-id=").append(getCHAPIdentifier()).append(']');
		return buff.toString();
	}
}
