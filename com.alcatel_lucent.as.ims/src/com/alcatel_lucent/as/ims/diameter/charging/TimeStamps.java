package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The time stamps (Time-Stamps AVP wrapper).
 */
public class TimeStamps {

	private Date _sipRequestTimestamp = null;
	private Date _sipResponseTimestamp = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public TimeStamps(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getSipRequestTimestampAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				if (def.getDiameterAVPFormat() == TimeFormat.INSTANCE) {
					setSipRequestTimestamp(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
				} else if (def.getDiameterAVPFormat() == UTF8StringFormat.INSTANCE) {
					setSipRequestTimestamp(new Date(ChargingUtils.fromUtcTimestamp(UTF8StringFormat.getUtf8String(searchedAvp.getValue()))));
				}
			}
		}

		def = ChargingUtils.getSipResponseTimestampAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				if (def.getDiameterAVPFormat() == TimeFormat.INSTANCE) {
					setSipResponseTimestamp(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
				} else if (def.getDiameterAVPFormat() == UTF8StringFormat.INSTANCE) {
					setSipResponseTimestamp(new Date(ChargingUtils.fromUtcTimestamp(UTF8StringFormat.getUtf8String(searchedAvp.getValue()))));
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
		DiameterAVPDefinition def = ChargingUtils.getTimeStampsAVP(version);
		if (def == null) {
			return null;
		}
		
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		
		Date date= getSipRequestTimestamp();
		if ( date != null) {
			def = ChargingUtils.getSipRequestTimestampAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				if (def.getDiameterAVPFormat() == TimeFormat.INSTANCE) {
					avp.setValue(TimeFormat.toTime(date.getTime()), false);
				}else if (def.getDiameterAVPFormat() == UTF8StringFormat.INSTANCE) {
					avp.setValue(UTF8StringFormat.toUtf8String(ChargingUtils.toUtcTimestamp(date.getTime())), false);
				}
				l.add(avp);
			}
		}
		
		date= getSipResponseTimestamp();
		if ( date != null) {
			def = ChargingUtils.getSipResponseTimestampAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				if (def.getDiameterAVPFormat() == TimeFormat.INSTANCE) {
					avp.setValue(TimeFormat.toTime(date.getTime()), false);
				}else if (def.getDiameterAVPFormat() == UTF8StringFormat.INSTANCE) {
					avp.setValue(UTF8StringFormat.toUtf8String(ChargingUtils.toUtcTimestamp(date.getTime())), false);
				}
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Gets the SIP request timestamp (mapped to the SIP-Request-Timestamp AVP).
	 * 
	 * @return The timestamp.
	 */
	protected final Date getSipRequestTimestamp() {
		return _sipRequestTimestamp;
	}

	/**
	 * Sets the SIP request timestamp.
	 * 
	 * @param timestamp The timestamp.
	 */
	protected final void setSipRequestTimestamp(Date timestamp) {
		_sipRequestTimestamp = timestamp;
	}

	/**
	 * Gets the SIP response timestamp (mapped to the SIP-Response-Timestamp
	 * AVP)..
	 * 
	 * @return The timestamp.
	 */
	protected final Date getSipResponseTimestamp() {
		return _sipResponseTimestamp;
	}

	/**
	 * Sets the SIP response timestamp.
	 * 
	 * @param timestamp The timestamp.
	 */
	protected final void setSipResponseTimestamp(Date timestamp) {
		_sipResponseTimestamp = timestamp;
	}

}
