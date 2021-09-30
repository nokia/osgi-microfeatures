package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The Subscription-Information AVP wrapper.
 */
public class SubscriptionInformation {

	private byte[] _callIdSipHeader;
	private byte[] _fromSipHeader;
	private byte[] _toSipHeader;
	private byte[] _recordRoute;

	/**
	 * Constructor for this class.
	 * 
	 * @param groupedValue The value of the grouped avp.
	 * @param version The Cx version.
	 */
	public SubscriptionInformation(byte[] groupedValue, Version version) {
		DiameterAVPDefinition def = CxUtils.getCallIDSIPHeaderAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setCallIdSipHeader(avp.getValue());
			}
		}
		def = CxUtils.getFromSIPHeaderAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setFromSipHeader(avp.getValue());
			}
		}
		def = CxUtils.getToSIPHeaderAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setToSipHeader(avp.getValue());
			}
		}
		def = CxUtils.getRecordRoutevAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setRecordRoute(avp.getValue());
			}
		}
	}

	/**
	 * Creates a grouped Subscription-Information AVP.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getSubscriptionInfoAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		byte[] value = getCallIdSipHeader();
		def = CxUtils.getCallIDSIPHeaderAVP(version);
		if (def != null && value != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(value, false);
			l.add(avp);
		}

		value = getFromSipHeader();
		def = CxUtils.getFromSIPHeaderAVP(version);
		if (def != null && value != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(value, false);
			l.add(avp);
		}

		value = getToSipHeader();
		def = CxUtils.getToSIPHeaderAVP(version);
		if (def != null && value != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(value, false);
			l.add(avp);
		}

		value = getRecordRoute();
		def = CxUtils.getRecordRoutevAVP(version);
		if (def != null && value != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(value, false);
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Gets the callIdSipHeader.
	 * 
	 * @return The callIdSipHeader.
	 */
	public final byte[] getCallIdSipHeader() {
		return copy(_callIdSipHeader);
	}

	/**
	 * Sets the callIdSipHeader.
	 * 
	 * @param callIdSipHeader The callIdSipHeader.
	 */
	public final void setCallIdSipHeader(byte[] callIdSipHeader) {
		_callIdSipHeader = copy(callIdSipHeader);
	}

	/**
	 * Gets the fromSipHeader.
	 * 
	 * @return The fromSipHeader.
	 */
	public final byte[] getFromSipHeader() {
		return copy(_fromSipHeader);
	}

	/**
	 * Sets the fromSipHeader.
	 * 
	 * @param fromSipHeader The fromSipHeader.
	 */
	public final void setFromSipHeader(byte[] fromSipHeader) {
		_fromSipHeader = copy(fromSipHeader);
	}

	/**
	 * Gets the toSipHeader.
	 * 
	 * @return The toSipHeader.
	 */
	public final byte[] getToSipHeader() {
		return copy(_toSipHeader);
	}

	/**
	 * Sets the toSipHeader.
	 * 
	 * @param toSipHeader The toSipHeader.
	 */
	public final void setToSipHeader(byte[] toSipHeader) {
		_toSipHeader = copy(toSipHeader);
	}

	/**
	 * Gets the recordRoute.
	 * 
	 * @return The recordRoute.
	 */
	public final byte[] getRecordRoute() {
		return copy(_recordRoute);
	}

	/**
	 * Sets the recordRoute.
	 * 
	 * @param recordRoute The recordRoute.
	 */
	public final void setRecordRoute(byte[] recordRoute) {
		_recordRoute = copy(recordRoute);
	}

	private byte[] copy(byte[] value) {
		byte[] res = null;
		if (value != null) {
			res = new byte[value.length];
			System.arraycopy(value, 0, res, 0, value.length);
		}
		return res;
	}

}
