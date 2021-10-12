// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.InterfaceType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Destination-Interface AVP wrapper.
 */
public class DestinationInterface {

	private String _interfaceId = null;
	private String _interfaceText = null;
	private String _interfacePort = null;
	private InterfaceType _interfaceType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public DestinationInterface(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getInterfaceIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setInterfaceId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getInterfaceTextAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setInterfaceText(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getInterfacePortAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setInterfacePort(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}
		
		def = ChargingUtils.getInterfaceTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setInterfaceType(InterfaceType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getDestinationInterfaceAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getInterfaceId() != null) {
			def = ChargingUtils.getInterfaceIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getInterfaceId()), false);
				l.add(avp);
			}
		}

		if (getInterfaceText() != null) {
			def = ChargingUtils.getInterfaceTextAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getInterfaceText()), false);
				l.add(avp);
			}
		}

		if (getInterfacePort() != null) {
			def = ChargingUtils.getInterfacePortAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getInterfacePort()), false);
				l.add(avp);
			}
		}

		if (getInterfaceType()!= null) {
			def = ChargingUtils.getInterfaceTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getInterfaceType().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Interface-Id.
	 * 
	 * @param id The id.
	 */
	public void setInterfaceId(String id) {
		_interfaceId = id;
	}

	/**
	 * Gets the Interface-Id.
	 * 
	 * @return The id.
	 */
	public String getInterfaceId() {
		return _interfaceId;
	}

	/**
	 * Sets the Interface-Text.
	 * 
	 * @param text The text.
	 */
	public void setInterfaceText(String text) {
		_interfaceText = text;
	}

	/**
	 * Gets the Interface-Text.
	 * 
	 * @return The text.
	 */
	public String getInterfaceText() {
		return _interfaceText;
	}

	/**
	 * Sets the Interface-Port.
	 * 
	 * @param port The port.
	 */
	public void setInterfacePort(String port) {
		_interfacePort = port;
	}

	/**
	 * Gets the Interface-Port.
	 * 
	 * @return The port.
	 */
	public String getInterfacePort() {
		return _interfacePort;
	}

	/**
	 * Sets the _interfaceType.
	 * 
	 * @param type The type.
	 */
	public void setInterfaceType(InterfaceType type) {
		_interfaceType = type;
	}

	/**
	 * Gets the _interfaceType.
	 * 
	 * @return The type.
	 */
	public InterfaceType getInterfaceType() {
		return _interfaceType;
	}


}
