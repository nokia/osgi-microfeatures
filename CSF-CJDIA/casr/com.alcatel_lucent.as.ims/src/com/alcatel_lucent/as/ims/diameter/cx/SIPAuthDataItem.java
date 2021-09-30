package com.alcatel_lucent.as.ims.diameter.cx;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * The SIP-Auth-Data-Item AVP wrapper.
 */
public class SIPAuthDataItem {

	private Long _itemNumber;
	private String _authenticationScheme;
	private byte[] _sipAuthenticate, _sipAuthorization, _authenticationContext, _confidentialityKey, _integrityKey;
	private SipDigestAuthenticate _sipDigestAuthenticate;
	private byte[] _framedIPAddress, _framedIPv6Prefix;
	private BigInteger _framedInterfaceId;
	private List<byte[]> _lineIdentifiers;

	/**
	 * Constructor for class.
	 */
	public SIPAuthDataItem() {}

	/**
	 * Constructor for class.
	 * 
	 * @param groupedData The data of the grouped AVP.
	 * @param version The Cx version.
	 */
	public SIPAuthDataItem(byte[] groupedData, Version version) {

		DiameterAVPDefinition def = CxUtils.getSipItemNumberAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setSIPItemNumber(Unsigned32Format.getUnsigned32(data.getValue(), 0));
			}
		}

		def = CxUtils.getSipAuthenticationSchemeAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setSIPAuthenticationScheme(UTF8StringFormat.getUtf8String(data.getValue()));
			}
		}

		def = CxUtils.getSipAuthenticateAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setSIPAuthenticate(data.getValue());
			}
		}

		def = CxUtils.getSipAuthorizationAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setSIPAuthorization(data.getValue());
			}
		}

		def = CxUtils.getSipAuthenticationContextAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setSIPAuthenticationContext(data.getValue());
			}
		}

		def = CxUtils.getConfidentialityKeyAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setConfidentialityKey(data.getValue());
			}
		}

		def = CxUtils.getIntegrityKeyAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setIntegrityKey(data.getValue());
			}
		}

		def = CxUtils.getSIPDigestAuthenticateAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setSipDigestAuthenticate(new SipDigestAuthenticate(data, version));
			}
		}

		def = CxUtils.getLineIdentifierAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				List<byte[]> l = new ArrayList<byte[]>();
				for (int i = 0; i < data.getValueSize(); i++) {
					l.add(data.getValue(i));
				}
				setLineIdentifiers(l);
			}
		}

		def = CxUtils.getFramedIPAddressAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setFramedIPAddress(data.getValue());
			}
		}

		def = CxUtils.getFramedIPv6PrefixAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setFramedIPv6Prefix(data.getValue());
			}
		}

		def = CxUtils.getFramedInterfaceIdAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (data != null) {
				setFramedInterfaceId(Unsigned64Format.getUnsigned64BigInteger(data.getValue(), 0));
			}
		}

	}

	/**
	 * Creates a grouped AVP with charging information.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getSipAuthDataItemAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = CxUtils.getSipItemNumberAVP(version);
		if (def != null && getSIPItemNumber() != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned32Format.toUnsigned32(getSIPItemNumber()), false);
			l.add(avp);
		}

		def = CxUtils.getSipAuthenticationSchemeAVP(version);
		if (def != null && getSIPAuthenticationScheme() != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getSIPAuthenticationScheme()), false);
			l.add(avp);
		}

		def = CxUtils.getSipAuthenticateAVP(version);
		if (def != null) {
			byte[] value = getSIPAuthenticate();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getSipAuthorizationAVP(version);
		if (def != null) {
			byte[] value = getSIPAuthorization();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getSipAuthenticationContextAVP(version);
		if (def != null) {
			byte[] value = getSIPAuthenticationContext();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getConfidentialityKeyAVP(version);
		if (def != null) {
			byte[] value = getConfidentialityKey();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getIntegrityKeyAVP(version);
		if (def != null) {
			byte[] value = getIntegrityKey();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		if (getSipDigestAuthenticate() != null) {
			DiameterAVP avp = getSipDigestAuthenticate().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		def = CxUtils.getLineIdentifierAVP(version);
		if (def != null && getLineIdentifiers() != null) {
			DiameterAVP avp = new DiameterAVP(def);
			for (byte[] data : getLineIdentifiers()) {
				avp.addValue(data, false);
			}
			l.add(avp);
		}

		def = CxUtils.getFramedIPAddressAVP(version);
		if (def != null) {
			byte[] value = getFramedIPAddress();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getFramedIPv6PrefixAVP(version);
		if (def != null) {
			byte[] value = getFramedIPv6Prefix();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				l.add(avp);
			}
		}

		def = CxUtils.getFramedInterfaceIdAVP(version);
		if (def != null && getFramedInterfaceId() != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(Unsigned64Format.toUnsigned64(getFramedInterfaceId()), false);
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the SIP-Item-Number value.
	 * 
	 * @param num The value.
	 */
	public void setSIPItemNumber(long num) {
		_itemNumber = Long.valueOf(num);
	}

	/**
	 * Gets the SIP-Item-Number value.
	 * 
	 * @return The SIP-Item-Number value.
	 */
	public Long getSIPItemNumber() {
		return _itemNumber;
	}

	/**
	 * Sets the SIP-Authentication-Scheme value.
	 * 
	 * @param scheme The value.
	 */
	public void setSIPAuthenticationScheme(String scheme) {
		_authenticationScheme = scheme;
	}

	/**
	 * Gets the SIP-Authentication-Scheme value.
	 * 
	 * @return The SIP-Authentication-Scheme value.
	 */
	public String getSIPAuthenticationScheme() {
		return _authenticationScheme;
	}

	/**
	 * Sets the SIP-Authenticate value.
	 * 
	 * @param auth The value.
	 */
	public void setSIPAuthenticate(byte[] auth) {
		_sipAuthenticate = copy(auth);
	}

	/**
	 * Gets the SIP-Authenticate value.
	 * 
	 * @return The SIP-Authenticate value.
	 */
	public byte[] getSIPAuthenticate() {
		return copy(_sipAuthenticate);
	}

	/**
	 * Sets the SIP-Authorization value.
	 * 
	 * @param auth The SIP-Authorization value.
	 */
	public void setSIPAuthorization(byte[] auth) {
		_sipAuthorization = copy(auth);
	}

	/**
	 * Gets the SIP-Authorization value.
	 * 
	 * @return The SIP-Authorization value.
	 */
	public byte[] getSIPAuthorization() {
		return copy(_sipAuthorization);
	}

	/**
	 * Sets the SIP-Authentication-Context value.
	 * 
	 * @param authContext The SIP-Authentication-Context value.
	 */
	public void setSIPAuthenticationContext(byte[] authContext) {
		_authenticationContext = copy(authContext);
	}

	/**
	 * Gets the SIP-Authentication-Context value.
	 * 
	 * @return The SIP-Authentication-Context value.
	 */
	public byte[] getSIPAuthenticationContext() {
		return copy(_authenticationContext);
	}

	/**
	 * Sets the Confidentiality-Key (CK).
	 * 
	 * @param key The Confidentiality-Key value.
	 */
	public void setConfidentialityKey(byte[] key) {
		_confidentialityKey = copy(key);
	}

	/**
	 * Gets the Confidentiality-Key (CK).
	 * 
	 * @return The Confidentiality-Key value.
	 */
	public byte[] getConfidentialityKey() {
		return copy(_confidentialityKey);
	}

	/**
	 * Sets the Integrity-Key (IK).
	 * 
	 * @param key The Integrity-Key value.
	 */
	public void setIntegrityKey(byte[] key) {
		_integrityKey = copy(key);
	}

	/**
	 * Gets the Integrity-Key (IK).
	 * 
	 * @return The Integrity-Key value.
	 */
	public byte[] getIntegrityKey() {
		return copy(_integrityKey);
	}

	/**
	 * Sets the SIP-Digest-Authenticate value.
	 * 
	 * @param sipDigestAuthenticate The value.
	 */
	public void setSipDigestAuthenticate(SipDigestAuthenticate sipDigestAuthenticate) {
		_sipDigestAuthenticate = sipDigestAuthenticate;
	}

	/**
	 * Sets the SIP-Digest-Authenticate value.
	 * 
	 * @return The SIP-Digest-Authenticate value.
	 */
	public SipDigestAuthenticate getSipDigestAuthenticate() {
		return _sipDigestAuthenticate;
	}

	/**
	 * Gets the framed-IP-Address value.
	 * 
	 * @return The framed-IP-Address value.
	 */
	public final byte[] getFramedIPAddress() {
		return copy(_framedIPAddress);
	}

	/**
	 * Sets the framed-IP-Address value.
	 * 
	 * @param framedIPAddress The framed-IP-Address value.
	 */
	public final void setFramedIPAddress(byte[] framedIPAddress) {
		_framedIPAddress = copy(framedIPAddress);
	}

	/**
	 * Gets the framedIPv6Prefix.
	 * 
	 * @return The framedIPv6Prefix.
	 */
	public final byte[] getFramedIPv6Prefix() {
		return copy(_framedIPv6Prefix);
	}

	/**
	 * Sets the framedIPv6Prefix.
	 * 
	 * @param framedIPv6Prefix The framedIPv6Prefix.
	 */
	public final void setFramedIPv6Prefix(byte[] framedIPv6Prefix) {
		_framedIPv6Prefix = copy(framedIPv6Prefix);
	}

	/**
	 * Gets the Framed-Interface-Id.
	 * 
	 * @return The Framed-Interface-Id value.
	 */
	public final BigInteger getFramedInterfaceId() {
		return _framedInterfaceId;
	}

	/**
	 * Sets the Framed-Interface-Id.
	 * 
	 * @param framedInterfaceId The Framed-Interface-Id value.
	 */
	public final void setFramedInterfaceId(BigInteger framedInterfaceId) {
		_framedInterfaceId = framedInterfaceId;
	}

	/**
	 * Gets the Line-Identifier list.
	 * 
	 * @return The line identifier list.
	 */
	public final List<byte[]> getLineIdentifiers() {
		if (_lineIdentifiers == null) {
			return null;
		}

		List<byte[]> res = new ArrayList<byte[]>();
		for (byte[] id : _lineIdentifiers) {
			res.add(copy(id));
		}
		return res;
	}

	/**
	 * Sets the Line-Identifier list.
	 * 
	 * @param lineIdentifiers The Line-Identifier list.
	 */
	public final void setLineIdentifiers(List<byte[]> lineIdentifiers) {
		if (lineIdentifiers != null) {
			_lineIdentifiers = new ArrayList<byte[]>();
			for (byte[] value : lineIdentifiers) {
				_lineIdentifiers.add(copy(value));
			}
		} else {
			_lineIdentifiers = null;
		}
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
