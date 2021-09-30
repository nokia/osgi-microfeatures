package com.nextenso.proxylet.diameter;

/**
 * The connection filter.
 * 
 * The connection filter objects are used to know if incoming connections are
 * allowed or not. They are managed in the DiameterFiletrTable.
 * 
 * @see DiameterFilterTable
 */
public class DiameterConnectionFilter {

	private String _host;
	private String _realm;
	private int _encryption;

	private DiameterConnectionFilter() {}

	/**
	 * Creates a new connection filter.
	 * 
	 * @param originHost The origin host to be compared to ( "*" means
	 *          any and null means not tested).
	 * @param originRealm The origin realm to be compared to ("*" means
	 *          any and null means not tested).
	 * @param encryptionLevel The encryption level (one out of DiameterPeer
	 *          .ENC_LEVEL values).
	 * @see DiameterPeer#ENC_LEVEL_FORBIDDEN
	 * @see DiameterPeer#ENC_LEVEL_OPTIONAL
	 * @see DiameterPeer#ENC_LEVEL_PREFERRED
	 * @see DiameterPeer#ENC_LEVEL_REQUIRED
	 */

	public DiameterConnectionFilter(String originHost, String originRealm, int encryptionLevel) {
		this();
		_host = originHost;
		_realm = originRealm;
		_encryption = encryptionLevel;
	}

	/**
	 * Gets the host.
	 * 
	 * @return The host to be checked.
	 */
	public String getHost() {
		return _host;
	}

	/**
	 * Gets the realm.
	 * 
	 * @return The realm to be checked.
	 */
	public String getRealm() {
		return _realm;
	}

	/**
	 * Gets the encryption level.
	 * 
	 * @return The encryption level to be checked.
	 */
	public int getEncryptionLevel() {
		return _encryption;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("ConnectionFilter:");
		res.append(" host=").append(getHost());
		res.append(", realm=").append(getRealm());
		res.append(", encryption level=").append(getEncryptionLevel());
		return res.toString();
	}

}
