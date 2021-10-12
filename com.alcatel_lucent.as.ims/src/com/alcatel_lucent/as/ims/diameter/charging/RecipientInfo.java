// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.Address;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The Recipient-Info AVP wrapper.
 */
public class RecipientInfo {

	private DestinationInterface _destinationInterface = null;
	private List<RecipientAddress> _recipientAddresses = new ArrayList<RecipientAddress>();
	private RecipientReceivedAddress _recipientReceivedAddress = null;
	private Address _recipientSccpAddress = null;
	private byte[] _smProtocolId = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param data The data of the AVP.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public RecipientInfo(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getDestinationInterfaceAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDestinationInterface(new DestinationInterface(searchedAvp, version));
			}
		}

		def = ChargingUtils.getRecipientAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addRecipientAddress(new RecipientAddress(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getRecipientReceivedAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setRecipientReceivedAddress(new RecipientReceivedAddress(searchedAvp.getValue(), version));
			}
		}

		def = ChargingUtils.getRecipientSccpAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setRecipientSccpAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getSmProtocolIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmProtocolId(searchedAvp.getValue());
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
		DiameterAVPDefinition def = ChargingUtils.getRecipientInfoAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getDestinationInterface() != null) {
			DiameterAVP avp = getDestinationInterface().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		Iterable<RecipientAddress> addresses = getRecipientAddresses();
		if (addresses.iterator().hasNext()) {
			def = ChargingUtils.getRecipientAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (RecipientAddress address : addresses) {
					avp.addValue(address.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		if (getRecipientReceivedAddress() != null) {
			DiameterAVP avp = getRecipientReceivedAddress().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getRecipientSccpAddress() != null) {
			def = ChargingUtils.getRecipientSccpAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getRecipientSccpAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getSmProtocolId() != null) {
			def = ChargingUtils.getSmProtocolIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSmProtocolId(), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Destination-Interface.
	 * 
	 * @param information The information.
	 */
	public void setDestinationInterface(DestinationInterface information) {
		_destinationInterface = information;
	}

	/**
	 * Gets the Destination-Interface.
	 * 
	 * @return The information.
	 */
	public DestinationInterface getDestinationInterface() {
		return _destinationInterface;
	}

	/**
	 * Adds a Recipient-Address.
	 * 
	 * @param address The address.
	 */
	public void addRecipientAddress(RecipientAddress address) {
		if (address != null) {
			_recipientAddresses.add(address);
		}
	}

	/**
	 * Gets the Recipient-Address list.
	 * 
	 * @return The addresses.
	 */
	public Iterable<RecipientAddress> getRecipientAddresses() {
		return _recipientAddresses;
	}

	/**
	 * Sets the Recipient-Received-Address.
	 * 
	 * @param address The address.
	 */
	public void setRecipientReceivedAddress(RecipientReceivedAddress address) {
		_recipientReceivedAddress = address;
	}

	/**
	 * Gets the Recipient-Received-Address.
	 * 
	 * @return The address.
	 */
	public RecipientReceivedAddress getRecipientReceivedAddress() {
		return _recipientReceivedAddress;
	}

	/**
	 * Sets the Recipient-SCCP-Address.
	 * 
	 * @param address The address.
	 */
	public void setRecipientSccpAddress(Address address) {
		_recipientSccpAddress = address;
	}

	/**
	 * Gets the Recipient-SCCP-Address.
	 * 
	 * @return The address.
	 */
	public Address getRecipientSccpAddress() {
		return _recipientSccpAddress;
	}

	/**
	 * Sets the SM-Protocol-ID.
	 * 
	 * @param id The id.
	 */
	public void setSmProtocolId(byte[] id) {
		_smProtocolId = ChargingUtils.copyArray(id);
	}

	/**
	 * Gets the SM-Protocol-ID.
	 * 
	 * @return The id.
	 */
	public byte[] getSmProtocolId() {
		return ChargingUtils.copyArray(_smProtocolId);
	}

}
