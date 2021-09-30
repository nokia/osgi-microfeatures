package com.alcatel_lucent.as.ims.diameter.sh;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.UncompatibleAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The User-Identity AVP wrapper.
 */
public class UserIdentity {

	private String _publicIdentity;
	private byte[] _msisdn;

	/**
	 * Constructor for this class.
	 * 
	 * @param publicIdentity The Public-Identity value.
	 */
	public UserIdentity(String publicIdentity) {
		setPublicIdentity(publicIdentity);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param msisdn The MSISDN value.
	 */
	public UserIdentity(byte[] msisdn) {
		setMsisdn(msisdn);
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param avp The avp.
	 * @param version The Sh version.
	 */
	public UserIdentity(DiameterAVP avp, Version version) {

		DiameterAVPDefinition def = ShUtils.getUserIdentityAvpDefinition(version);
		if (!avp.isInstanceOf(def)) {
			throw new UncompatibleAVPDefinition(def, avp.getDiameterAVPDefinition());
		}

		def = ShUtils.getMsisdnAvpDefinition(version);
		if (def == null) {
			throw new UncompatibleAVPDefinition(avp.getDiameterAVPDefinition());
		}
		DiameterAVP dAvp = GroupedFormat.getDiameterAVP(def, avp.getValue(), false);
		if (dAvp != null) {
			setMsisdn(dAvp.getValue());
			return;
		}

		def = ShUtils.getPublicIdentityAvpDefinition(version);
		if (def == null) {
			throw new UncompatibleAVPDefinition(avp.getDiameterAVPDefinition());
		}
		dAvp = GroupedFormat.getDiameterAVP(def, avp.getValue(), false);
		if (dAvp != null) {
			setPublicIdentity(UTF8StringFormat.getUtf8String(dAvp.getValue()));
		}

	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ShUtils.getUserIdentityAvpDefinition(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		byte[] value = getMsisdn();
		if (value != null) {
			def = ShUtils.getMsisdnAvpDefinition(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				avps.add(avp);
			}
		} else if (getPublicIdentity() != null) {
			def = ShUtils.getPublicIdentityAvpDefinition(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getPublicIdentity()), false);
				avps.add(avp);
			}
		}
		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Sets the Public-Identity value.
	 * 
	 * @param publicidentity The Public-Identity value.
	 */
	public void setPublicIdentity(String publicidentity) {
		_publicIdentity = publicidentity;
	}

	/**
	 * Gets the Public-Identity value.
	 * 
	 * @return The Public-Identity value.
	 */
	public String getPublicIdentity() {
		return _publicIdentity;
	}

	/**
	 * Sets the MSISDN value.
	 * 
	 * @param msisdn The MSISDN value.
	 */
	public void setMsisdn(byte[] msisdn) {
		_msisdn = copy(msisdn);
	}

	/**
	 * Gets the MSISDN value.
	 * 
	 * @return The MSISDN value.
	 */
	public byte[] getMsisdn() {
		return copy(_msisdn);
	}

	private byte[] copy(byte[] value) {
		byte[] res = null;
		if (value != null) {
			res = new byte[value.length];
			System.arraycopy(value, 0, res, 0, value.length);
		}
		return res;
	}

}