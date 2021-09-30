package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.FinalUnitAction;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Flows AVP wrapper.
 */
public class Flows {

	private Long _mediaComponentNumber = null;
	private List<Long> _flowNumbers = new ArrayList<Long>();
	private FinalUnitAction _finalUnitAction = null;

	private Flows() {}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param number The Media-Component-Number.
	 */
	public Flows(Long number) {
		this();
		setMediaComponentNumber(number);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public Flows(byte[] data, Version version)
			throws DiameterMissingAVPException {
		this();
		if (data == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getMediaComponentNumberAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp == null) {
				throw new DiameterMissingAVPException(def);
			}
			setMediaComponentNumber(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getFlowNumberAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addFlowNumber(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(i), 0)));
				}
			}
		}

		def = ChargingUtils.getFinalUnitActionAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp != null) {
			setFinalUnitAction(FinalUnitAction.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
		}
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getFlowsAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getMediaComponentNumberAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getMediaComponentNumber()), false);
			avps.add(avp);
		}

		Iterable<Long> numbers = getFlowNumbers();
		if (numbers.iterator().hasNext()) {
			def = ChargingUtils.getFlowNumberAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (Long number : numbers) {
					avp.addValue(Unsigned32Format.toUnsigned32(number), false);
				}
				avps.add(avp);
			}
		}

		if (getFinalUnitAction() != null) {
			def = ChargingUtils.getFinalUnitActionAVP();
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getFinalUnitAction().getValue()), false);
				avps.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the Media-Component-Number.
	 * 
	 * @param number The number.
	 */
	public void setMediaComponentNumber(Long number) {
		if (number == null) {
			throw new NullPointerException("number is null");
		}
		_mediaComponentNumber = number;
	}

	/**
	 * Gets the Media-Component-Number.
	 * 
	 * @return The number.
	 */
	public Long getMediaComponentNumber() {
		return _mediaComponentNumber;
	}

	/**
	 * Adds a Flow-Number.
	 * 
	 * @param number The number.
	 */
	public void addFlowNumber(Long number) {
		if (number != null) {
			_flowNumbers.add(number);
		}
	}

	/**
	 * Gets the Flow-Number list.
	 * 
	 * @return The numbers.
	 */
	public Iterable<Long> getFlowNumbers() {
		return _flowNumbers;
	}

	/**
	 * Sets the Final-Unit-Action.
	 * 
	 * @param action The action.
	 */
	public void setFinalUnitAction(FinalUnitAction action) {
		this._finalUnitAction = action;
	}

	/**
	 * Gets the Final-Unit-Action.
	 * 
	 * @return The action.
	 */
	public FinalUnitAction getFinalUnitAction() {
		return _finalUnitAction;
	}

}
