package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.UserEquipmentInfoType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The User-Equipment-Info AVP wrapper.
 */
public class UserEquipmentInfo {

	private UserEquipmentInfoType _userEquipmentInfoType = null;
	private byte[] _userEquipmentInfoValue = null;

	private UserEquipmentInfo() {}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param type The User-Equipment-Info-Type.
	 * @param value The User-Equipment-Info-Value.
	 */
	public UserEquipmentInfo(UserEquipmentInfoType type, byte[] value) {
		this();
		setUserEquipmentInfoType(type);
		setUserEquipmentInfoValue(value);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public UserEquipmentInfo(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		this();
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getUserEquipmentInfoTypeAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setUserEquipmentInfoType(UserEquipmentInfoType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getUserEquipmentInfoValueAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setUserEquipmentInfoValue(searchedAvp.getValue());
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getUserEquipmentInfoAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getUserEquipmentInfoTypeAVP();
		DiameterAVP avp = new DiameterAVP(def);
		avp.setValue(EnumeratedFormat.toEnumerated(getUserEquipmentInfoType().getValue()), false);
		l.add(avp);

		def = ChargingUtils.getUserEquipmentInfoValueAVP();
		avp = new DiameterAVP(def);
		avp.setValue(getUserEquipmentInfoValue(), false);
		l.add(avp);

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the User-Equipment-Info-Type.
	 * 
	 * @param type The type.
	 */
	protected void setUserEquipmentInfoType(UserEquipmentInfoType type) {
		if (type == null) {
			throw new NullPointerException("type is null");
		}
		_userEquipmentInfoType = type;
	}

	/**
	 * Gets the User-Equipment-Info-Type.
	 * 
	 * @return The type.
	 */
	public UserEquipmentInfoType getUserEquipmentInfoType() {
		return _userEquipmentInfoType;
	}

	/**
	 * Sets the User-Equipment-Info-Value.
	 * 
	 * @param value The value.
	 */
	protected void setUserEquipmentInfoValue(byte[] value) {
		if (value == null) {
			throw new NullPointerException("value is null");
		}
		_userEquipmentInfoValue = ChargingUtils.copyArray(value);
	}

	/**
	 * Gets the User-Equipment-Info-Value.
	 * 
	 * @return The value.
	 */
	public byte[] getUserEquipmentInfoValue() {
		return ChargingUtils.copyArray(_userEquipmentInfoValue);
	}

}
