package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Service-Specific-Info AVP wrapper.
 */
public class ServiceSpecificInfo {

	private String _serviceSpecificData = null;
	private Long _serviceSpecificType = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public ServiceSpecificInfo(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public ServiceSpecificInfo(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getServiceSpecificDataAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceSpecificData(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}
		def = ChargingUtils.getServiceSpecificTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceSpecificType(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getMessageBodyAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		if (getServiceSpecificData() != null) {
			def = ChargingUtils.getServiceSpecificDataAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getServiceSpecificData()), false);
				l.add(avp);
			}
		}

		if (getServiceSpecificType() != null) {
			def = ChargingUtils.getServiceSpecificTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getServiceSpecificType()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the service specific data.
	 * 
	 * @param data The data.
	 */
	public void setServiceSpecificData(String data) {
		_serviceSpecificData = data;
	}

	/**
	 * Gets the service specific data (mapped to the Service-Specific-Data AVP).
	 * 
	 * @return The _serviceSpecificData.
	 */
	public String getServiceSpecificData() {
		return _serviceSpecificData;
	}

	/**
	 * Sets the service specific type.
	 * 
	 * @param type The type.
	 */
	public void setServiceSpecificType(Long type) {
		_serviceSpecificType = type;
	}

	/**
	 * Gets the service specific type (mapped to the Service-Specific-Type AVP)
	 * 
	 * @return The type.
	 */
	public Long getServiceSpecificType() {
		return _serviceSpecificType;
	}

}
