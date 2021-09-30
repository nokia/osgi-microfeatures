package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.LocationEstimateType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Location-Type AVP wrapper.
 */
public class LocationType {

	private LocationEstimateType _locationEstimateType = null;
	private String _deferredLocationEventType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public LocationType(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public LocationType(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getLocationEstimateTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLocationEstimateType(LocationEstimateType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getDeferredLocationEventTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDeferredLocationEventType(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getLocationTypeAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getLocationEstimateType() != null) {
			def = ChargingUtils.getLocationTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getLocationEstimateType().getValue()), false);
				l.add(avp);
			}
		}

		if (getDeferredLocationEventType() != null) {
			def = ChargingUtils.getWlanTechnologyAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getDeferredLocationEventType()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Location-Estimate-Type.
	 * 
	 * @param type The type.
	 */
	public void setLocationEstimateType(LocationEstimateType type) {
		_locationEstimateType = type;
	}

	/**
	 * Gets the Location-Estimate-Type.
	 * 
	 * @return The type.
	 */
	public LocationEstimateType getLocationEstimateType() {
		return _locationEstimateType;
	}

	/**
	 * Sets the Deferred-Location-Event-Type.
	 * 
	 * @param type The type.
	 */
	public void setDeferredLocationEventType(String type) {
		_deferredLocationEventType = type;
	}

	/**
	 * Gets the Deferred-Location-Event-Type.
	 * 
	 * @return The type.
	 */
	public String getDeferredLocationEventType() {
		return _deferredLocationEventType;
	}

}
