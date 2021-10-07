package com.alcatel.as.service.management;

import com.alcatel.as.util.config.ConfigConstants;

/**
 * Any component which needs to be managed can provide this
 * {@link ManagedService} interface into the OSGi registry. The component will
 * then be managed like the following:
 * <ul>
 * <li>if the OSGi service property {@link ENABLE_STATISTIC} is set to true,
 * then the component will be invoked in its getStatistics method, which must
 * return the counters defined in the component counter descriptor.
 * <li>if the OSGi service property {@link ENABLE_COMMAND} is set to true, then
 * the component will be invoked in its messageReceived/adminMessageReceived
 * methods.
 * <li>if the OSGi service property {@link ENABLE_DISCOVERY} is set to true,
 * then the component will be invoked in its advertisementReceived method in
 * order to be advertised about the address of all other peers which are running
 * within the platform.</li>
 * 
 * @internal This interface is private and may change in the future.
 * @deprecated
 */
public interface ManagedService {

	/**
	 * Service property indicating that we provide our statistic through our
	 * getStatistics method.
	 */
	final static String ENABLE_STATISTIC = "enableStatistic";

	/**
	 * Service property indicating that we accept remote commands through our
	 * applicationCommandReceived method.
	 */
	final static String ENABLE_COMMAND = "enableCommand";

	/**
	 * Service property indicating that we need to track peer address
	 * advertisement events.
	 */
	final static String ENABLE_DISCOVERY = "enableDiscovery";

	/**
	 * Service property indicating the major version of our ManagedService.
	 */
	final static String MAJOR_VERSION = "version.major";

	/**
	 * Service property indicating the minor version of our ManagedService.
	 */
	final static String MINOR_VERSION = "version.minor";

	/**
	 * Service property indicating the component name. By default, the
	 * "component.name" system property will be used.
	 */
	final static String COMPONENT_NAME = "componentName";

	/**
	 * Service property indicating the instance name of the managed component
	 */
	final static String INSTANCE_NAME = "instanceName";

	/**
	 * Service property indicating the component id of the managed component
	 */
	final static String COMPONENT_ID = "componentId";

	/**
	 * Service property indicating the parent instance name of the managed
	 * component.
	 */
	final static String PARENT = "parent";

	/**
	 * Indicates if this service is really active, or in a standby mode. (default
	 * is false). Components in standby mode will be seen as started, but with a
	 * special color in the web administration.
	 */
	boolean onStandby();

	/**
	 * Provide the current set of statistical counters of the managed service.
	 * This method is only used if the OSGi service property
	 * {@link ENABLE_STATISTIC} is set to true..
	 * 
	 * @return Set of statistical counters of the application.
	 */
	int[] getStatistics();

	/**
	 * Notify this managed service about the advertisement of a remote application
	 * address. Only called when the {@link ENABLE_ADDRESSING} OSGi service
	 * property is set to true.
	 * 
	 * @param hostName Name of the host on which the remote application instance
	 *          is running
	 * @param applicationName Remote application name
	 * @param applicationIdentifier Remote application identifier
	 * @param instanceName Remote application instance name
	 * @param ipAddresses List of IP addresses this remote application instance
	 *          listens on
	 * @param ports Associated list of port numbers
	 * @param active true if the application is active, false if it is going down
	 */
	void advertisementReceived(Advertisement advertisement);

	/**
	 * To be implemented if the component needs to advertise addresses to others.
	 * Return null other wise.
	 * 
	 * @return an address to be advertised, or null.
	 */
	public Address getListeningAddress();

	/**
	 * Notify this managed service that a message has been received from the web
	 * admin. Only called if the OSGi service property {@link ENABLE_COMMAND} is
	 * set to true.
	 * 
	 * @param code Message code (see codes
	 * @param replyTo Reply to address, if needed for this command
	 * @param intParams the set of integer message parameters.
	 * @param strParams the set of string message parameters.
	 */
	void adminCommandReceived(int code, String replyTo, int[] intParams, String[] strParams);

	/**
	 * Notify this managed service that a message has been received from another
	 * peer. Only called if the OSGi service property {@link ENABLE_COMMAND} is
	 * set to true. The peer can send messages using the {@link ManagementService}
	 * service.
	 * 
	 * @param code Message code
	 * @param replyTo Reply to address, if needed for this command
	 * @param data The data.
	 * @param off The offset in the data.
	 * @param len The length.
	 */
	void commandReceived(int code, String replyTo, byte[] data, int off, int len);
}
