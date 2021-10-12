// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.Address;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * The Service-Data-Container AVP wrapper.
 */
public class ServiceDataContainer {

	private AfCorrelationInformation _afCorrelationInformation = null;
	private String _chargingRuleBaseName = null;
	private Long _accountingInputOctets = null;
	private Long _accountingInputPackets = null;
	private Long _accountingOutputOctets = null;
	private Long _accountingOutputPackets = null;
	private Long _localSequenceNumber = null;
	private QosInformation _qosInformation = null;
	private Long _ratingGroup = null;
	private Date _changeTime = null;
	private Long _serviceIdentifier = null;
	private ServiceSpecificInfo _serviceSpecificInfo = null;
	private Address _sgsnAddress = null;
	private Date _timeFirstUsage = null;
	private Date _timeLastUsage = null;
	private Long _timeUsage = null;
	private Integer _changeCondition = null;
	private byte[] _3gppUserLocationInfo = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public ServiceDataContainer(byte[] data, Version version)
			throws DiameterMissingAVPException {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getAfCorrelationInformationAVP(version);
		DiameterAVP searchedAvp;

		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAfCorrelationInformation(new AfCorrelationInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getChargingRuleBaseNameAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChargingRuleBaseName(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAccountingInputOctetsAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
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

		def = ChargingUtils.getLocalSequenceNumberAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLocalSequenceNumber(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getQosInformationAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setQosInformation(new QosInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getRatingGroupAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setRatingGroup(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getChangeTimeAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChangeTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getServiceIdentifierAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setServiceIdentifier(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getServiceSpecificInfoAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceSpecificInfo(new ServiceSpecificInfo(searchedAvp, version));
			}
		}

		def = ChargingUtils.getSgsnAddressAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSgsnAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getTimeFirstUsageAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTimeFirstUsage(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTimeLastUsageAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTimeLastUsage(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTimeUsageAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTimeUsage(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getChangeConditionAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChangeCondition(Integer.valueOf(Integer32Format.getInteger32(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getServiceDataContainerAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getAfCorrelationInformation() != null) {
			DiameterAVP avp = getAfCorrelationInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getChargingRuleBaseName() != null) {
			def = ChargingUtils.getChargingRuleBaseNameAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getChargingRuleBaseName()), false);
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

		if (getLocalSequenceNumber() != null) {
			def = ChargingUtils.getLocalSequenceNumberAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getLocalSequenceNumber()), false);
				l.add(avp);
			}
		}

		if (getQosInformation() != null) {
			DiameterAVP avp = getQosInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getRatingGroup() != null) {
			def = ChargingUtils.getRatingGroupAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getRatingGroup()), false);
			l.add(avp);
		}

		if (getChangeTime() != null) {
			def = ChargingUtils.getChangeTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getChangeTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getServiceIdentifier() != null) {
			def = ChargingUtils.getServiceIdentifierAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getServiceIdentifier()), false);
			l.add(avp);
		}

		if (getServiceSpecificInfo() != null) {
			DiameterAVP avp = getServiceSpecificInfo().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getSgsnAddress() != null) {
			def = ChargingUtils.getSgsnAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSgsnAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getTimeFirstUsage() != null) {
			def = ChargingUtils.getTimeFirstUsageAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getTimeFirstUsage().getTime()), false);
				l.add(avp);
			}
		}

		if (getTimeLastUsage() != null) {
			def = ChargingUtils.getTimeLastUsageAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getTimeLastUsage().getTime()), false);
				l.add(avp);
			}
		}

		if (getTimeUsage() != null) {
			def = ChargingUtils.getTimeUsageAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getTimeUsage()), false);
				l.add(avp);
			}
		}

		if (getChangeCondition() != null) {
			def = ChargingUtils.getChangeConditionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Integer32Format.toInteger32(getChangeCondition()), false);
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

	/**
	 * Sets the AF-Correlation-Information.
	 * 
	 * @param information The information.
	 */
	public void setAfCorrelationInformation(AfCorrelationInformation information) {
		_afCorrelationInformation = information;
	}

	/**
	 * Gets the AF-Correlation-Information.
	 * 
	 * @return The information.
	 */
	public AfCorrelationInformation getAfCorrelationInformation() {
		return _afCorrelationInformation;
	}

	/**
	 * Sets the Charging-Rule-Base-Name.
	 * 
	 * @param name The name.
	 */
	public void setChargingRuleBaseName(String name) {
		_chargingRuleBaseName = name;
	}

	/**
	 * Gets the Charging-Rule-Base-Name.
	 * 
	 * @return The name.
	 */
	public String getChargingRuleBaseName() {
		return _chargingRuleBaseName;
	}

	/**
	 * Sets the Local-Sequence-Number.
	 * 
	 * @param number The number.
	 */
	public void setLocalSequenceNumber(Long number) {
		_localSequenceNumber = number;
	}

	/**
	 * Gets the Local-Sequence-Number.
	 * 
	 * @return The number.
	 */
	public Long getLocalSequenceNumber() {
		return _localSequenceNumber;
	}

	/**
	 * Sets the Rating-Group.
	 * 
	 * @param group The group.
	 */
	public void setRatingGroup(Long group) {
		_ratingGroup = group;
	}

	/**
	 * Gets the Rating-Group.
	 * 
	 * @return The group.
	 */
	public Long getRatingGroup() {
		return _ratingGroup;
	}

	/**
	 * Sets the Service-Identifier.
	 * 
	 * @param identifier The identifier.
	 */
	public void setServiceIdentifier(Long identifier) {
		_serviceIdentifier = identifier;
	}

	/**
	 * Gets the Service-Identifier.
	 * 
	 * @return The identifier.
	 */
	public Long getServiceIdentifier() {
		return _serviceIdentifier;
	}

	/**
	 * Sets the Service-Specific-Info.
	 * 
	 * @param info The info.
	 */
	public void setServiceSpecificInfo(ServiceSpecificInfo info) {
		_serviceSpecificInfo = info;
	}

	/**
	 * Gets the Service-Specific-Info.
	 * 
	 * @return The info.
	 */
	public ServiceSpecificInfo getServiceSpecificInfo() {
		return _serviceSpecificInfo;
	}

	/**
	 * Sets the SGSN-Address.
	 * 
	 * @param address The address.
	 */
	public void setSgsnAddress(Address address) {
		_sgsnAddress = address;
	}

	/**
	 * Gets the SGSN-Address.
	 * 
	 * @return The address.
	 */
	public Address getSgsnAddress() {
		return _sgsnAddress;
	}

	/**
	 * Sets the Time-First-Usage.
	 * 
	 * @param time The time.
	 */
	public void setTimeFirstUsage(Date time) {
		if (time == null) {
			_timeFirstUsage = null;
		} else {
			_timeFirstUsage = (Date) time.clone();
		}
	}

	/**
	 * Gets the Time-First-Usage.
	 * 
	 * @return The time.
	 */
	public Date getTimeFirstUsage() {
		if (_timeFirstUsage == null) {
			return null;
		}
		return (Date) _timeFirstUsage.clone();
	}

	/**
	 * Sets the Time-Last-Usage.
	 * 
	 * @param time The time.
	 */
	public void setTimeLastUsage(Date time) {
		if (time == null) {
			_timeLastUsage = null;
		} else {
			_timeLastUsage = (Date) time.clone();
		}
	}

	/**
	 * Gets the Time-Last-Usage.
	 * 
	 * @return The time.
	 */
	public Date getTimeLastUsage() {
		if (_timeLastUsage == null) {
			return null;
		}
		return (Date) _timeLastUsage.clone();
	}

	/**
	 * Sets the Time-Usage.
	 * 
	 * @param usage The usage.
	 */
	public void setTimeUsage(Long usage) {
		_timeUsage = usage;
	}

	/**
	 * Gets the Time-Usage.
	 * 
	 * @return The usage.
	 */
	public Long getTimeUsage() {
		return _timeUsage;
	}

}
