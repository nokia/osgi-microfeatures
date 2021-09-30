package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The LCS-Requestor-Id AVP wrapper.
 */
public class LcsRequestorId {

	private String _lcsDataCodingScheme = null;
	private String _lcsRequestorIdString = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public LcsRequestorId(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getLcsDataCodingSchemeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsDataCodingScheme(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}
		def = ChargingUtils.getLcsRequestorIdStringAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsRequestorIdString(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getLcsRequestorIdAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		if (getLcsDataCodingScheme() != null) {
			def = ChargingUtils.getLcsDataCodingSchemeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsDataCodingScheme()), false);
				avps.add(avp);
			}
		}

		if (getLcsRequestorIdString() != null) {
			def = ChargingUtils.getLcsRequestorIdStringAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLcsRequestorIdString()), false);
				avps.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the LCS-Data-Coding-Scheme.
	 * 
	 * @param scheme The scheme.
	 */
	public void setLcsDataCodingScheme(String scheme) {
		_lcsDataCodingScheme = scheme;
	}

	/**
	 * Gets the LCS-Data-Coding-Scheme.
	 * 
	 * @return The scheme.
	 */
	public String getLcsDataCodingScheme() {
		return _lcsDataCodingScheme;
	}

	/**
	 * Sets the LCS-Requestor-ID-String.
	 * 
	 * @param id The id.
	 */
	public void setLcsRequestorIdString(String id) {
		_lcsRequestorIdString = id;
	}

	/**
	 * Gets the LCS-Requestor-ID-String.
	 * 
	 * @return The id.
	 */
	public String getLcsRequestorIdString() {
		return _lcsRequestorIdString;
	}

}
