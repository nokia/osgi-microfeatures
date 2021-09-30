package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * The Traffic-Data-Volumes AVP wrapper.
 */
public class TrafficDataVolumes {

	private QosInformation _qosInformation = null;
	private Long _accountingInputOctets = null;
	private Long _accountingInputPackets = null;
	private Long _accountingOutputOctets = null;
	private Long _accountingOutputPackets = null;
	private Integer _changeCondition = null;
	private Date _changeTime = null;
	private byte[] _3gppUserLocationInfo = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory avp is not present in
	 *              the avp.
	 */
	public TrafficDataVolumes(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory avp is not present in
	 *              the avp.
	 */
	public TrafficDataVolumes(byte[] data, Version version)
			throws DiameterMissingAVPException {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getQosInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setQosInformation(new QosInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getAccountingInputOctetsAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setAccountingInputOctets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getAccountingInputPacketsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setAccountingInputPackets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getAccountingOutputOctetsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setAccountingOutputOctets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getAccountingOutputPacketsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setAccountingOutputPackets(Long.valueOf(Unsigned64Format.getUnsigned64Long(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getChangeConditionAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChangeCondition(Integer.valueOf(Integer32Format.getInteger32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getChangeTimeAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChangeTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.get3gppUserLocationInfoAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppUserLocationInfo(searchedAvp.getValue());
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
		DiameterAVPDefinition def = ChargingUtils.getTrafficDataVolumesAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getQosInformation() != null) {
			DiameterAVP avp = getQosInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getAccountingInputOctets() != null) {
			def = ChargingUtils.getAccountingInputOctetsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getAccountingInputOctets()), false);
			l.add(avp);

		}

		if (getAccountingInputPackets() != null) {
			def = ChargingUtils.getAccountingInputPacketsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getAccountingInputPackets()), false);
			l.add(avp);
		}

		if (getAccountingOutputOctets() != null) {
			def = ChargingUtils.getAccountingOutputOctetsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getAccountingOutputOctets()), false);
			l.add(avp);
		}

		if (getAccountingOutputPackets() != null) {
			def = ChargingUtils.getAccountingOutputPacketsAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getAccountingOutputPackets()), false);
			l.add(avp);
		}

		if (getChangeCondition() != null) {
			def = ChargingUtils.getChangeConditionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Integer32Format.toInteger32(getChangeCondition()), false);
				l.add(avp);
			}
		}

		if (getChangeTime() != null) {
			def = ChargingUtils.getChangeTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getChangeTime().getTime()), false);
				l.add(avp);
			}
		}

		if (get3gppUserLocationInfo() != null) {
			def = ChargingUtils.get3gppUserLocationInfoAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gppUserLocationInfo(), false);
				l.add(avp);
			}
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
	 * Sets the QoS-Information.
	 * 
	 * @param info The info.
	 */
	public void setQosInformation(QosInformation info) {
		_qosInformation = info;
	}

	/**
	 * Gets the QoS-Information.
	 * 
	 * @return The info.
	 */
	public QosInformation getQosInformation() {
		return _qosInformation;
	}

	/**
	 * Sets the Accounting-Input-Octets.
	 * 
	 * @param octets The octets.
	 */
	public void setAccountingInputOctets(Long octets) {
		_accountingInputOctets = octets;
	}

	/**
	 * Gets the Accounting-Input-Octets.
	 * 
	 * @return The octets.
	 */
	public Long getAccountingInputOctets() {
		return _accountingInputOctets;
	}

	/**
	 * Sets the Accounting-Input-Packets.
	 * 
	 * @param octets The octets.
	 */
	public void setAccountingInputPackets(Long octets) {
		_accountingInputPackets = octets;
	}

	/**
	 * Gets the Accounting-Input-Packets.
	 * 
	 * @return The octets.
	 */
	public Long getAccountingInputPackets() {
		return _accountingInputPackets;
	}

	/**
	 * Sets the Accounting-Output-Octets.
	 * 
	 * @param octets The octets.
	 */
	public void setAccountingOutputOctets(Long octets) {
		_accountingOutputOctets = octets;
	}

	/**
	 * Gets the Accounting-Output-Octets.
	 * 
	 * @return The octets.
	 */
	public Long getAccountingOutputOctets() {
		return _accountingOutputOctets;
	}

	/**
	 * Sets the Accounting-Output-Packets.
	 * 
	 * @param octets The octets.
	 */
	public void setAccountingOutputPackets(Long octets) {
		_accountingOutputPackets = octets;
	}

	/**
	 * Gets the Accounting-Output-Packets.
	 * 
	 * @return The octets.
	 */
	public Long getAccountingOutputPackets() {
		return _accountingOutputPackets;
	}

	/**
	 * Sets the Change-Condition.
	 * 
	 * @param condition The condition.
	 */
	public void setChangeCondition(Integer condition) {
		_changeCondition = condition;
	}

	/**
	 * Gets the Change-Condition.
	 * 
	 * @return The condition.
	 */
	public Integer getChangeCondition() {
		return _changeCondition;
	}

	/**
	 * Sets the Change-Time.
	 * 
	 * @param time The time.
	 */
	public void setChangeTime(Date time) {
		if (time == null) {
			_changeTime = null;
		} else {
			_changeTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the Change-Time.
	 * 
	 * @return The time.
	 */
	public Date getChangeTime() {
		if (_changeTime == null) {
			return null;
		}
		return (Date) _changeTime.clone();
	}

	/**
	 * Sets the 3GPP-User-Location-Info.
	 * 
	 * @param info The info.
	 */
	public void set3gppUserLocationInfo(byte[] info) {
		_3gppUserLocationInfo = copyArray(info);
	}

	/**
	 * Gets the 3GPP-User-Location-Info.
	 * 
	 * @return The info.
	 */
	public byte[] get3gppUserLocationInfo() {
		return copyArray(_3gppUserLocationInfo);
	}

}
