package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Cost-Information AVP wrapper.
 */
public class CostInformation {

	private UnitValue _unitValue = null;
	private Long _currencyCode = null;
	private String _costUnit = null;

	private CostInformation() {}

	/**
	 * Constructor for this class. 
	 *
	 * @param unitValue The Unit-Value.
	 * @param currencyCode The Currency-Code.
	 */
	public CostInformation(UnitValue unitValue, Long currencyCode) {
		this();
		setUnitValue(unitValue);
		setCurrencyCode(currencyCode);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public CostInformation(DiameterAVP avp)
			throws DiameterMissingAVPException {
		if (avp == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getUnitValueAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setUnitValue(new UnitValue(searchedAvp));

		def = ChargingUtils.getCurrencyCodeAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setCurrencyCode(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getCostUnitAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setCostUnit(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
		}

	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getCostInformationAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		l.add(getUnitValue().toAvp());

		def = ChargingUtils.getCurrencyCodeAVP();
		DiameterAVP avp = new DiameterAVP(def);
		avp.setValue(Unsigned32Format.toUnsigned32(getCurrencyCode()), false);

		if (getCostUnit() != null) {
			def = ChargingUtils.getCostUnitAVP();
			avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getCostUnit()), false);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Unit-Value.
	 * 
	 * @param value The value.
	 */
	public void setUnitValue(UnitValue value) {
		if (value == null) {
			throw new IllegalArgumentException("value is null");
		}
		_unitValue = value;
	}

	/**
	 * Gets the Unit-Value.
	 * 
	 * @return The value.
	 */
	public UnitValue getUnitValue() {
		return _unitValue;
	}

	/**
	 * Sets the Currency-Code.
	 * 
	 * @param code The code.
	 */
	public void setCurrencyCode(Long code) {
		if (code == null) {
			throw new IllegalArgumentException("code is null");
		}
		_currencyCode = code;
	}

	/**
	 * Gets the Currency-Code.
	 * 
	 * @return The code.
	 */
	public Long getCurrencyCode() {
		return _currencyCode;
	}

	/**
	 * Sets the Cost-Unit.
	 * 
	 * @param unit The unit.
	 */
	public void setCostUnit(String unit) {
		_costUnit = unit;
	}

	/**
	 * Gets the Cost-Unit.
	 * 
	 * @return The unit.
	 */
	public String getCostUnit() {
		return _costUnit;
	}

}
