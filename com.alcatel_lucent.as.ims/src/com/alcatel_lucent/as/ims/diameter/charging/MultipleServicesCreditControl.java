// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.EnvelopeReporting;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ReportingReason;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Multiple-Services-Credit-Control AVP wrapper.
 */
public class MultipleServicesCreditControl {

	private GrantedServiceUnit _grantedServiceUnit = null;
	private RequestedServiceUnit _requestedServiceUnit = null;
	private List<UsedServiceUnit> _usedServiceUnits = new ArrayList<UsedServiceUnit>();
	private List<Long> _serviceIdentifiers = new ArrayList<Long>();
	private Long _ratingGroup = null;
	private List<GsuPoolReference> _gsuPoolReferences = new ArrayList<GsuPoolReference>();
	private Long _validityTime = null;
	private Long _resultCode = null;
	private FinalUnitIndication _finalUnitIndication = null;
	private Long _timeQuotaThreshold = null;
	private Long _volumeQuotaThreshold = null;
	private Long _unitQuotaThreshold = null;
	private Long _quotaHoldingTime = null;
	private Long _quotaConsumptionTime = null;
	private List<ReportingReason> _reportingReasons = new ArrayList<ReportingReason>();
	private Trigger _trigger = null;
	private PsFurnishChargingInformation _psFurnishChargingInformation = null;
	private byte[] _refundInformation = null;
	private List<AfCorrelationInformation> _afCorrelationInformations = new ArrayList<AfCorrelationInformation>();
	private List<Envelope> _envelopes = new ArrayList<Envelope>();
	private EnvelopeReporting _envelopeReporting = null;
	private TimeQuotaMechanism _timeQuotaMechanism = null;
	private List<ServiceSpecificInfo> _serviceSpecificInfo = new ArrayList<ServiceSpecificInfo>();

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public MultipleServicesCreditControl(byte[] data, Version version)
			throws DiameterMissingAVPException {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getGrantedServiceUnitAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setGrantedServiceUnit(new GrantedServiceUnit(searchedAvp, version));
		}

		def = ChargingUtils.getRequestedServiceUnitAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setRequestedServiceUnit(new RequestedServiceUnit(searchedAvp, version));
		}

		def = ChargingUtils.getUsedServiceUnitAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			for (int i = 0; i < searchedAvp.getValueSize(); i++) {
				addUsedServiceUnit(new UsedServiceUnit(searchedAvp.getValue(i), version));
			}
		}

		def = ChargingUtils.getServiceIdentifierAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			for (int i = 0; i < searchedAvp.getValueSize(); i++) {
				addServiceIdentifier(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(i), 0)));
			}
		}

		def = ChargingUtils.getRatingGroupAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setRatingGroup(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getGsuPoolReferenceAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			for (int i = 0; i < searchedAvp.getValueSize(); i++) {
				addGsuPoolReference(new GsuPoolReference(searchedAvp.getValue(i)));
			}
		}

		def = ChargingUtils.getValidityTimeAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setValidityTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = DiameterBaseConstants.AVP_RESULT_CODE;
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setResultCode(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setFinalUnitIndication(new FinalUnitIndication(searchedAvp));
		}

		def = ChargingUtils.getTimeQuotaThresholdAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTimeQuotaThreshold(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getVolumeQuotaThresholdAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setVolumeQuotaThreshold(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getUnitQuotaThresholdAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setUnitQuotaThreshold(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getQuotaHoldingTimeAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setQuotaHoldingTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getQuotaConsumptionTimeAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setQuotaConsumptionTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getReportingReasonAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addReportingReason(ReportingReason.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(i), 0)));
				}
			}
		}

		def = ChargingUtils.getTriggerAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTrigger(new Trigger(searchedAvp, version));
			}
		}

		def = ChargingUtils.getPsFurnishChargingInformationAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPsFurnishChargingInformation(new PsFurnishChargingInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getRefundInformationAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setRefundInformation(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getAfCorrelationInformationAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addAfCorrelationInformation(new AfCorrelationInformation(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getEnvelopeAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addEnvelope(new Envelope(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getEnvelopeReportingAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setEnvelopeReporting(EnvelopeReporting.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTimeQuotaMechanismAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTimeQuotaMechanism(new TimeQuotaMechanism(searchedAvp, version));
			}
		}

		def = ChargingUtils.getServiceSpecificInfoAVP(version);
		if (def != null) {
			searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addServiceSpecificInfo(new ServiceSpecificInfo(searchedAvp.getValue(i), version));
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
		DiameterAVPDefinition def = ChargingUtils.getMultipleServiceCreditControlAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getGrantedServiceUnit() != null) {
			DiameterAVP avp = getGrantedServiceUnit().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getRequestedServiceUnit() != null) {
			DiameterAVP avp = getRequestedServiceUnit().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		Iterable<UsedServiceUnit> units = getUsedServiceUnits();
		if (units.iterator().hasNext()) {
			def = ChargingUtils.getUsedServiceUnitAVP();
			DiameterAVP avp = new DiameterAVP(def);
			for (UsedServiceUnit unit : units) {
				avp.addValue(unit.toAvp(version).getValue(), false);
			}
			l.add(avp);
		}

		Iterable<Long> ids = getServiceIdentifiers();
		if (ids.iterator().hasNext()) {
			def = ChargingUtils.getServiceIdentifierAVP();
			DiameterAVP avp = new DiameterAVP(def);
			for (Long id : ids) {
				avp.addValue(Unsigned32Format.toUnsigned32(id), false);
			}
			l.add(avp);
		}

		if (getRatingGroup() != null) {
			def = ChargingUtils.getRatingGroupAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getRatingGroup()), false);
			l.add(avp);
		}

		Iterable<GsuPoolReference> references = getGsuPoolReferences();
		if (references.iterator().hasNext()) {
			def = ChargingUtils.getGsuPoolReferenceAVP();
			DiameterAVP avp = new DiameterAVP(def);
			for (GsuPoolReference ref : references) {
				avp.addValue(ref.toAvp().getValue(), false);
			}
			l.add(avp);
		}

		if (getValidityTime() != null) {
			def = ChargingUtils.getValidityTimeAVP();
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getValidityTime()), false);
			l.add(avp);
		}

		if (getResultCode() != null) {
			def = DiameterBaseConstants.AVP_RESULT_CODE;
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getResultCode()), false);
			l.add(avp);
		}

		if (getFinalUnitIndication() != null) {
			DiameterAVP avp = getFinalUnitIndication().toAvp();
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getTimeQuotaThreshold() != null) {
			def = ChargingUtils.getTimeQuotaThresholdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getTimeQuotaThreshold()), false);
				l.add(avp);
			}
		}

		if (getVolumeQuotaThreshold() != null) {
			def = ChargingUtils.getVolumeQuotaThresholdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getVolumeQuotaThreshold()), false);
				l.add(avp);
			}
		}

		if (getUnitQuotaThreshold() != null) {
			def = ChargingUtils.getUnitQuotaThresholdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getUnitQuotaThreshold()), false);
				l.add(avp);
			}
		}

		if (getQuotaHoldingTime() != null) {
			def = ChargingUtils.getQuotaHoldingTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getQuotaHoldingTime()), false);
				l.add(avp);
			}
		}

		if (getQuotaConsumptionTime() != null) {
			def = ChargingUtils.getQuotaConsumptionTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getQuotaConsumptionTime()), false);
				l.add(avp);
			}
		}

		Iterable<ReportingReason> reasons = getReportingReasons();
		if (reasons.iterator().hasNext()) {
			def = ChargingUtils.getReportingReasonAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (ReportingReason reason : reasons) {
					avp.addValue(EnumeratedFormat.toEnumerated(reason.getValue()), false);
				}
				l.add(avp);
			}
		}

		if (getTrigger() != null) {
			DiameterAVP avp = getTrigger().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getPsFurnishChargingInformation() != null) {
			DiameterAVP avp = getPsFurnishChargingInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getRefundInformation() != null) {
			def = ChargingUtils.getRefundInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getRefundInformation(), false);
				l.add(avp);
			}
		}

		Iterable<AfCorrelationInformation> informations = getAfCorrelationInformations();
		if (informations.iterator().hasNext()) {
			def = ChargingUtils.getAfCorrelationInformationAVP(version);
			DiameterAVP avp = new DiameterAVP(def);
			for (AfCorrelationInformation information : informations) {
				avp.addValue(information.toAvp(version).getValue(), false);
			}
			l.add(avp);
		}

		Iterable<Envelope> envelopes = getEnvelopes();
		if (envelopes.iterator().hasNext()) {
			def = ChargingUtils.getEnvelopeAVP(version);
			DiameterAVP avp = new DiameterAVP(def);
			for (Envelope envelope : envelopes) {
				avp.addValue(envelope.toAvp(version).getValue(), false);
			}
			l.add(avp);
		}

		if (getEnvelopeReporting() != null) {
			def = ChargingUtils.getEnvelopeReportingAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getEnvelopeReporting().getValue()), false);
				l.add(avp);
			}
		}

		if (getTimeQuotaMechanism() != null) {
			DiameterAVP avp = getTimeQuotaMechanism().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		Iterable<ServiceSpecificInfo> infos = getServiceSpecificInfos();
		if (infos.iterator().hasNext()) {
			def = ChargingUtils.getServiceSpecificInfoAVP(version);
			DiameterAVP avp = new DiameterAVP(def);
			for (ServiceSpecificInfo info : infos) {
				avp.addValue(info.toAvp(version).getValue(), false);
			}
			l.add(avp);
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
	 * Sets the Granted-Service-Unit.
	 * 
	 * @param unit The unit.
	 */
	public void setGrantedServiceUnit(GrantedServiceUnit unit) {
		_grantedServiceUnit = unit;
	}

	/**
	 * Gets the Granted-Service-Unit.
	 * 
	 * @return The unit.
	 */
	public GrantedServiceUnit getGrantedServiceUnit() {
		return _grantedServiceUnit;
	}

	/**
	 * Sets the Requested-Service-Unit.
	 * 
	 * @param unit The unit.
	 */
	public void setRequestedServiceUnit(RequestedServiceUnit unit) {
		_requestedServiceUnit = unit;
	}

	/**
	 * Gets the Requested-Service-Unit.
	 * 
	 * @return The unit.
	 */
	public RequestedServiceUnit getRequestedServiceUnit() {
		return _requestedServiceUnit;
	}

	/**
	 * Adds an Used-Service-Unit.
	 * 
	 * @param unit The unit to be added.
	 */
	public void addUsedServiceUnit(UsedServiceUnit unit) {
		_usedServiceUnits.add(unit);
	}

	/**
	 * Gets the Used-Service-Unit list.
	 * 
	 * @return The unit list.
	 */
	public Iterable<UsedServiceUnit> getUsedServiceUnits() {
		return _usedServiceUnits;
	}

	/**
	 * Sets a Service-Identifier.
	 * 
	 * @param id The id.
	 */
	public void addServiceIdentifier(Long id) {
		if (id != null) {
			_serviceIdentifiers.add(id);
		}
	}

	/**
	 * Gets the Service-Identifier list.
	 * 
	 * @return The id.
	 */
	public Iterable<Long> getServiceIdentifiers() {
		return _serviceIdentifiers;
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
	 * Adds a G-S-U-Pool-Reference.
	 * 
	 * @param reference The reference to be added.
	 */
	public void addGsuPoolReference(GsuPoolReference reference) {
		if (reference != null) {
			_gsuPoolReferences.add(reference);
		}
	}

	/**
	 * Gets the G-S-U-Pool-Reference list.
	 * 
	 * @return The references.
	 */
	public Iterable<GsuPoolReference> getGsuPoolReferences() {
		return _gsuPoolReferences;
	}

	/**
	 * Sets the Validity-Time.
	 * 
	 * @param time The time.
	 */
	public void setValidityTime(Long time) {
		_validityTime = time;
	}

	/**
	 * Gets the Validity-Time.
	 * 
	 * @return The time.
	 */
	public Long getValidityTime() {
		return _validityTime;
	}

	/**
	 * Sets the Result-Code.
	 * 
	 * @param code The code.
	 */
	public void setResultCode(Long code) {
		_resultCode = code;
	}

	/**
	 * Gets the Result-Code.
	 * 
	 * @return The code.
	 */
	public Long getResultCode() {
		return _resultCode;
	}

	/**
	 * Sets the Final-Unit-Indication.
	 * 
	 * @param indication The indication.
	 */
	public void setFinalUnitIndication(FinalUnitIndication indication) {
		_finalUnitIndication = indication;
	}

	/**
	 * Gets the Final-Unit-Indication.
	 * 
	 * @return The indication.
	 */
	public FinalUnitIndication getFinalUnitIndication() {
		return _finalUnitIndication;
	}

	/**
	 * Sets the Time-Quota-Threshold.
	 * 
	 * @param threshold The threshold.
	 */
	public void setTimeQuotaThreshold(Long threshold) {
		_timeQuotaThreshold = threshold;
	}

	/**
	 * Gets the Time-Quota-Threshold.
	 * 
	 * @return The threshold.
	 */
	public Long getTimeQuotaThreshold() {
		return _timeQuotaThreshold;
	}

	/**
	 * Sets the Volume-Quota-Threshold.
	 * 
	 * @param threshold The threshold.
	 */
	public void setVolumeQuotaThreshold(Long threshold) {
		_volumeQuotaThreshold = threshold;
	}

	/**
	 * Gets the Volume-Quota-Threshold.
	 * 
	 * @return The threshold.
	 */
	public Long getVolumeQuotaThreshold() {
		return _volumeQuotaThreshold;
	}

	/**
	 * Sets the Unit-Quota-Threshold.
	 * 
	 * @param threshold The threshold.
	 */
	public void setUnitQuotaThreshold(Long threshold) {
		this._unitQuotaThreshold = threshold;
	}

	/**
	 * Gets the Unit-Quota-Threshold.
	 * 
	 * @return The threshold.
	 */
	public Long getUnitQuotaThreshold() {
		return _unitQuotaThreshold;
	}

	/**
	 * Sets the Quota-Holding-Time.
	 * 
	 * @param time The time.
	 */
	public void setQuotaHoldingTime(Long time) {
		_quotaHoldingTime = time;
	}

	/**
	 * Gets the Quota-Holding-Time.
	 * 
	 * @return The time.
	 */
	public Long getQuotaHoldingTime() {
		return _quotaHoldingTime;
	}

	/**
	 * Sets the Quota-Consumption-Time.
	 * 
	 * @param time The time.
	 */
	public void setQuotaConsumptionTime(Long time) {
		_quotaConsumptionTime = time;
	}

	/**
	 * Gets the Quota-Consumption-Time.
	 * 
	 * @return The time.
	 */
	public Long getQuotaConsumptionTime() {
		return _quotaConsumptionTime;
	}

	/**
	 * Adds a Reporting-Reason.
	 * 
	 * @param reason The reason to be added.
	 */
	public void addReportingReason(ReportingReason reason) {
		if (reason != null) {
			_reportingReasons.add(reason);
		}
	}

	/**
	 * Gets the Reporting list.
	 * 
	 * @return The reasons.
	 */
	public Iterable<ReportingReason> getReportingReasons() {
		return _reportingReasons;
	}

	/**
	 * Sets the Trigger.
	 * 
	 * @param trigger The trigger.
	 */
	public void setTrigger(Trigger trigger) {
		_trigger = trigger;
	}

	/**
	 * Gets the Trigger.
	 * 
	 * @return The trigger.
	 */
	public Trigger getTrigger() {
		return _trigger;
	}

	/**
	 * Sets the PS-Furnish-Charging-Information.
	 * 
	 * @param information The information.
	 */
	public void setPsFurnishChargingInformation(PsFurnishChargingInformation information) {
		_psFurnishChargingInformation = information;
	}

	/**
	 * Gets the PS-Furnish-Charging-Information.
	 * 
	 * @return The information.
	 */
	public PsFurnishChargingInformation getPsFurnishChargingInformation() {
		return _psFurnishChargingInformation;
	}

	/**
	 * Sets the Refund-Information.
	 * 
	 * @param information The information.
	 */
	public void setRefundInformation(byte[] information) {
		_refundInformation = copyArray(information);
	}

	/**
	 * Gets the Refund-Information.
	 * 
	 * @return The information.
	 */
	public byte[] getRefundInformation() {
		return copyArray(_refundInformation);
	}

	/**
	 * Adds a AF-Correlation-Information.
	 * 
	 * @param information The information to be adde.
	 */
	public void addAfCorrelationInformation(AfCorrelationInformation information) {
		if (information != null) {
			_afCorrelationInformations.add(information);
		}
	}

	/**
	 * Gets the AF-Correlation-Information list.
	 * 
	 * @return The information.
	 */
	public Iterable<AfCorrelationInformation> getAfCorrelationInformations() {
		return _afCorrelationInformations;
	}

	/**
	 * Adds an Envelope.
	 * 
	 * @param envelope The envelope to be added.
	 */
	public void addEnvelope(Envelope envelope) {
		if (envelope != null) {
			_envelopes.add(envelope);
		}
	}

	/**
	 * Gets the Envelope list.
	 * 
	 * @return The envelopes.
	 */
	public Iterable<Envelope> getEnvelopes() {
		return _envelopes;
	}

	/**
	 * Sets the Envelope-Reporting.
	 * 
	 * @param reporting The reporting.
	 */
	public void setEnvelopeReporting(EnvelopeReporting reporting) {
		_envelopeReporting = reporting;
	}

	/**
	 * Gets the Envelope-Reporting.
	 * 
	 * @return The reporting.
	 */
	public EnvelopeReporting getEnvelopeReporting() {
		return _envelopeReporting;
	}

	/**
	 * Sets the Time-Quota-Mechanism.
	 * 
	 * @param mechanism The mechanism.
	 */
	public void setTimeQuotaMechanism(TimeQuotaMechanism mechanism) {
		_timeQuotaMechanism = mechanism;
	}

	/**
	 * Gets the Time-Quota-Mechanism.
	 * 
	 * @return The mechanism.
	 */
	public TimeQuotaMechanism getTimeQuotaMechanism() {
		return _timeQuotaMechanism;
	}

	/**
	 * Add a Service-Specific-Info.
	 * 
	 * @param info The info.
	 */
	public void addServiceSpecificInfo(ServiceSpecificInfo info) {
		if (info != null) {
			_serviceSpecificInfo.add(info);
		}
	}

	/**
	 * Gets the Service-Specific-Info list.
	 * 
	 * @return The info list.
	 */
	public Iterable<ServiceSpecificInfo> getServiceSpecificInfos() {
		return _serviceSpecificInfo;
	}

}
