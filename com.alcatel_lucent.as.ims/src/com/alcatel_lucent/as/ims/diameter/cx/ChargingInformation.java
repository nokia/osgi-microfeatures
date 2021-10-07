package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.UncompatibleAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Charging-Information AVP wrapper.
 */
public class ChargingInformation {

	private String _pecfn;
	private String _secfn;
	private String _pccfn;
	private String _sccfn;

	/**
	 * Constructor.
	 * 
	 * @param pccfn The address of the Primary Charging Collection Function
	 */
	public ChargingInformation(String pccfn) {
		setPCCFN(pccfn);
	}

	/**
	 * Constructor with a Diameter AVP.
	 * 
	 * @param avp The avp which contains the charging collection.
	 * @param version The version of the 3GPP 29.229 document.
	 * @throws UncompatibleAVPDefinition if the avp is not compatible with the
	 *           version.
	 */
	public ChargingInformation(DiameterAVP avp, Version version)
			throws UncompatibleAVPDefinition {

		DiameterAVPDefinition def = CxUtils.getChargingInformationAVP(version);
		if (!avp.isInstanceOf(def)) {
			throw new UncompatibleAVPDefinition(def, avp.getDiameterAVPDefinition());
		}

		def = CxUtils.getPrimaryEventChargingFunctionNameAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				setPECFN(UTF8StringFormat.getUtf8String(data.getValue()));
			}
		}
		def = CxUtils.getSecondaryChargingFunctionNameAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				setSECFN(UTF8StringFormat.getUtf8String(data.getValue()));
			}
		}
		def = CxUtils.getPrimaryChargingFunctionNameAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				setPCCFN(UTF8StringFormat.getUtf8String(data.getValue()));
			}
		}
		def = CxUtils.getSecondaryChargingFunctionNameAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				setSCCFN(UTF8StringFormat.getUtf8String(data.getValue()));
			}
		}
	}


	/**
	 * Creates a grouped AVP with this charging information.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getChargingInformationAVP(version);
		if (def == null) {
			return null;
		}
		if (getPCCFN() == null && getPECFN() == null && getSCCFN() == null && getSECFN() == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		DiameterAVP avp;
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		if (getPECFN() != null) {
			def = CxUtils.getPrimaryEventChargingFunctionNameAVP(version);
			avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getPECFN()), false);
			l.add(avp);
		}
		if (getSECFN() != null) {
			def = CxUtils.getSecondaryEventChargingFunctionNameAVP(version);
			avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getSECFN()), false);
			l.add(avp);
		}

		if (getPCCFN() != null) {
			def = CxUtils.getPrimaryChargingFunctionNameAVP(version);
			avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getPCCFN()), false);
			l.add(avp);
		}
		if (getSCCFN() != null) {
			def = CxUtils.getSecondaryChargingFunctionNameAVP(version);
			avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getSCCFN()), false);
			l.add(avp);
		}
		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the address of the Primary Event Charging Function.
	 * 
	 * @param pecfn The value.
	 */
	public void setPECFN(String pecfn) {
		_pecfn = pecfn;
	}

	/**
	 * Gets the address of the Primary Event Charging Function.
	 * 
	 * @return The address of the Primary Event Charging Function.
	 */
	public String getPECFN() {
		return _pecfn;
	}

	/**
	 * Sets the address of the Secondary Event Charging Function.
	 * 
	 * @param secfn The value.
	 */
	public void setSECFN(String secfn) {
		_secfn = secfn;
	}

	/**
	 * Gets the address of the Secondary Event Charging Function.
	 * 
	 * @return The address of the Secondary Event Charging Function.
	 */
	public String getSECFN() {
		return _secfn;
	}

	/**
	 * Sets the address of the Primary Charging Collection Function.
	 * 
	 * @param pccfn The value.
	 */
	public void setPCCFN(String pccfn) {
		_pccfn = pccfn;
	}

	/**
	 * Gets the address of the Primary Charging Collection Function.
	 */
	private String getPCCFN() {
		return _pccfn;
	}

	/**
	 * Sets the address of the Secondary Charging Collection Function.
	 */
	public void setSCCFN(String sccfn) {
		_sccfn = sccfn;
	}

	/**
	 * Gets the address of the Secondary Charging Collection Function.
	 * 
	 * @return The address of the Secondary Charging Collection Function.
	 */
	public String getSCCFN() {
		return _sccfn;
	}

}
