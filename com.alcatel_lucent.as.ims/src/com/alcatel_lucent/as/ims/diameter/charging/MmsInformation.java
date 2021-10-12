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
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.Adaptations;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ContentClass;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.DeliveryReportRequested;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.DrmContent;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MMBoxStorageRequested;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MessageType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.Priority;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ReadReplyReportRequested;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The MMS-Information AVP wrapper.
 */
public class MmsInformation {

	private List<OriginatorAddress> _originatorAddresses = new ArrayList<OriginatorAddress>();
	private List<RecipientAddress> _recipientAddresses = new ArrayList<RecipientAddress>();
	private Date _submissionTime = null;
	private MmContentType _mmContentType = null;
	private Priority _priority = null;
	private String _messageId = null;
	private MessageType _messageType = null;
	private Long _messageSize = null;
	private MessageClass _messageClass = null;
	private DeliveryReportRequested _deliveryReportRequested = null;
	private ReadReplyReportRequested _readReplyReportRequested = null;
	private MMBoxStorageRequested _mmBoxStorageRequested = null;
	private String _applicId = null;
	private String _replyApplicId = null;
	private String _auxApplicInfo = null;
	private ContentClass _contentClass = null;
	private DrmContent _drmContent = null;
	private Adaptations _adaptations = null;
	private String _vaspId = null;
	private String _vasId = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MmsInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getOriginatorAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addOriginatorAddress(new OriginatorAddress(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getRecipientAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addRecipientAddress(new RecipientAddress(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getSubmissionTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSubmissionTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMmContentTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMmContentType(new MmContentType(searchedAvp, version));
			}
		}

		def = ChargingUtils.getPriorityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPriority(Priority.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMessageIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMessageId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getMessageTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMessageType(MessageType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMessageSizeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMessageSize(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMessageClassAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMessageClass(new MessageClass(searchedAvp, version));
			}
		}

		def = ChargingUtils.getDeliveryReportRequestedAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDeliveryReportRequested(DeliveryReportRequested.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getReadReplyReportRequestedAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setReadReplyReportRequested(ReadReplyReportRequested.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMmboxStorageRequestedAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMmBoxStorageRequested(MMBoxStorageRequested.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getReplyApplicIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setReplyApplicId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAuxApplicInfoAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAuxApplicInfo(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getContentClassAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setContentClass(ContentClass.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getDrmContentAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDrmContent(DrmContent.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getAdaptationsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAdaptations(Adaptations.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getVaspIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setVaspId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getVasIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setVasId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getMmsInformationAVP(version);
		if (def == null) {
			return null;
		}
		
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		Iterable<OriginatorAddress> oAdresses = getOriginatorAddresses();
		if (oAdresses.iterator().hasNext()) {
			def = ChargingUtils.getOriginatorAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (OriginatorAddress address : oAdresses) {
					avp.addValue(address.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		Iterable<RecipientAddress> rAddresses = getRecipientAddresses();
		if (oAdresses.iterator().hasNext()) {
			def = ChargingUtils.getRecipientAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (RecipientAddress address : rAddresses) {
					avp.addValue(address.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		if (getSubmissionTime() != null) {
			def = ChargingUtils.getSubmissionTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getSubmissionTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getMmContentType() != null) {
			DiameterAVP avp = getMmContentType().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getPriority() != null) {
			def = ChargingUtils.getPriorityAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPriority().getValue()), false);
				l.add(avp);
			}
		}

		if (getMessageId() != null) {
			def = ChargingUtils.getMessageIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getMessageId()), false);
				l.add(avp);
			}
		}

		if (getMessageType() != null) {
			def = ChargingUtils.getMessageTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPriority().getValue()), false);
				l.add(avp);
			}
		}

		if (getMessageSize() != null) {
			def = ChargingUtils.getMessageSizeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getMessageSize()), false);
				l.add(avp);
			}
		}

		if (getMessageClass() != null) {
			DiameterAVP avp = getMessageClass().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getDeliveryReportRequested() != null) {
			def = ChargingUtils.getDeliveryReportRequestedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getDeliveryReportRequested().getValue()), false);
				l.add(avp);
			}
		}

		if (getReadReplyReportRequested() != null) {
			def = ChargingUtils.getReadReplyReportRequestedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getReadReplyReportRequested().getValue()), false);
				l.add(avp);
			}
		}

		if (getMmBoxStorageRequested() != null) {
			def = ChargingUtils.getMmboxStorageRequestedAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getMmBoxStorageRequested().getValue()), false);
				l.add(avp);
			}
		}

		if (getApplicId() != null) {
			def = ChargingUtils.getApplicIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getApplicId()), false);
				l.add(avp);
			}
		}

		if (getReplyApplicId() != null) {
			def = ChargingUtils.getReplyApplicIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getReplyApplicId()), false);
				l.add(avp);
			}
		}

		if (getAuxApplicInfo() != null) {
			def = ChargingUtils.getAuxApplicInfoAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getAuxApplicInfo()), false);
				l.add(avp);
			}
		}

		if (getContentClass() != null) {
			def = ChargingUtils.getContentClassAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getContentClass().getValue()), false);
				l.add(avp);
			}
		}

		if (getDrmContent() != null) {
			def = ChargingUtils.getDrmContentAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getDrmContent().getValue()), false);
				l.add(avp);
			}
		}

		if (getAdaptations() != null) {
			def = ChargingUtils.getAdaptationsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getAdaptations().getValue()), false);
				l.add(avp);
			}
		}

		if (getVaspId() != null) {
			def = ChargingUtils.getVaspIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getVaspId()), false);
				l.add(avp);
			}
		}

		if (getVasId() != null) {
			def = ChargingUtils.getVasIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getVasId()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Adds an Originator-Address.
	 * 
	 * @param address The address.
	 */
	public void addOriginatorAddress(OriginatorAddress address) {
		if (address != null) {
			_originatorAddresses.add(address);
		}
	}

	/**
	 * Gets the Originator-Address list.
	 * 
	 * @return The addresses.
	 */
	public Iterable<OriginatorAddress> getOriginatorAddresses() {
		return _originatorAddresses;
	}

	/**
	 * Adds a Recipient-Address.
	 * 
	 * @param address The address.
	 */
	public void addRecipientAddress(RecipientAddress address) {
		if (address != null) {
			_recipientAddresses.add(address);
		}
	}

	/**
	 * Gets the Recipient-Address list.
	 * 
	 * @return The address.
	 */
	public Iterable<RecipientAddress> getRecipientAddresses() {
		return _recipientAddresses;
	}

	/**
	 * Sets the Submission-Time.
	 * 
	 * @param time The time.
	 */
	public void setSubmissionTime(Date time) {
		if (time == null) {
			_submissionTime = null;
		} else {
			_submissionTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the Submission-Time.
	 * 
	 * @return The time.
	 */
	public Date getSubmissionTime() {
		if (_submissionTime == null) {
			return null;
		}
		return (Date) _submissionTime.clone();
	}

	/**
	 * Sets the MM-Content-Type.
	 * 
	 * @param type The type.
	 */
	public void setMmContentType(MmContentType type) {
		_mmContentType = type;
	}

	/**
	 * Gets the MM-Content-Type.
	 * 
	 * @return The type.
	 */
	public MmContentType getMmContentType() {
		return _mmContentType;
	}

	/**
	 * Sets the Priority.
	 * 
	 * @param priority The priority.
	 */
	public void setPriority(Priority priority) {
		_priority = priority;
	}

	/**
	 * Gets the Priority.
	 * 
	 * @return The priority.
	 */
	public Priority getPriority() {
		return _priority;
	}

	/**
	 * Sets the Message-ID.
	 * 
	 * @param id The Message-ID.
	 */
	public void setMessageId(String id) {
		_messageId = id;
	}

	/**
	 * Gets the Message-ID.
	 * 
	 * @return The Message-ID.
	 */
	public String getMessageId() {
		return _messageId;
	}

	/**
	 * Sets the Message-Type.
	 * 
	 * @param type The type.
	 */
	public void setMessageType(MessageType type) {
		_messageType = type;
	}

	/**
	 * Gets the Message-Type.
	 * 
	 * @return The type.
	 */
	public MessageType getMessageType() {
		return _messageType;
	}

	/**
	 * Sets the Message-Size.
	 * 
	 * @param size The size.
	 */
	public void setMessageSize(Long size) {
		_messageSize = size;
	}

	/**
	 * Gets the Message-Size.
	 * 
	 * @return The size.
	 */
	public Long getMessageSize() {
		return _messageSize;
	}

	/**
	 * Sets the Message-Class.
	 * 
	 * @param messageClass The message class.
	 */
	public void setMessageClass(MessageClass messageClass) {
		_messageClass = messageClass;
	}

	/**
	 * Gets the Message-Class.
	 * 
	 * @return The message class.
	 */
	public MessageClass getMessageClass() {
		return _messageClass;
	}

	/**
	 * Sets the Delivery-Report-Requested.
	 * 
	 * @param status The status.
	 */
	public void setDeliveryReportRequested(DeliveryReportRequested status) {
		_deliveryReportRequested = status;
	}

	/**
	 * Gets the Delivery-Report-Requested.
	 * 
	 * @return The status.
	 */
	public DeliveryReportRequested getDeliveryReportRequested() {
		return _deliveryReportRequested;
	}

	/**
	 * Sets the Read-Reply-Report-Requested.
	 * 
	 * @param status The status.
	 */
	public void setReadReplyReportRequested(ReadReplyReportRequested status) {
		_readReplyReportRequested = status;
	}

	/**
	 * Gets the Read-Reply-Report-Requested.
	 * 
	 * @return The status.
	 */
	public ReadReplyReportRequested getReadReplyReportRequested() {
		return _readReplyReportRequested;
	}

	/**
	 * Sets the MMBox-Storage-Requested.
	 * 
	 * @param status The status.
	 */
	public void setMmBoxStorageRequested(MMBoxStorageRequested status) {
		_mmBoxStorageRequested = status;
	}

	/**
	 * Gets the MMBox-Storage-Requested.
	 * 
	 * @return The status.
	 */
	public MMBoxStorageRequested getMmBoxStorageRequested() {
		return _mmBoxStorageRequested;
	}

	/**
	 * Sets the Applic-ID.
	 * 
	 * @param id The id.
	 */
	public void setApplicId(String id) {
		_applicId = id;
	}

	/**
	 * Gets the Applic-ID.
	 * 
	 * @return The id.
	 */
	public String getApplicId() {
		return _applicId;
	}

	/**
	 * Sets the Reply-Applic-ID.
	 * 
	 * @param id The id.
	 */
	public void setReplyApplicId(String id) {
		_replyApplicId = id;
	}

	/**
	 * Gets the Reply-Applic-ID.
	 * 
	 * @return The id.
	 */
	public String getReplyApplicId() {
		return _replyApplicId;
	}

	/**
	 * Sets the Content-Class.
	 * 
	 * @param contentClass The content class.
	 */
	public void setContentClass(ContentClass contentClass) {
		_contentClass = contentClass;
	}

	/**
	 * Gets the Content-Class.
	 * 
	 * @return The content class.
	 */
	public ContentClass getContentClass() {
		return _contentClass;
	}

	/**
	 * Sets the DRM-Content.
	 * 
	 * @param content The content.
	 */
	public void setDrmContent(DrmContent content) {
		_drmContent = content;
	}

	/**
	 * Gets the DRM-Content.
	 * 
	 * @return The content.
	 */
	public DrmContent getDrmContent() {
		return _drmContent;
	}

	/**
	 * Sets the Adaptations.
	 * 
	 * @param adaptations The adaptations.
	 */
	public void setAdaptations(Adaptations adaptations) {
		_adaptations = adaptations;
	}

	/**
	 * Gets the Adaptations.
	 * 
	 * @return The adaptations.
	 */
	public Adaptations getAdaptations() {
		return _adaptations;
	}

	/**
	 * Sets the VASP-Id.
	 * 
	 * @param id The id.
	 */
	public void setVaspId(String id) {
		_vaspId = id;
	}

	/**
	 * Gets the VASP-Id.
	 * 
	 * @return The id.
	 */
	public String getVaspId() {
		return _vaspId;
	}

	/**
	 * Sets the VAS-Id.
	 * 
	 * @param id The id.
	 */
	public void setVasId(String id) {
		_vasId = id;
	}

	/**
	 * Gets the VAS-Id.
	 * 
	 * @return The id.
	 */
	public String getVasId() {
		return _vasId;
	}

	/**
	 * Sets the Aux-Applic-Info.
	 * 
	 * @param info The info.
	 */
	public void setAuxApplicInfo(String info) {
		_auxApplicInfo = info;
	}

	/**
	 * Gets the Aux-Applic-Info.
	 * 
	 * @return The info.
	 */
	public String getAuxApplicInfo() {
		return _auxApplicInfo;
	}

}
