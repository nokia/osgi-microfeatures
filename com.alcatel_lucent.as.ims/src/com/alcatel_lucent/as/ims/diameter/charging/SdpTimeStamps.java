package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;

/**
 * The SDP-TimeStamps AVP wrapper.
 */
public class SdpTimeStamps {

	private Date _sdpOfferTimeStamp = null;
	private Date _sdpAnswerTimeStamp = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SdpTimeStamps(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SdpTimeStamps(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getSdpOfferTimestampAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSdpOfferTimeStamp(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getSdpAnswerTimestampAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSdpAnswerTimeStamp(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getMessageBodyAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getSdpOfferTimeStamp() != null) {
			def = ChargingUtils.getSdpOfferTimestampAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getSdpOfferTimeStamp().getTime()), false);
				l.add(avp);
			}
		}

		if (getSdpAnswerTimeStamp() != null) {
			def = ChargingUtils.getSdpAnswerTimestampAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getSdpAnswerTimeStamp().getTime()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the SDP offer timestamp.
	 * 
	 * @param timestamp The timestamp.
	 */
	public void setSdpOfferTimeStamp(Date timestamp) {
		if (timestamp == null) {
			_sdpOfferTimeStamp = null;
		} else {
			_sdpOfferTimeStamp = (Date) timestamp.clone();
		}
	}

	/**
	 * Gets the SDP offer timestamp (mapped to the SDP-Offer-Timestamp AVP).
	 * 
	 * @return The timestamp.
	 */
	public Date getSdpOfferTimeStamp() {
		if (_sdpOfferTimeStamp == null) {
			return null;
		}
		return (Date) _sdpOfferTimeStamp.clone();
	}

	/**
	 * Sets the SDP answer timestamp.
	 * 
	 * @param timestamp The timestamp.
	 */
	public void setSdpAnswerTimeStamp(Date timestamp) {
		if (timestamp == null) {
			_sdpAnswerTimeStamp = null;
		} else {
			_sdpAnswerTimeStamp = (Date) timestamp.clone();
		}
	}

	/**
	 * Gets the SDP answer timestamp (mapped to the SDP-Answer-Timestamp AVP).
	 * 
	 * @return The timestamp.
	 */
	public Date getSdpAnswerTimeStamp() {
		if (_sdpAnswerTimeStamp == null) {
			return null;
		}
		return (Date) _sdpAnswerTimeStamp.clone();
	}
}
