package com.alcatel.as.service.management;

/**
 * This class is a helper for implementing the ManagedService interface.
 * 
 * @see ManagedService
 * @internal
 * @deprecated
 */
public class ManagedServiceSupport
		implements ManagedService {

	/**
	 * Provides the reporter API with the current set of statistical counters of
	 * the application.
	 * <p>
	 * This method is called by the API on a regular basis to retrieve the current
	 * set of statistical counters of the application, thus allowing the API to
	 * report this information to management tools. The list of counters reported
	 * by the application must match the set of counters defined for this
	 * application in the associated application descriptor.
	 * <p>
	 * This method is only called if the application called the
	 * <b>doStatisticsReporting</b> method of the API.
	 * 
	 * @return A set of statistical counters of the application.
	 */
	public int[] getStatistics() {
		throw new RuntimeException("This method should be overriden!");
	}

	/**
	 * Indicates if this service is really active, or in a standby mode. (default
	 * is false). Components in standby mode will be seen as started, but with a
	 * special color in the web administration.
	 */
	public boolean onStandby() {
		return false;
	}

	/**
	 * Notifies the application when addressing information is received.
	 * <p>
	 * Provided the application has called the <b>getAddressingInformation</b>
	 * method of the reporting API, this method is called by the API whenever it
	 * receives addressing information about another application instance to
	 * provide this information to the application.
	 * 
	 * @param advert The advertisement
	 */
	public void advertisementReceived(Advertisement advert) {
		throw new RuntimeException("This method should be overriden!");
	}

	/**
	 * @see com.alcatel.as.service.management.ManagedService#commandReceived(int, java.lang.String, byte[], int, int)
	 */
	public void commandReceived(int code, String replyTo, byte[] data, int off, int len) {
		throw new RuntimeException("This method should be overriden!");
	}

	/**
	 * @see com.alcatel.as.service.management.ManagedService#adminCommandReceived(int, java.lang.String, int[], java.lang.String[])
	 */
	public void adminCommandReceived(int code, String replyTo, int[] intParams, String[] strParams) {
		throw new RuntimeException("This method should be overriden!");
	}

	/**
	 * to be implemented if the component needs to advertise addresses to others
	 * return null other wise
	 */
	public Address getListeningAddress() {
		return null;
	}
}
