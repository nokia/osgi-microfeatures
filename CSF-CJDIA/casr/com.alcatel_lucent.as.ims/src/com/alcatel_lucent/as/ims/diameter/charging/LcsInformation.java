package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShUtils;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The LCS-Information AVP wrapper.
 */
public class LcsInformation {

	private LcsClientId _lcsClientId = null;
	private LocationType _locationType = null;
	private String _locationEstimate = null;
	private String _positioningData = null;
	// IMSI ???
	private byte[] _msisdn = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public LcsInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getLcsClientIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsClientId(new LcsClientId(searchedAvp, version));
			}
		}

		def = ChargingUtils.getLocationTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLocationType(new LocationType(searchedAvp, version));
			}
		}

		def = ChargingUtils.getLocationEstimateAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLocationEstimate(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getPositioningDataAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPositioningData(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ShUtils.getMsisdnAvpDefinition(ChargingUtils.getVersion29329(version));
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMsisdn(searchedAvp.getValue());
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
		DiameterAVPDefinition def = ChargingUtils.getLcsInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		if (getLcsClientId() != null) {
			DiameterAVP avp = getLcsClientId().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		if (getLocationType() != null) {
			DiameterAVP avp = getLocationType().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		if (getLocationEstimate() != null) {
			def = ChargingUtils.getLocationEstimateAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getLocationEstimate()), false);
			}
		}

		if (getPositioningData() != null) {
			def = ChargingUtils.getPositioningDataAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getPositioningData()), false);
			}
		}

		if (getMsisdn() != null) {
			def = ShUtils.getMsisdnAvpDefinition(ChargingUtils.getVersion29329(version));
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getMsisdn(), false);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the LCS-Client-Id.
	 * 
	 * @param identity The identity.
	 */
	public void setLcsClientId(LcsClientId identity) {
		_lcsClientId = identity;
	}

	/**
	 * Gets the LCS-Client-Id.
	 * 
	 * @return The identity.
	 */
	public LcsClientId getLcsClientId() {
		return _lcsClientId;
	}

	/**
	 * Sets the Location-Type.
	 * 
	 * @param type The type.
	 */
	public void setLocationType(LocationType type) {
		_locationType = type;
	}

	/**
	 * Gets the Location-Type.
	 * 
	 * @return The type.
	 */
	public LocationType getLocationType() {
		return _locationType;
	}

	/**
	 * Sets the Location-Estimate.
	 * 
	 * @param estimate The estimate.
	 */
	public void setLocationEstimate(String estimate) {
		_locationEstimate = estimate;
	}

	/**
	 * Gets the Location-Estimate.
	 * 
	 * @return The estimate.
	 */
	public String getLocationEstimate() {
		return _locationEstimate;
	}

	/**
	 * Sets the Positioning-Data.
	 * 
	 * @param data The data.
	 */
	public void setPositioningData(String data) {
		_positioningData = data;
	}

	/**
	 * Gets the Positioning-Data.
	 * 
	 * @return The data.
	 */
	public String getPositioningData() {
		return _positioningData;
	}

	/**
	 * Sets the MSISDN.
	 * 
	 * @param msisdn The MSISDN.
	 */
	public void setMsisdn(byte[] msisdn) {
		_msisdn = ChargingUtils.copyArray(msisdn);
	}

	/**
	 * Gets the MSISDN.
	 * 
	 * @return The MSISDN.
	 */
	public byte[] getMsisdn() {
		return ChargingUtils.copyArray(_msisdn);
	}

}
