package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.EnvelopeReporting;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Offline-Charging AVP wrapper.
 */
public class OfflineCharging {

	private Long _quotaConsumptionTime = null;
	private TimeQuotaMechanism _timeQuotaMechanism = null;
	private EnvelopeReporting _envelopeReporting = null;
	private List<MultipleServicesCreditControl> _multipleServicesCreditControl = new ArrayList<MultipleServicesCreditControl>();
	private List<DiameterAVP> _avps = new ArrayList<DiameterAVP>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public OfflineCharging(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getQuotaConsumptionTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setQuotaConsumptionTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTimeQuotaMechanismAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTimeQuotaMechanism(new TimeQuotaMechanism(searchedAvp, version));
			}
		}

		def = ChargingUtils.getEnvelopeReportingAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setEnvelopeReporting(EnvelopeReporting.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMultipleServiceCreditControlAVP();
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addMultipleServicesCreditControl(new MultipleServicesCreditControl(searchedAvp.getValue(i), version));
				}
			}
		}

		List<DiameterAVP> avps = GroupedFormat.getGroupedAVPs(data, false);
		for (DiameterAVP a : avps) {
			def = a.getDiameterAVPDefinition();
			if (def == ChargingUtils.getQuotaConsumptionTimeAVP(version)) {
				continue;
			}
			if (def == ChargingUtils.getTimeQuotaMechanismAVP(version)) {
				continue;
			}
			if (def == ChargingUtils.getEnvelopeReportingAVP(version)) {
				continue;
			}
			if (def == ChargingUtils.getMultipleServiceCreditControlAVP()) {
				continue;
			}
			addAvp(a);
		}

	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getOfflineChargingAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getQuotaConsumptionTime() != null) {
			def = ChargingUtils.getQuotaConsumptionTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getQuotaConsumptionTime()), false);
				l.add(avp);
			}
		}

		if (getTimeQuotaMechanism() != null) {
			DiameterAVP avp = getTimeQuotaMechanism().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getEnvelopeReporting() != null) {
			def = ChargingUtils.getEnvelopeReportingAVP(version);
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(EnumeratedFormat.toEnumerated(getEnvelopeReporting().getValue()), false);
			l.add(avp);
		}

		Iterable<MultipleServicesCreditControl> controls = getMultipleServicesCreditControls();
		if (controls.iterator().hasNext()) {
			def = ChargingUtils.getMultipleServiceCreditControlAVP();
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (MultipleServicesCreditControl control : controls) {
					avp.addValue(control.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		for (DiameterAVP avp : getAvps()) {
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
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
	 * Ads a Multiple-Services-Credit-Control.
	 * 
	 * @param control The control.
	 */
	public void addMultipleServicesCreditControl(MultipleServicesCreditControl control) {
		_multipleServicesCreditControl.add(control);
	}

	/**
	 * Gets the Multiple-Services-Credit-Control list.
	 * 
	 * @return The controls.
	 */
	public Iterable<MultipleServicesCreditControl> getMultipleServicesCreditControls() {
		return _multipleServicesCreditControl;
	}

	/**
	 * Adds an avp.
	 * 
	 * @param avp The avp.
	 */
	public void addAvp(DiameterAVP avp) {
		_avps.add(avp);
	}

	/**
	 * Gets the avps list.
	 * 
	 * @return The avps.
	 */
	public Iterable<DiameterAVP> getAvps() {
		return _avps;
	}

}
