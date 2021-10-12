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
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ReplyPathRequested;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.SmMessageType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.SmServiceType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.SmsNode;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The SMS-Information AVP wrapper.
 */
public class SmsInformation {

	private SmsNode _smsNode = null;
	private Address _clientAddress = null;
	private Address _originatorSccpAddress = null;
	private Address _smscAddress = null;
	private Integer _dataCodingScheme = null;
	private Date _smDischargeTime = null;
	private SmMessageType _smMessageType = null;
	private OriginatorInterface _originatorInterface = null;
	private byte[] _smProtocolId = null;
	private ReplyPathRequested _replyPathRequested = null;
	private byte[] _smStatus = null;
	private byte[] _smUserDataHeader = null;
	private Long _numberOfMessagesSent = null;
	private List<RecipientInfo> _recipientinfos = new ArrayList<RecipientInfo>();
	private OriginatorReceivedAddress _originatorReceivedAddress = null;
	private SmServiceType _smServiceType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public SmsInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getSmsNodeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmsNode(SmsNode.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getClientAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setClientAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getOriginatingSccpAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOriginatorSccpAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getSmscAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmscAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getDataCodingSchemeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDataCodingScheme(Integer.valueOf(Integer32Format.getInteger32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getSmDischargeTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmDischargeTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getSmMessageTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmMessageType(SmMessageType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getOriginatorInterfaceAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOriginatorInterface(new OriginatorInterface(searchedAvp, version));
			}
		}

		def = ChargingUtils.getSmProtocolIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmProtocolId(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getReplyPathRequestedAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setReplyPathRequested(ReplyPathRequested.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getSmStatusAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmStatus(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getSmUserDataHeaderAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmUserDataHeader(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getNumberOfMessagesSentAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfMessagesSent(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getRecipientInfoAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addRecipientinfo(new RecipientInfo(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getOriginatorReceivedAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOriginatorReceivedAddress(new OriginatorReceivedAddress(searchedAvp, version));
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
		DiameterAVPDefinition def = ChargingUtils.getSmsInformationAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getSmsNode() != null) {
			def = ChargingUtils.getSmsNodeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getSmsNode().getValue()), false);
				l.add(avp);
			}
		}

		if (getClientAddress() != null) {
			def = ChargingUtils.getClientAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getClientAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getOriginatorSccpAddress() != null) {
			def = ChargingUtils.getOriginatingSccpAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getOriginatorSccpAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getSmscAddress() != null) {
			def = ChargingUtils.getSmscAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSmscAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getDataCodingScheme() != null) {
			def = ChargingUtils.getDataCodingSchemeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Integer32Format.toInteger32(getDataCodingScheme()), false);
				l.add(avp);
			}
		}

		if (getSmDischargeTime() != null) {
			def = ChargingUtils.getSmDischargeTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getSmDischargeTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getSmMessageType() != null) {
			def = ChargingUtils.getSmMessageTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getSmMessageType().getValue()), false);
				l.add(avp);
			}
		}

		if (getOriginatorInterface() != null) {
			def = ChargingUtils.getOriginatorInterfaceAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getOriginatorInterface().toAvp(version).getValue(), false);
				l.add(avp);
			}
		}

		if (getSmProtocolId() != null) {
			def = ChargingUtils.getSmProtocolIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSmProtocolId(), false);
				l.add(avp);
			}
		}

		if (getReplyPathRequested() != null) {
			def = ChargingUtils.getReplyPathRequestedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getReplyPathRequested().getValue()), false);
				l.add(avp);
			}
		}

		if (getSmStatus() != null) {
			def = ChargingUtils.getSmStatusAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSmStatus(), false);
				l.add(avp);
			}
		}

		if (getSmUserDataHeader() != null) {
			def = ChargingUtils.getSmUserDataHeaderAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSmUserDataHeader(), false);
				l.add(avp);
			}
		}

		if (getNumberOfMessagesSent() != null) {
			def = ChargingUtils.getNumberOfMessagesSentAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getNumberOfMessagesSent()), false);
				l.add(avp);
			}
		}

		Iterable<RecipientInfo> infos = getRecipientinfos();
		if (infos.iterator().hasNext()) {
			DiameterAVP avp = new DiameterAVP(def);
			for (RecipientInfo info : infos) {
				DiameterAVP infoAvp = info.toAvp(version);
				if (infoAvp != null) {
					avp.addValue(infoAvp.getValue(), false);
				}
			}
			if (avp.getValueSize() > 0) {
				l.add(avp);
			}
		}

		if (getOriginatorReceivedAddress() != null) {
			def = ChargingUtils.getOriginatorReceivedAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.addValue(getOriginatorReceivedAddress().toAvp(version).getValue(), false);
				l.add(avp);
			}
		}

		if (getSmServiceType() != null) {
			def = ChargingUtils.getSmServiceTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getSmServiceType().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the SMS-Node.
	 * 
	 * @param node node.
	 */
	public void setSmsNode(SmsNode node) {
		_smsNode = node;
	}

	/**
	 * Gets the SMS-Node.
	 * 
	 * @return The node.
	 */
	public SmsNode getSmsNode() {
		return _smsNode;
	}

	/**
	 * Sets the Client-Address.
	 * 
	 * @param address The address.
	 */
	public void setClientAddress(Address address) {
		_clientAddress = address;
	}

	/**
	 * Gets the Client-Address.
	 * 
	 * @return The address.
	 */
	public Address getClientAddress() {
		return _clientAddress;
	}

	/**
	 * Sets the Originator-SCCP-Address.
	 * 
	 * @param address The address.
	 */
	public void setOriginatorSccpAddress(Address address) {
		_originatorSccpAddress = address;
	}

	/**
	 * Gets the Originator-SCCP-Address.
	 * 
	 * @return The address.
	 */
	public Address getOriginatorSccpAddress() {
		return _originatorSccpAddress;
	}

	/**
	 * Sets the SMSC-Address.
	 * 
	 * @param address The address.
	 */
	public void setSmscAddress(Address address) {
		_smscAddress = address;
	}

	/**
	 * Gets the SMSC-Address.
	 * 
	 * @return The address.
	 */
	public Address getSmscAddress() {
		return _smscAddress;
	}

	/**
	 * Sets the Data-Coding-Scheme.
	 * 
	 * @param scheme The scheme.
	 */
	public void setDataCodingScheme(Integer scheme) {
		_dataCodingScheme = scheme;
	}

	/**
	 * Gets the Data-Coding-Scheme.
	 * 
	 * @return The scheme.
	 */
	public Integer getDataCodingScheme() {
		return _dataCodingScheme;
	}

	/**
	 * Sets the SM-Discharge-Time.
	 * 
	 * @param time The time.
	 */
	public void setSmDischargeTime(Date time) {
		if (time == null) {
			_smDischargeTime = null;
		} else {
			_smDischargeTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the SM-Discharge-Time.
	 * 
	 * @return The time.
	 */
	public Date getSmDischargeTime() {
		if (_smDischargeTime == null) {
			return null;
		}
		return (Date) _smDischargeTime.clone();
	}

	/**
	 * Sets the SM-Message-Type.
	 * 
	 * @param type The type.
	 */
	public void setSmMessageType(SmMessageType type) {
		_smMessageType = type;
	}

	/**
	 * Gets the SM-Message-Type.
	 * 
	 * @return The type.
	 */
	public SmMessageType getSmMessageType() {
		return _smMessageType;
	}

	/**
	 * Sets the Originator-Interface.
	 * 
	 * @param information The information.
	 */
	public void setOriginatorInterface(OriginatorInterface information) {
		_originatorInterface = information;
	}

	/**
	 * Gets the Originator-Interface.
	 * 
	 * @return The information.
	 */
	public OriginatorInterface getOriginatorInterface() {
		return _originatorInterface;
	}

	/**
	 * Sets the SM-Protocol-ID.
	 * 
	 * @param id The id.
	 */
	public void setSmProtocolId(byte[] id) {
		_smProtocolId = ChargingUtils.copyArray(id);
	}

	/**
	 * Gets the SM-Protocol-ID.
	 * 
	 * @return The id.
	 */
	public byte[] getSmProtocolId() {
		return ChargingUtils.copyArray(_smProtocolId);
	}

	/**
	 * Sets the Reply-Path-Requested.
	 * 
	 * @param path The path.
	 */
	public void setReplyPathRequested(ReplyPathRequested path) {
		_replyPathRequested = path;
	}

	/**
	 * Gets the Reply-Path-Requested.
	 * 
	 * @return The path.
	 */
	public ReplyPathRequested getReplyPathRequested() {
		return _replyPathRequested;
	}

	/**
	 * Sets the SM-Status.
	 * 
	 * @param status The status.
	 */
	public void setSmStatus(byte[] status) {
		_smStatus = ChargingUtils.copyArray(status);
	}

	/**
	 * Gets the SM-Status.
	 * 
	 * @return The status.
	 */
	public byte[] getSmStatus() {
		return ChargingUtils.copyArray(_smStatus);
	}

	/**
	 * Sets the SM-User-Data-Header.
	 * 
	 * @param header The header.
	 */
	public void setSmUserDataHeader(byte[] header) {
		_smUserDataHeader = ChargingUtils.copyArray(header);
	}

	/**
	 * Gets the SM-User-Data-Header.
	 * 
	 * @return The header.
	 */
	public byte[] getSmUserDataHeader() {
		return ChargingUtils.copyArray(_smUserDataHeader);
	}

	/**
	 * Sets the Number-Of-Messages-Sent.
	 * 
	 * @param number The number.
	 */
	public void setNumberOfMessagesSent(Long number) {
		_numberOfMessagesSent = number;
	}

	/**
	 * Gets the Number-Of-Messages-Sent.
	 * 
	 * @return The number.
	 */
	public Long getNumberOfMessagesSent() {
		return _numberOfMessagesSent;
	}

	/**
	 * Adds an Recipient-info.
	 * 
	 * @param info The info.
	 */
	public void addRecipientinfo(RecipientInfo info) {
		if (info != null) {
			_recipientinfos.add(info);
		}
	}

	/**
	 * Gets the Recipient-info list.
	 * 
	 * @return The infos.
	 */
	public Iterable<RecipientInfo> getRecipientinfos() {
		return _recipientinfos;
	}

	/**
	 * Sets the Originator-Received-Address.
	 * 
	 * @param address The address.
	 */
	public void setOriginatorReceivedAddress(OriginatorReceivedAddress address) {
		_originatorReceivedAddress = address;
	}

	/**
	 * Gets the Originator-Received-Address.
	 * 
	 * @return The address.
	 */
	public OriginatorReceivedAddress getOriginatorReceivedAddress() {
		return _originatorReceivedAddress;
	}

	/**
	 * Sets the SM-Service-Type.
	 * 
	 * @param type The type.
	 */
	public void setSmServiceType(SmServiceType type) {
		_smServiceType = type;
	}

	/**
	 * Gets the SM-Service-Type.
	 * 
	 * @return The type.
	 */
	public SmServiceType getSmServiceType() {
		return _smServiceType;
	}

}
