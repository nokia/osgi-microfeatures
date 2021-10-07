package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.Address;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The WlLAN-Information AVP wrapper.
 */
public class WlanInformation {

	private Long _wlanSessionId = null;
	private Address _pdgAddress = null;
	private Long _pdgChargingId = null;
	private Address _wagAddress = null;
	private byte[] _wagPlmnId = null;
	private WlanRadioContainer _wlanRadioContainer = null;
	private Address _wlanUeLocalIpaddress = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public WlanInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getWlanSessionIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWlanSessionId(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPdgAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPdgAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getPdgChargingIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPdgChargingId(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getWagAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWagAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getWagPlmnIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWagPlmnId(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getWlanRadioContainerAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWlanRadioContainer(new WlanRadioContainer(searchedAvp, version));
			}
		}

		def = ChargingUtils.getWlanUeLocalIpaddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWlanUeLocalIpaddress(new Address(searchedAvp.getValue()));
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
		DiameterAVPDefinition def = ChargingUtils.getWlanInformationAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getWlanSessionId() != null) {
			def = ChargingUtils.getWlanSessionIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getWlanSessionId()), false);
				l.add(avp);
			}
		}

		if (getPdgAddress() != null) {
			def = ChargingUtils.getPdgAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getPdgAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getPdgChargingId() != null) {
			def = ChargingUtils.getPdgChargingIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getPdgChargingId()), false);
				l.add(avp);
			}
		}

		if (getWagAddress() != null) {
			def = ChargingUtils.getWagAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getWagAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getWagPlmnId() != null) {
			def = ChargingUtils.getWagPlmnIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getWagPlmnId(), false);
				l.add(avp);
			}
		}

		if (getWlanRadioContainer() != null) {
			def = ChargingUtils.getWagPlmnIdAVP(version);
			if (def != null) {
				DiameterAVP avp = getWlanRadioContainer().toAvp(version);
				l.add(avp);
			}
		}

		if (getWlanUeLocalIpaddress() != null) {
			def = ChargingUtils.getWlanUeLocalIpaddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getWlanUeLocalIpaddress().getAvpValue(), false);
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
	 * Sets the PDG_Address.
	 * 
	 * @param pdgAddress The address.
	 */
	public void setPdgAddress(Address pdgAddress) {
		_pdgAddress = pdgAddress;
	}

	/**
	 * Gets the PDG_Address.
	 * 
	 * @return The address.
	 */
	public Address getPdgAddress() {
		return _pdgAddress;
	}

	/**
	 * Sets the WLAN-Session-Id.
	 * 
	 * @param wlanSessionId The session id.
	 */
	public void setWlanSessionId(Long wlanSessionId) {
		_wlanSessionId = wlanSessionId;
	}

	/**
	 * Gets the WLAN-Session-Id.
	 * 
	 * @return The session id.
	 */
	public Long getWlanSessionId() {
		return _wlanSessionId;
	}

	/**
	 * Sets the PDG-Charging-Id.
	 * 
	 * @param id The id.
	 */
	public void setPdgChargingId(Long id) {
		_pdgChargingId = id;
	}

	/**
	 * Gets the PDG-Charging-Id.
	 * 
	 * @return The id.
	 */
	public Long getPdgChargingId() {
		return _pdgChargingId;
	}

	/**
	 * Sets the WAG-Address.
	 * 
	 * @param address The address.
	 */
	public void setWagAddress(Address address) {
		_wagAddress = address;
	}

	/**
	 * Gets the WAG-Address.
	 * 
	 * @return The address.
	 */
	public Address getWagAddress() {
		return _wagAddress;
	}

	/**
	 * Sets the WAG-PLMN-Id.
	 * 
	 * @param id The id.
	 */
	public void setWagPlmnId(byte[] id) {
		_wagPlmnId = copyArray(id);
	}

	/**
	 * Gets the WAG-PLMN-Id.
	 * 
	 * @return The id.
	 */
	public byte[] getWagPlmnId() {
		return copyArray(_wagPlmnId);
	}

	/**
	 * Sets the WLAN-Radio-Container.
	 * 
	 * @param container The container.
	 */
	public void setWlanRadioContainer(WlanRadioContainer container) {
		_wlanRadioContainer = container;
	}

	/**
	 * Gets the WLAN-Radio-Container.
	 * 
	 * @return The container.
	 */
	public WlanRadioContainer getWlanRadioContainer() {
		return _wlanRadioContainer;
	}

	/**
	 * Sets the WLAN-UE-Local-IPAddress.
	 * 
	 * @param address The address.
	 */
	public void setWlanUeLocalIpaddress(Address address) {
		_wlanUeLocalIpaddress = address;
	}

	/**
	 * Gets the WLAN-UE-Local-IPAddress.
	 * 
	 * @return The address.
	 */
	public Address getWlanUeLocalIpaddress() {
		return _wlanUeLocalIpaddress;
	}

}
