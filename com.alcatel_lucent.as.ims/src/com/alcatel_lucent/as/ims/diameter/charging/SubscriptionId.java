// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.SubscriptionIdType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Subscription-Id AVP wrapper.
 */
public class SubscriptionId {

	private SubscriptionIdType _subscriptionIdType = null;
	private String _subscriptionIdData = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param type The Subscription-Id-Type
	 * @param data The Subscription-Id-Data.
	 */
	public SubscriptionId(SubscriptionIdType type, String data) {
		setSubscriptionIdType(type);
		setSubscriptionIdData(data);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SubscriptionId(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SubscriptionId(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getSubscriptionIdTypeAVP();
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			setSubscriptionIdType(SubscriptionIdType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
		}
		
		def = ChargingUtils.getSubscriptionIdDataAVP();
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			setSubscriptionIdData(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
		}

	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getSubscriptionIdAVP();
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getSubscriptionIdTypeAVP();
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(EnumeratedFormat.toEnumerated(getSubscriptionIdType().getValue()), false);
			l.add(avp);
		}

		def = ChargingUtils.getSubscriptionIdDataAVP();
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getSubscriptionIdData()), false);
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Gets the subscription id type (mapped to the Subscription-Id-Type AVP).
	 * 
	 * @return The type.
	 */
	public final SubscriptionIdType getSubscriptionIdType() {
		return _subscriptionIdType;
	}

	/**
	 * Sets the Subscription-Id-Type.
	 * 
	 * @param type The type.
	 */
	private final void setSubscriptionIdType(SubscriptionIdType type) {
		if (type == null) {
			throw new IllegalArgumentException("no type");
		}
		_subscriptionIdType = type;
	}

	/**
	 * Gets the subscription id data (mapped to the Subscription-Id-Data AVP).
	 * 
	 * @return The data.
	 */
	public final String getSubscriptionIdData() {
		return _subscriptionIdData;
	}

	/**
	 * Sets the Subscription-Id-Data.
	 * 
	 * @param data The data.
	 */
	private final void setSubscriptionIdData(String data) {
		if (data == null) {
			throw new IllegalArgumentException("no data");
		}
		_subscriptionIdData = data;
	}

}
