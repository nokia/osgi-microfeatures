// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AddressType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AddresseeType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * 
 * The Recipient-Address AVP wrapper.
 */
public class RecipientAddress {

	private AddressType _addressType = null;
	private String _addressData = null;
	private AddressDomain _addressDomain = null;
	private AddresseeType _addresseeType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public RecipientAddress(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getAddressTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAddressType(AddressType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getAddressDataAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAddressData(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAddressDomainAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAddressDomain(new AddressDomain(searchedAvp, version));
			}
		}

		def = ChargingUtils.getAddresseeTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAddresseeType(AddresseeType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getRecipientAddressAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getAddressType() != null) {
			def = ChargingUtils.getAddressTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getAddressType().getValue()), false);
				l.add(avp);
			}
		}

		if (getAddressData() != null) {
			def = ChargingUtils.getAddressTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getAddressData()), false);
				l.add(avp);
			}
		}

		if (getAddressDomain() != null) {
			DiameterAVP avp = getAddressDomain().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getAddresseeType() != null) {
			def = ChargingUtils.getAddresseeTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getAddresseeType().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Address-Type.
	 * 
	 * @param type The type.
	 */
	public void setAddressType(AddressType type) {
		_addressType = type;
	}

	/**
	 * Gets the Address-Type.
	 * 
	 * @return The type.
	 */
	public AddressType getAddressType() {
		return _addressType;
	}

	/**
	 * Sets the Address-Data.
	 * 
	 * @param data The data.
	 */
	public void setAddressData(String data) {
		_addressData = data;
	}

	/**
	 * Gets the Address-Data.
	 * 
	 * @return The data.
	 */
	public String getAddressData() {
		return _addressData;
	}

	/**
	 * Sets the Address-Domain.
	 * 
	 * @param domain The domain.
	 */
	public void setAddressDomain(AddressDomain domain) {
		_addressDomain = domain;
	}

	/**
	 * Gets the Address-Domain.
	 * 
	 * @return The domain.
	 */
	public AddressDomain getAddressDomain() {
		return _addressDomain;
	}

	/**
	 * Sets the Addressee-Type.
	 * 
	 * @param type The type.
	 */
	public void setAddresseeType(AddresseeType type) {
		_addresseeType = type;
	}

	/**
	 * Gets the Addressee-Type.
	 * 
	 * @return The type.
	 */
	public AddresseeType getAddresseeType() {
		return _addresseeType;
	}

}
