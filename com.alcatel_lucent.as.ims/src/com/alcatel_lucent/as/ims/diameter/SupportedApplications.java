// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Supported-Applications AVP wrapper.
 */
public class SupportedApplications {

	private List<Long> _authApplicationIds = new ArrayList<Long>();
	private List<Long> _acctApplicationIds = new ArrayList<Long>();
	private List<Long> _vendorIds = new ArrayList<Long>();

	/**
	 * Constructor for this class. 
	 */
	public SupportedApplications() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param authApplicationIds The list of supported Auth-Application-Ids
	 * @param acctApplicationIds The list of supported Acct-Application-Ids
	 * @param vendorIds The list of supported Vendor--Ids
	 */
	public SupportedApplications(List<Long> authApplicationIds, List<Long> acctApplicationIds, List<Long> vendorIds) {
		_authApplicationIds.addAll(authApplicationIds);
		_acctApplicationIds.addAll(acctApplicationIds);
		_vendorIds.addAll(vendorIds);
	}

	/**
	 * Constructor with a Diameter AVP.
	 * 
	 * @param avp The avp which contains the charging collection.
	 * @param version The version of the 3GPP 29.229 document.
	 * @throws UncompatibleAVPDefinition if the avp is not compatible with the
	 *           version.
	 */
	public SupportedApplications(DiameterAVP avp, Version version)
			throws UncompatibleAVPDefinition {
		DiameterAVPDefinition def = CxUtils.getSupportedApplicationsAVP(version);
		if (def == null) {
			throw new UncompatibleAVPDefinition(avp.getDiameterAVPDefinition());
		}
		if ( !avp.isInstanceOf(def)) {
			throw new UncompatibleAVPDefinition(def, avp.getDiameterAVPDefinition());
		}

		List avps = GroupedFormat.getGroupedAVPs(avp.getValue(), false);
		for (int i = 0; i < avps.size(); i++) {
			DiameterAVP theAvp = (DiameterAVP) avps.get(i);
			if (theAvp.isInstanceOf(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID)) {
				addAuthApplicationId(Unsigned32Format.getUnsigned32(theAvp.getValue(), 0));
			} else if (theAvp.isInstanceOf(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID)) {
				addAcctApplicationId(Unsigned32Format.getUnsigned32(theAvp.getValue(), 0));
			} else if (avp.isInstanceOf(DiameterBaseConstants.AVP_VENDOR_ID)) {
				addVendorId(Unsigned32Format.getUnsigned32(avp.getValue(), 0));
			}
		}

	}

	/**
	 * Creates the grouped Supported-Applications AVP.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getSupportedApplicationsAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);

		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		// Adds Auth-Application-Ids
		for (long id : _authApplicationIds) {
			DiameterAVP authAppIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
			authAppIdAVP.setValue(Unsigned32Format.toUnsigned32(id), false);
			avps.add(authAppIdAVP);
		}

		// Adds Acct-Application-Id
		for (long id : _acctApplicationIds) {
			DiameterAVP acctAppIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
			acctAppIdAVP.setValue(Unsigned32Format.toUnsigned32(id), false);
			avps.add(acctAppIdAVP);
		}

		// Adds Vendor-Specific-Application-Id
		for (long id : _vendorIds) {
			DiameterAVP vendorIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
			vendorIdAVP.setValue(Unsigned32Format.toUnsigned32(id), false);
			avps.add(vendorIdAVP);
		}
		res.setValue(GroupedFormat.toGroupedAVP(avps), true);
		return res;
	}

	/**
	 * Adds an auth application id.
	 * 
	 * @param id The id.
	 */
	public void addAuthApplicationId(Long id) {
		_authApplicationIds.add(id);
	}

	/**
	 * Adds an acct application id.
	 * 
	 * @param id The id.
	 */
	public void addAcctApplicationId(Long id) {
		_acctApplicationIds.add(id);
	}

	/**
	 * Adds a vendor id.
	 * 
	 * @param id The id.
	 */
	public void addVendorId(Long id) {
		_vendorIds.add(id);
	}

}
