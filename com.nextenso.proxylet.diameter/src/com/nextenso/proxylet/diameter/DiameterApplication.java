package com.nextenso.proxylet.diameter;

/**
 * The Diameter Application.
 */
public class DiameterApplication {

	private long _vendorId;
	private long _applicationId;
	private boolean _isAuthentication;

	/**
	 * Constructor for this class.
	 * 
	 * @param applicationId The application identifier.
	 * @param vendorId The vendor identifier.
	 * @param isAuthentication true if it is an authentication application, false
	 *          if it is an accounting application.
	 */
	public DiameterApplication(long applicationId, long vendorId, boolean isAuthentication) {
		_applicationId = applicationId;
		_vendorId = vendorId;
		_isAuthentication = isAuthentication;
	}

	/**
	 * Gets the application identifier.
	 * 
	 * @return The application identifier.
	 */
	public long getApplicationId() {
		return _applicationId;
	}

	/**
	 * Gets the vendor identifier.
	 * 
	 * @return The vendor identifier.
	 */
	public long getVendorId() {
		return _vendorId;
	}

	/**
	 * Indicates whether the application is vendor specific.
	 * 
	 * @return true if the application is vendor specific.
	 */
	public boolean isVendorSpecific() {
		return (_vendorId > 0);
	}

	/**
	 * Indicates whether the application is Authentication or Accounting.
	 * 
	 * @return true if it is an authentication application, false if it is an
	 *         accounting application.
	 */
	public boolean isAuthentication() {
		return _isAuthentication;
	}

	/**
	 * Indicates whether the application is an accounting application.
	 * 
	 * @return true if it is an accounting application.
	 */
	public boolean isAccounting() {
		return !_isAuthentication;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return (int) ((isAuthentication () ? 1 : -1) * (getVendorId () ^ getApplicationId ()));
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DiameterApplication)) {
			return false;
		}
		DiameterApplication app = (DiameterApplication) obj;
		if (getApplicationId() != app.getApplicationId()) {
			return false;
		}
		if (getVendorId() != app.getVendorId()) {
			return false;
		}
		if (isAuthentication() != app.isAuthentication()) {
			return false;
		}
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("[Diameter Application id=");
		res.append(getApplicationId()).append(", vendor-id=").append(getVendorId());
		res.append(", is authentication=").append(isAuthentication());
		res.append(']');
		return res.toString();
	}

}
