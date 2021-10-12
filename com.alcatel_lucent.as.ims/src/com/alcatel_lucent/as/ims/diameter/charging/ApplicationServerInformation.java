// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Application-Server-Information AVP wrapper.
 */
public class ApplicationServerInformation {

	private String _applicationServer = null;
	private List<String> _applicationProvidedCalledPartyAddresses = new ArrayList<String>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public ApplicationServerInformation(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The data of the avp.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public ApplicationServerInformation(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getApplicationServerAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setApplicationServer(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getApplicationProvidedCalledPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addApplicationProvidedCalledPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
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
		DiameterAVPDefinition def = ChargingUtils.getApplicationServerInformationAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		
		if (getApplicationServer() != null) {
			def = ChargingUtils.getApplicationServerAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getApplicationServer()), false);
				l.add(avp);
			}
		}

		Iterable<String> addresses = getApplicationProvidedCalledPartyAddresses();
		if (addresses.iterator().hasNext()) {
			def = ChargingUtils.getApplicationProvidedCalledPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String address : addresses) {
					avp.addValue(UTF8StringFormat.toUtf8String(address), false);
				}
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the application server (mapped to the Application-Server AVP).
	 * 
	 * @param server The application server.
	 */
	public void setApplicationServer(String server) {
		_applicationServer = server;
	}

	/**
	 * Gets the application server.
	 * 
	 * @return The application server.
	 */
	public String getApplicationServer() {
		return _applicationServer;
	}

	/**
	 * Gets the application provided called party addresses (mapped to the
	 * ApplicationProvidedCalledPartyAddress AVP).
	 * 
	 * @return The addresses
	 */
	public Iterable<String> getApplicationProvidedCalledPartyAddresses() {
		return _applicationProvidedCalledPartyAddresses;
	}

	/**
	 * Adds an application provided called party address.
	 * 
	 * @param address The address to be added.
	 */
	public void addApplicationProvidedCalledPartyAddress(String address) {
		_applicationProvidedCalledPartyAddresses.add(address);
	}
}
