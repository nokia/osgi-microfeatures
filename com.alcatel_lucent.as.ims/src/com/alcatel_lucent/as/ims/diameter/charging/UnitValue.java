package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.Integer64Format;

/**
 * The Unit-Value AVP wrapper.
 */
public class UnitValue {

	private Long _valueDigit = null;
	private Integer _exponent = null;

	private UnitValue() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param valueDigit The Value-Digit.
	 */
	public UnitValue(Long valueDigit) {
		this();
		setValueDigit(valueDigit);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @exception DiameterMissingAVPException if the Value-Digit is not present in
	 *              the avp.
	 */
	public UnitValue(DiameterAVP avp)
			throws DiameterMissingAVPException {
		this();
		if (avp == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] informationData = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getValueDigitsAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setValueDigit(Long.valueOf(Integer64Format.getInteger64(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getExponentAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
		if (searchedAvp != null) {
			setExponent(Integer.valueOf(Integer32Format.getInteger32(searchedAvp.getValue(), 0)));
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getUnitValueAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getValueDigitsAVP();
		DiameterAVP avp = new DiameterAVP(def);
		avp.setValue(Integer64Format.toInteger64(getValueDigit()), false);
		avps.add(avp);

		if (getExponent() != null) {
			def = ChargingUtils.getExponentAVP();
			avp = new DiameterAVP(def);
			avp.setValue(Integer32Format.toInteger32(getExponent()), false);
			avps.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the Value-Digit.
	 * 
	 * @param value The value.
	 */
	protected void setValueDigit(Long value) {
		if (value == null) {
			throw new NullPointerException("value is null");
		}
		_valueDigit = value;
	}

	/**
	 * Gets the Value-Digit.
	 * 
	 * @return The value.
	 */
	public Long getValueDigit() {
		return _valueDigit;
	}

	/**
	 * Sets the Exponent.
	 * 
	 * @param exponent The exponent.
	 */
	public void setExponent(Integer exponent) {
		_exponent = exponent;
	}

	/**
	 * Gets theExponent.
	 * 
	 * @return The exponent.
	 */
	public Integer getExponent() {
		return _exponent;
	}

}
