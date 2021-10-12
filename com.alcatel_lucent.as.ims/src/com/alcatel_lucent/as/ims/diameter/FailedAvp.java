// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import java.util.ArrayList;
import java.util.List;

import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The Failed-AVP AVP wrapper.
 */
public class FailedAvp {

	private List<DiameterAVP> _avps = new ArrayList<DiameterAVP>();

	/**
	 * Constructor for this class. 
	 *
	 * @param data
	 */
	public FailedAvp(byte[] data) {
		List<DiameterAVP> avps = GroupedFormat.getGroupedAVPs(data, true);
		_avps.addAll(avps);
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = DiameterBaseConstants.AVP_FAILED_AVP;
		DiameterAVP res = new DiameterAVP(def);
		if (!_avps.isEmpty()) {
			List<DiameterAVP> l = new ArrayList<DiameterAVP>();
			l.addAll(_avps);
			res.setValue(GroupedFormat.toGroupedAVP(l), false);
		}
		return res;
	}

	/**
	 * Adds an AVP.
	 * 
	 * @param avp The avp to be added.
	 */
	public void addAvp(DiameterAVP avp) {
		if (avp != null) {
			_avps.add(avp);
		}

	}

	/**
	 * Gets the AVP list.
	 * 
	 * @return The avps.
	 */
	public Iterable<DiameterAVP> getAvps() {
		return _avps;
	}

}
