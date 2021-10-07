package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.UncompatibleAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The SIP-Digest-Authenticate AVP wrapper.
 */
public class SipDigestAuthenticate {

	private byte[] _realm = null;
	private byte[] _algo = null;
	private byte[] _qop = null;
	private byte[] _ha1 = null;

	public SipDigestAuthenticate() {}

	/**
	 * Constructor with a Diameter AVP.
	 * 
	 * @param data The avp.
	 * @param version The version of the 3GPP 29.229 document.
	 * @throws UncompatibleAVPDefinition if the avp is not compatible with the
	 *           version.
	 */
	public SipDigestAuthenticate(DiameterAVP data, Version version)
			throws UncompatibleAVPDefinition {
		DiameterAVPDefinition def = CxUtils.getSIPDigestAuthenticateAVP(version);
		if (data == null) {
			throw new NullPointerException("no data");
		}

		if (!data.isInstanceOf(def)) {
			throw new UncompatibleAVPDefinition(def, data.getDiameterAVPDefinition());
		}

		byte[] groupedValue = data.getValue();
		def = CxUtils.getDigestRealmAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setDigestRealm(avp.getValue());
			}
		}

		def = CxUtils.getDigestAlgorithmAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setDigestAlgorithm(avp.getValue());
			}
		}

		def = CxUtils.getDigestQoPAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setDigestQop(avp.getValue());
			}
		}

		def = CxUtils.getDigestHA1AVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedValue, false);
			if (avp != null) {
				setDigestHa1(avp.getValue());
			}
		}

	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("[SipDigestAuhtentication");
		if (getDigestRealm() != null) {
			res.append(" realm=").append(new String(getDigestRealm()));
		}
		if (getDigestAlgorithm() != null) {
			res.append(" algorithm=").append(new String(getDigestAlgorithm()));
		}
		if (getDigestQop() != null) {
			res.append(" QoP=").append(new String(getDigestQop()));
		}
		if (getDigestHa1() != null) {
			res.append(" HA1=").append(new String(getDigestHa1()));
		}

		res.append("]");
		return res.toString();
	}

	/**
	 * Creates a grouped AVP with this charging information.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getSIPDigestAuthenticateAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = CxUtils.getDigestRealmAVP(version);
		if (def != null) {
			byte[] value = getDigestRealm();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getDigestAlgorithmAVP(version);
		if (def != null) {
			byte[] value = getDigestAlgorithm();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getDigestQoPAVP(version);
		if (def != null) {
			byte[] value = getDigestQop();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getDigestHA1AVP(version);
		if (def != null) {
			byte[] value = getDigestHa1();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the digest realm.
	 * 
	 * @param realm The digest realm.
	 */
	public void setDigestRealm(byte[] realm) {
		_realm = copy(realm);
	}

	/**
	 * Gets the digest realm.
	 * 
	 * @return The digest realm.
	 */
	public byte[] getDigestRealm() {
		return copy(_realm);
	}

	/**
	 * Sets the digest algorithm.
	 * 
	 * @param algo The digest algorithm.
	 */
	public void setDigestAlgorithm(byte[] algo) {
		_algo = copy(algo);
	}

	/**
	 * Gets the digest algorithm.
	 * 
	 * @return The digest algorithm.
	 */
	public byte[] getDigestAlgorithm() {
		return copy(_algo);
	}

	/**
	 * Sets the digest QoP.
	 * 
	 * @param qop The digest QoP.
	 */
	public void setDigestQop(byte[] qop) {
		_qop = copy(qop);
	}

	/**
	 * Gets the digest QoP.
	 * 
	 * @return The digest QoP.
	 */
	public byte[] getDigestQop() {
		return copy(_qop);
	}

	/**
	 * Sets the digest HA1.
	 * 
	 * @param ha1 The digest HA1.
	 */
	public void setDigestHa1(byte[] ha1) {
		_ha1 = copy(ha1);
	}

	/**
	 * Gets the digest HA1.
	 * 
	 * @return The digest HA1.
	 */
	public byte[] getDigestHa1() {
		return copy(_ha1);
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
