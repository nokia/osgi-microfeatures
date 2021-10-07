package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PreemptionCapability;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PreemptionVulnerability;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Allocation-Retention-Priority AVP wrapper.
 */
public class AllocationRetentionPriority {

	private Long _priorityLevel = null;
	private PreemptionCapability _preemptionCapability = null;
	private PreemptionVulnerability _preemptionVulnerability = null;

	private AllocationRetentionPriority() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param priorityLevel The Priority-Level.
	 */
	public AllocationRetentionPriority(Long priorityLevel) {
		this();
		setPriorityLevel(priorityLevel);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory avp is not present in
	 *              the avp.
	 */
	public AllocationRetentionPriority(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		this();
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getPriorityLevelAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp == null) {
				throw new DiameterMissingAVPException(def);
			}
			setPriorityLevel(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getPreemptionCapabilityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPreemptionCapability(PreemptionCapability.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPreemptionVulnerabilityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPreemptionVulnerability(PreemptionVulnerability.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

	}

	/**
	 * Creates a grouped Allocation-Retention-Priority AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getAllocationRetentionPriorityAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getPriorityLevelAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getPriorityLevel()), false);
			l.add(avp);
		}

		if (getPreemptionCapability() != null) {
			def = ChargingUtils.getPreemptionCapabilityAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPreemptionCapability().getValue()), false);
				l.add(avp);
			}
		}

		if (getPreemptionVulnerability() != null) {
			def = ChargingUtils.getPreemptionVulnerabilityAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPreemptionVulnerability().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Priority-Level.
	 * 
	 * @param level The level.
	 */
	protected void setPriorityLevel(Long level) {
		if (level == null) {
			throw new NullPointerException("level is null");
		}
		_priorityLevel = level;
	}

	/**
	 * Gets the Priority-Level.
	 * 
	 * @return The level.
	 */
	public Long getPriorityLevel() {
		return _priorityLevel;
	}

	/**
	 * Sets the Pre-emption-Capability.
	 * 
	 * @param capability The capability.
	 */
	public void setPreemptionCapability(PreemptionCapability capability) {
		_preemptionCapability = capability;
	}

	/**
	 * Gets the Pre-emption-Capability.
	 * 
	 * @return The capability.
	 */
	public PreemptionCapability getPreemptionCapability() {
		return _preemptionCapability;
	}

	/**
	 * Sets the Pre-emption-Vulnerability.
	 * 
	 * @param vulnerability The vulnerability.
	 */
	public void setPreemptionVulnerability(PreemptionVulnerability vulnerability) {
		_preemptionVulnerability = vulnerability;
	}

	/**
	 * Gets the Pre-emption-Vulnerability.
	 * 
	 * @return The vulnerability.
	 */
	public PreemptionVulnerability getPreemptionVulnerability() {
		return _preemptionVulnerability;
	}

}
