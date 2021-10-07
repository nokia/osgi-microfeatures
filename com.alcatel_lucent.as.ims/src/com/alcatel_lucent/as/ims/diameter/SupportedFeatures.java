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
 * The Supported-Features AVP wrapper.
 */
public class SupportedFeatures {

	private Long _vendorId;
	private Long _featureListId;
	private Long _featureList;

	/**
	 * Constructor for this class.
	 * 
	 * @param vendorId The vendor id.
	 * @param featureListId The feature list id.
	 * @param featureList The feature list
	 */
	public SupportedFeatures(long vendorId, long featureListId, long featureList) {
		setVendorId(vendorId);
		setFeatureListId(featureListId);
		setFeatureList(featureList);
	}

	/**
	 * Constructor with a Diameter AVP.
	 * 
	 * @param data The value of the avp.
	 * @param version The version of the 3GPP 29.229 document.
	 */
	public SupportedFeatures(byte[] data, Version version) {

		DiameterAVP res = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID, data, false);
		if (res != null) {
			setVendorId(Long.valueOf(Unsigned32Format.getUnsigned32(res.getValue(), 0)));
		}

		res = GroupedFormat.getDiameterAVP(CxUtils.getFeatureListIDAVP(version), data, false);
		if (res != null) {
			setFeatureListId(Long.valueOf(Unsigned32Format.getUnsigned32(res.getValue(), 0)));
		}

		res = GroupedFormat.getDiameterAVP(CxUtils.getFeatureListAVP(version), data, false);
		if (res != null) {
			setFeatureList(Long.valueOf(Unsigned32Format.getUnsigned32(res.getValue(), 0)));
		}

	}

	/**
	 * Creates the grouped Supported-Features AVP.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getSupportedFeaturesAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);

		DiameterAVP vendorIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
		vendorIdAVP.setValue(Unsigned32Format.toUnsigned32(getVendorId()), false);

		def = CxUtils.getFeatureListIDAVP(version);
		DiameterAVP featureListIdAVP = new DiameterAVP(def);
		featureListIdAVP.setValue(Unsigned32Format.toUnsigned32(getFeatureListId()), false);

		def = CxUtils.getFeatureListAVP(version);
		DiameterAVP featureListAVP = new DiameterAVP(def);
		featureListAVP.setValue(Unsigned32Format.toUnsigned32(_featureList), false);
		List groupedAVP = new ArrayList(3);
		groupedAVP.add(vendorIdAVP);
		groupedAVP.add(featureListIdAVP);
		groupedAVP.add(featureListAVP);
		res.addValue(GroupedFormat.toGroupedAVP(groupedAVP), false);

		return res;
	}
	/**
	 * Sets the Vendor-Id value.
	 * 
	 * @param vendorId The Vendor-Id value.
	 */
	public void setVendorId(Long vendorId) {
		_vendorId = vendorId;
	}

	/**
	 * Gets the Vendor-Id value.
	 * 
	 * @return The Vendor-Id value.
	 */
	public Long getVendorId() {
		return _vendorId;
	}

	/**
	 * Sets the Feature-List-ID value.
	 * 
	 * @param featureListId The Feature-List-ID value.
	 */
	public void setFeatureListId(Long featureListId) {
		_featureListId = featureListId;
	}

	/**
	 * Gets the Feature-List-ID value.
	 * 
	 * @return The Feature-List-ID value.
	 */
	public Long getFeatureListId() {
		return _featureListId;
	}

	/**
	 * Sets the Feature-List value.
	 * 
	 * @param featureList The Feature-List value.
	 */
	public void setFeatureList(Long featureList) {
		_featureList = featureList;
	}

	/**
	 * Gets the Feature-List value.
	 * 
	 * @return The Feature-List value.
	 */
	public Long getFeatureList() {
		return _featureList;
	}
}
