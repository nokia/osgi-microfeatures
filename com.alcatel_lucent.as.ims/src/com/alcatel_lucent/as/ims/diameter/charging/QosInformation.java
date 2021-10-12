// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.QosClassIdentifier;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The QoS-Information AVP wrapper.
 */
public class QosInformation {

	private QosClassIdentifier _qosClassIdentifier = null;
	private Long _maxRequestedBandwidthUL = null;
	private Long _maxRequestedBandwidthDL = null;
	private Long _garanteedBitrateUL = null;
	private Long _garanteedBitrateDL = null;
	private byte[] _bearerIdentifier = null;
	private AllocationRetentionPriority _allocationRetentionPriority = null;
	private Long _apnAggregateMaxBitrateUL = null;
	private Long _apnAggregateMaxBitrateDL = null;
	private List<DiameterAVP> _avps = new ArrayList<DiameterAVP>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory avp is not present in
	 *              the avp.
	 */
	public QosInformation(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		byte[] data = avp.getValue();
		DiameterAVPDefinition def = ChargingUtils.getQoSClassIdentifierAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setQosClassIdentifier(QosClassIdentifier.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMaxRequestedBandwidthULAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMaxRequestedBandwidthUL(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMaxRequestedBandwidthDLAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMaxRequestedBandwidthDL(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getGaranteedBitrateULAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setGaranteedBitrateUL(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getGaranteedBitrateDLAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setGaranteedBitrateDL(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getBearerIdentifierAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setBearerIdentifier(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getAllocationRetentionPriorityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAllocationRetentionPriority(new AllocationRetentionPriority(searchedAvp, version));
			}
		}

		def = ChargingUtils.getApnAggregateMaxBitrateULAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setApnAggregateMaxBitrateUL(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getApnAggregateMaxBitrateDLAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setApnAggregateMaxBitrateDL(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		List<DiameterAVP> avps = GroupedFormat.getGroupedAVPs(data, false);
		for (DiameterAVP a : avps) {
			def = a.getDiameterAVPDefinition();
			if (def == ChargingUtils.getQoSClassIdentifierAVP(version) || def == ChargingUtils.getMaxRequestedBandwidthULAVP(version)
					|| def == ChargingUtils.getMaxRequestedBandwidthDLAVP(version) || def == ChargingUtils.getGaranteedBitrateULAVP(version)
					|| def == ChargingUtils.getGaranteedBitrateDLAVP(version) || def == ChargingUtils.getBearerIdentifierAVP(version)
					|| def == ChargingUtils.getAllocationRetentionPriorityAVP(version) || def == ChargingUtils.getApnAggregateMaxBitrateULAVP(version)
					|| def == ChargingUtils.getApnAggregateMaxBitrateDLAVP(version)) {
				continue;
			}
			addAvp(a);
		}
	}

	/**
	 * Creates a grouped QoS-Information AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getQosInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getQosClassIdentifier() != null) {
			def = ChargingUtils.getQoSClassIdentifierAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getQosClassIdentifier().getValue()), false);
				l.add(avp);
			}
		}

		if (getMaxRequestedBandwidthUL() != null) {
			def = ChargingUtils.getMaxRequestedBandwidthULAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getMaxRequestedBandwidthUL()), false);
				l.add(avp);
			}
		}

		if (getMaxRequestedBandwidthDL() != null) {
			def = ChargingUtils.getMaxRequestedBandwidthDLAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getMaxRequestedBandwidthDL()), false);
				l.add(avp);
			}
		}

		if (getGaranteedBitrateUL() != null) {
			def = ChargingUtils.getGaranteedBitrateULAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getGaranteedBitrateUL()), false);
				l.add(avp);
			}
		}

		if (getGaranteedBitrateDL() != null) {
			def = ChargingUtils.getGaranteedBitrateDLAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getGaranteedBitrateDL()), false);
				l.add(avp);
			}
		}

		if (getBearerIdentifier() != null) {
			def = ChargingUtils.getBearerIdentifierAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getBearerIdentifier(), false);
				l.add(avp);
			}
		}

		if (getAllocationRetentionPriority() != null) {
			DiameterAVP avp = getAllocationRetentionPriority().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getApnAggregateMaxBitrateUL() != null) {
			def = ChargingUtils.getApnAggregateMaxBitrateULAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getApnAggregateMaxBitrateUL()), false);
				l.add(avp);
			}
		}

		if (getApnAggregateMaxBitrateDL() != null) {
			def = ChargingUtils.getApnAggregateMaxBitrateDLAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getApnAggregateMaxBitrateDL()), false);
				l.add(avp);
			}
		}

		for (DiameterAVP avp : getAvps()) {
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
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

	/**
	 * Sets the QoS-Class-Identifier.
	 * 
	 * @param id The id.
	 */
	public void setQosClassIdentifier(QosClassIdentifier id) {
		_qosClassIdentifier = id;
	}

	/**
	 * Gets the QoS-Class-Identifier.
	 * 
	 * @return The id.
	 */
	public QosClassIdentifier getQosClassIdentifier() {
		return _qosClassIdentifier;
	}

	/**
	 * Sets the Max-Requested-Bandwidth-UL.
	 * 
	 * @param bandwidth The bandwidth.
	 */
	public void setMaxRequestedBandwidthUL(Long bandwidth) {
		_maxRequestedBandwidthUL = bandwidth;
	}

	/**
	 * Gets the Max-Requested-Bandwidth-UL.
	 * 
	 * @return The bandwidth.
	 */
	public Long getMaxRequestedBandwidthUL() {
		return _maxRequestedBandwidthUL;
	}

	/**
	 * Sets the Max-Requested-Bandwidth-DL.
	 * 
	 * @param bandwidth The bandwidth.
	 */
	public void setMaxRequestedBandwidthDL(Long bandwidth) {
		_maxRequestedBandwidthDL = bandwidth;
	}

	/**
	 * Gets the Max-Requested-Bandwidth-DL.
	 * 
	 * @return The bandwidth.
	 */
	public Long getMaxRequestedBandwidthDL() {
		return _maxRequestedBandwidthDL;
	}

	/**
	 * Sets the Garanteed-Bitrate-UL.
	 * 
	 * @param bitrate The bitrate.
	 */
	public void setGaranteedBitrateUL(Long bitrate) {
		_garanteedBitrateUL = bitrate;
	}

	/**
	 * Gets the Garanteed-Bitrate-UL.
	 * 
	 * @return The bitrate.
	 */
	public Long getGaranteedBitrateUL() {
		return _garanteedBitrateUL;
	}

	/**
	 * Sets the Garanteed-Bitrate-DL.
	 * 
	 * @param bitrate The bitrate.
	 */
	public void setGaranteedBitrateDL(Long bitrate) {
		_garanteedBitrateDL = bitrate;
	}

	/**
	 * Gets the Garanteed-Bitrate-DL.
	 * 
	 * @return The bitrate.
	 */
	public Long getGaranteedBitrateDL() {
		return _garanteedBitrateDL;
	}

	/**
	 * Sets the Bearer-Identifier.
	 * 
	 * @param id The id.
	 */
	public void setBearerIdentifier(byte[] id) {
		_bearerIdentifier = copyArray(id);
	}

	/**
	 * Gets the Bearer-Identifier.
	 * 
	 * @return The id.
	 */
	public byte[] getBearerIdentifier() {
		return copyArray(_bearerIdentifier);
	}

	/**
	 * Sets the Allocation-Retention-Priority.
	 * 
	 * @param priority The priority.
	 */
	public void setAllocationRetentionPriority(AllocationRetentionPriority priority) {
		_allocationRetentionPriority = priority;
	}

	/**
	 * Gets the Allocation-Retention-Priority.
	 * 
	 * @return The priority.
	 */
	public AllocationRetentionPriority getAllocationRetentionPriority() {
		return _allocationRetentionPriority;
	}

	/**
	 * Sets the APN-Aggregate-Max-Bitrate-UL.
	 * 
	 * @param bitrate The bitrate.
	 */
	public void setApnAggregateMaxBitrateUL(Long bitrate) {
		_apnAggregateMaxBitrateUL = bitrate;
	}

	/**
	 * Gets the APN-Aggregate-Max-Bitrate-UL.
	 * 
	 * @return The bitrate.
	 */
	public Long getApnAggregateMaxBitrateUL() {
		return _apnAggregateMaxBitrateUL;
	}

	/**
	 * Sets the APN-Aggregate-Max-Bitrate-DL.
	 * 
	 * @param bitrate The bitrate.
	 */
	public void setApnAggregateMaxBitrateDL(Long bitrate) {
		_apnAggregateMaxBitrateDL = bitrate;
	}

	/**
	 * Gets the APN-Aggregate-Max-Bitrate-DL.
	 * 
	 * @return The bitrate.
	 */
	public Long getApnAggregateMaxBitrateDL() {
		return _apnAggregateMaxBitrateDL;
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
