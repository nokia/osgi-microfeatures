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
 * The Inter-Operator-Identifier AVP wrapper.
 */
public class InterOperatorIdentifier {

	private String _originatingIoi = null;
	private String _terminatingIoi = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public InterOperatorIdentifier(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public InterOperatorIdentifier(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getOriginatingIoiAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOriginatingIOI(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getTerminatingIoiAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTerminatingIOI(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getInterOperatorIdentifierAVP(version);
		if (def == null) {
			return null;
		}
		
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		if (getOriginatingIOI() != null) {
			def = ChargingUtils.getOriginatingIoiAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getOriginatingIOI()), false);
				l.add(avp);
			}
		}
		if (getTerminatingIOI() != null) {
			def = ChargingUtils.getTerminatingIoiAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getTerminatingIOI()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the originating IOI.
	 * 
	 * @param ioi The IOI.
	 */
	public void setOriginatingIOI(String ioi) {
		_originatingIoi = ioi;
	}

	/**
	 * Gets the originating IOI (mapped to the Originating-IOI AVP).
	 * 
	 * @return The IOI.
	 */
	public String getOriginatingIOI() {
		return _originatingIoi;
	}

	/**
	 * Sets the Terminating IOI.
	 * 
	 * @param ioi The IOI.
	 */
	public void setTerminatingIOI(String ioi) {
		_terminatingIoi = ioi;
	}

	/**
	 * Gets the Terminating IOI (mapped to the Terminating-IOI AVP).
	 * 
	 * @return The IOI.
	 */
	public String getTerminatingIOI() {
		return _terminatingIoi;
	}

}
