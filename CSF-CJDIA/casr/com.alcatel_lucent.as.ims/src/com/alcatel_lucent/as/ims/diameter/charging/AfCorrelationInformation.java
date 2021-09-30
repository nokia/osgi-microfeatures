package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The AF-Correlation-Information AVP wrapper.
 */
public class AfCorrelationInformation {

	private byte[] _afChargingIdentifier = null;
	private List<Flows> _flows = new ArrayList<Flows>();

	private AfCorrelationInformation() {}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param id The AF-Charging-Identifier.
	 */
	public AfCorrelationInformation(byte[] id) {
		this();
		setAfChargingIdentifier(id);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public AfCorrelationInformation(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public AfCorrelationInformation(byte[] data, Version version)
			throws DiameterMissingAVPException {
		this();
		if (data == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getAfChargingIdentifierAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp == null) {
				throw new DiameterMissingAVPException(def);
			}
			setAfChargingIdentifier(searchedAvp.getValue());
		}

		def = ChargingUtils.getFlowsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addFlows(new Flows(searchedAvp.getValue(i), version));
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
		DiameterAVPDefinition def = ChargingUtils.getAfCorrelationInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getAfChargingIdentifierAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(getAfChargingIdentifier(), false);
			avps.add(avp);
		}

		Iterable<Flows> flows = getFlows();
		if (flows.iterator().hasNext()) {
			def = ChargingUtils.getFlowsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (Flows f : flows) {
					avp.addValue(f.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the AF-Charging-Identifier.
	 * 
	 * @param id The id.
	 */
	public void setAfChargingIdentifier(byte[] id) {
		if (id == null) {
			throw new NullPointerException("id is null");
		}
		_afChargingIdentifier = copyArray(id);
	}

	/**
	 * Gets the AF-Charging-Identifier.
	 * 
	 * @return The id.
	 */
	public byte[] getAfChargingIdentifier() {
		return copyArray(_afChargingIdentifier);
	}

	/**
	 * Adds a Flows.
	 * 
	 * @param flows The flows.
	 */
	public void addFlows(Flows flows) {
		if (flows != null) {
			_flows.add(flows);
		}
	}

	/**
	 * Gets the Flows list.
	 * 
	 * @return The _flows.
	 */
	public Iterable<Flows> getFlows() {
		return _flows;
	}

	private byte[] copyArray(byte[] src) {
		if (src == null) {
			return null;
		}

		int len = src.length;
		byte[] res = new byte[len];
		System.arraycopy(src, 0, res, 0, len);
		return res;
	}

}
