// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.FinalUnitAction;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat.IPFilterRule;

/**
 * The Final-Unit-Indication AVP wrapper.
 */
public class FinalUnitIndication {

	private FinalUnitAction _finalUnitAction = null;
	private List<IPFilterRule> _restrictionFilterRules = new ArrayList<IPFilterRule>();
	private List<String> _filterIds = new ArrayList<String>();
	private RedirectServer _redirectServer = null;

	private FinalUnitIndication() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param action The Final-Unit-Action.
	 */
	public FinalUnitIndication(FinalUnitAction action) {
		this();
		setFinalUnitAction(action);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @exception DiameterMissingAVPException if the Final-Unit-Action avp is not
	 *              present in the avp.
	 */
	public FinalUnitIndication(DiameterAVP avp)
			throws DiameterMissingAVPException {
		this();
		if (avp == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getFinalUnitActionAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setFinalUnitAction(FinalUnitAction.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getRestrictionFilterRuleAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			for (int i = 0; i < searchedAvp.getValueSize(); i++) {
				byte[] value = searchedAvp.getValue(i);
				addRestrictionFilterRule(IPFilterRuleFormat.INSTANCE.getIPFilterRule(value, 0, value.length));
			}
		}

		def = ChargingUtils.getFilterIdAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			for (int i = 0; i < searchedAvp.getValueSize(); i++) {
				byte[] value = searchedAvp.getValue(i);
				addFilterId(UTF8StringFormat.getUtf8String(value));
			}
		}

		def = ChargingUtils.getRestrictionFilterRuleAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setRedirectServer(new RedirectServer(searchedAvp));
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getFinalUnitIndicationAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getFinalUnitActionAVP();
		DiameterAVP avp = new DiameterAVP(def);
		avp.setValue(EnumeratedFormat.toEnumerated(getFinalUnitAction().getValue()), false);
		avps.add(avp);

		Iterable<IPFilterRule> rules = getRestrictionFilterRules();
		if (rules.iterator().hasNext()) {
			def = ChargingUtils.getRestrictionFilterRuleAVP();
			avp = new DiameterAVP(def);
			for (IPFilterRule rule : rules) {
				avp.addValue(IPFilterRuleFormat.INSTANCE.toIPFilterRule(rule), false);
			}
			avps.add(avp);
		}

		Iterable<String> ids = getFilterIds();
		if (ids.iterator().hasNext()) {
			def = ChargingUtils.getFilterIdAVP();
			avp = new DiameterAVP(def);
			for (String id : ids) {
				avp.addValue(UTF8StringFormat.toUtf8String(id), false);
			}
			avps.add(avp);
		}

		if (getRedirectServer() != null) {
			avp = getRedirectServer().toAvp();
			avps.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the Final-Unit-Action.
	 * 
	 * @param action The action.
	 */
	protected void setFinalUnitAction(FinalUnitAction action) {
		if (action == null) {
			throw new NullPointerException("action is null");
		}
		_finalUnitAction = action;
	}

	/**
	 * Gets the Final-Unit-Action.
	 * 
	 * @return The action.
	 */
	public FinalUnitAction getFinalUnitAction() {
		return _finalUnitAction;
	}

	/**
	 * Adds a Restriction-Filter-Rule.
	 * 
	 * @param rule The rule.
	 */
	public void addRestrictionFilterRule(IPFilterRule rule) {
		if (rule != null) {
			_restrictionFilterRules.add(rule);
		}
	}

	/**
	 * Gets the Restriction-Filter-Rule list.
	 * 
	 * @return The rules.
	 */
	public Iterable<IPFilterRule> getRestrictionFilterRules() {
		return _restrictionFilterRules;
	}

	/**
	 * Adds a Filter-Id.
	 * 
	 * @param id The id.
	 */
	public void addFilterId(String id) {
		if (id != null) {
			_filterIds.add(id);
		}
	}

	/**
	 * Gets the Filter-Id list.
	 * 
	 * @return The ids.
	 */
	public Iterable<String> getFilterIds() {
		return _filterIds;
	}

	/**
	 * Sets the Redirect-Server.
	 * 
	 * @param server The server.
	 */
	public void setRedirectServer(RedirectServer server) {
		_redirectServer = server;
	}

	/**
	 * Gets the Redirect-Server.
	 * 
	 * @return The server.
	 */
	public RedirectServer getRedirectServer() {
		return _redirectServer;
	}

}