package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RedirectAddressType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Redirect-Server AVP wrapper.
 */
public class RedirectServer {

	private RedirectAddressType _redirectAddressType = null;
	private String _redirectServerAddress = null;

	private RedirectServer() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param type The Redirect-Address-Type.
	 * @param address The Redirect-Server-Address.
	 */
	public RedirectServer(RedirectAddressType type, String address) {
		this();
		setRedirectAddressType(type);
		setRedirectServerAddress(address);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @exception DiameterMissingAVPException if the Final-Unit-Action avp is not
	 *              present in the avp.
	 */
	public RedirectServer(DiameterAVP avp)
			throws DiameterMissingAVPException {
		this();
		if (avp == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getRedirectAddressTypeAVP();
		DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setRedirectAddressType(RedirectAddressType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));

		def = ChargingUtils.getRedirectServerAddressAVP();
		searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
		if (searchedAvp == null) {
			throw new DiameterMissingAVPException(def);
		}
		setRedirectServerAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp() {
		DiameterAVPDefinition def = ChargingUtils.getRedirectServerAVP();
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getRedirectAddressTypeAVP();
		DiameterAVP avp = new DiameterAVP(def);
		avp.setValue(EnumeratedFormat.toEnumerated(getRedirectAddressType().getValue()), false);
		avps.add(avp);

		def = ChargingUtils.getRedirectServerAddressAVP();
		avp = new DiameterAVP(def);
		avp.setValue(UTF8StringFormat.toUtf8String(getRedirectServerAddress()), false);
		avps.add(avp);

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the Redirect-Address-Type.
	 * 
	 * @param type The type.
	 */
	protected void setRedirectAddressType(RedirectAddressType type) {
		if (type != null) {
			_redirectAddressType = type;
		}
	}

	/**
	 * Gets the Redirect-Address-Type.
	 * 
	 * @return The type.
	 */
	public RedirectAddressType getRedirectAddressType() {
		return _redirectAddressType;
	}

	/**
	 * Sets the Redirect-Server-Address.
	 * 
	 * @param address The address.
	 */
	protected void setRedirectServerAddress(String address) {
		if (address != null) {
			_redirectServerAddress = address;
		}
	}

	/**
	 * Gets the Redirect-Server-Address.
	 * 
	 * @return The address.
	 */
	public String getRedirectServerAddress() {
		return _redirectServerAddress;
	}

}
