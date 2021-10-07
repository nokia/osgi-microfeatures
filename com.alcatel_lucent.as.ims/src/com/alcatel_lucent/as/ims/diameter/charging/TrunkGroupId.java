package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Trunk-Group-Id AVP wrapper.
 */
public class TrunkGroupId {

	private String _incomingTrunkGroupId = null;
	private String _outgoingTrunkGroupId = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public TrunkGroupId(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public TrunkGroupId(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getIncomingTrunkGroupIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setIncomingTrunkGroupId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getOutgoingTrunkGroupIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOutgoingTrunkGroupId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getTrunkGroupIdAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getIncomingTrunkGroupId() != null) {
			def = ChargingUtils.getIncomingTrunkGroupIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getIncomingTrunkGroupId()), false);
				l.add(avp);
			}
		}

		if (getOutgoingTrunkGroupId() != null) {
			def = ChargingUtils.getOutgoingTrunkGroupIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getOutgoingTrunkGroupId()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the incoming trunk group identifier.
	 * 
	 * @param id The identifier.
	 */
	public void setIncomingTrunkGroupId(String id) {
		_incomingTrunkGroupId = id;
	}

	/**
	 * Gets the incoming trunk group identifier (mapped to the
	 * Incoming-Trunk-Group-ID AVP.
	 * 
	 * @return The identifier.
	 */
	public String getIncomingTrunkGroupId() {
		return _incomingTrunkGroupId;
	}

	/**
	 * Sets the outgoing trunk group identifier (mapped to the
	 * Outgoing-Trunk-Group-ID AVP.
	 * 
	 * @param id The identifier.
	 */
	public void setOutgoingTrunkGroupId(String id) {
		this._outgoingTrunkGroupId = id;
	}

	/**
	 * Gets the outgoing trunk group identifier.
	 * 
	 * @return The identifier.
	 */
	public String getOutgoingTrunkGroupId() {
		return _outgoingTrunkGroupId;
	}

}
