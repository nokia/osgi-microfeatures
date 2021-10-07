package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PsAppendFreeFormatData;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The PS-Furnish-Charging-Information AVP wrapper.
 */
public class PsFurnishChargingInformation {

	private byte[] _3gppChargingId = null;
	private byte[] _psFreeFormatData = null;
	private PsAppendFreeFormatData _psAppendFreeFormatData = null;

	private PsFurnishChargingInformation() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param chargingId The 3GPP-Charging-Id.
	 * @param psFreeFormatData The PS-Free-Format-Data.
	 */
	public PsFurnishChargingInformation(byte[] chargingId, byte[] psFreeFormatData) {
		this();
		set3gppChargingId(chargingId);
		setPsFreeFormatData(psFreeFormatData);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public PsFurnishChargingInformation(DiameterAVP avp, Version version) {
		this();
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.get3gppChargingIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			set3gppChargingId(searchedAvp.getValue());
		}

		def = ChargingUtils.getPsFreeFormatDataAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			setPsFreeFormatData(searchedAvp.getValue());
		}

		def = ChargingUtils.getPsAppendFreeFormatDataAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPsAppendFreeFormatData(PsAppendFreeFormatData.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getPsFurnishChargingInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.get3gppChargingIdAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(get3gppChargingId(), false);
			l.add(avp);
		}

		def = ChargingUtils.getPsFreeFormatDataAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(getPsFreeFormatData(), false);
			l.add(avp);
		}

		if (getPsAppendFreeFormatData() != null) {
			def = ChargingUtils.getPsAppendFreeFormatDataAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPsAppendFreeFormatData().getValue()), false);
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
	 * Sets the 3GPP-Charging-Id.
	 * 
	 * @param id The id.
	 */
	public void set3gppChargingId(byte[] id) {
		if (id == null) {
			throw new NullPointerException("3GPP-Charging-Id value is null");
		}

		_3gppChargingId = copyArray(id);
	}

	/**
	 * Gets the 3GPP-Charging-Id.
	 * 
	 * @return The id.
	 */
	public byte[] get3gppChargingId() {
		return copyArray(_3gppChargingId);
	}

	/**
	 * Sets the PS-Free-Format-Data.
	 * 
	 * @param data The data.
	 */
	public void setPsFreeFormatData(byte[] data) {
		if (data == null) {
			throw new NullPointerException("PS-Free-Format-Data value is null");
		}

		_psFreeFormatData = copyArray(data);
	}

	/**
	 * Gets the PS-Free-Format-Data.
	 * 
	 * @return The data.
	 */
	public byte[] getPsFreeFormatData() {
		return copyArray(_psFreeFormatData);
	}

	/**
	 * Sets the PS-Append-Free-Format-Data.
	 * 
	 * @param data The data.
	 */
	public void setPsAppendFreeFormatData(PsAppendFreeFormatData data) {
		_psAppendFreeFormatData = data;
	}

	/**
	 * Gets the PS-Append-Free-Format-Data.
	 * 
	 * @return The data.
	 */
	public PsAppendFreeFormatData getPsAppendFreeFormatData() {
		return _psAppendFreeFormatData;
	}
}
