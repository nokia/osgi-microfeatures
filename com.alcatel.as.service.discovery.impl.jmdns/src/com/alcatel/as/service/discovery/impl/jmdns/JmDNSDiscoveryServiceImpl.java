// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.discovery.impl.jmdns;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.shutdown.Shutdownable;
import com.alcatel.as.service.shutdown.Shutdown;

/**
 * JmDNSDiscovery service implementation. 
 * 
 * When local Advertisement is registered in the OSGi registry, it will be:
 *   1) re-registered using some required attributes (like provider=JmDNS) in order to notify other listeners in 
 *   the same jvm.
 *   2) published to other remote Jvms using JmDNS.
 *   
 * This implementation internally uses a "_asrsrv._tcp.asr. + external cluster addr" service type.
 * 
 * Since jmdns API is blocking, we configure our DM component as a concurrent component that will be handled in a dedicated
 * ASR IO blocking executor Queue. This means that all component lifecycle and dependency callbacks will be scheduled in the 
 * dedicated queue (running in the IO threadpool).
 * All JmDnscallbacks are also scheduled in the same queue, in order to avoid manual synchronization.
 * 
 * FIXME: Add counter for number of registered service, number of filter set, ...
 */
@Component(provides = {Shutdownable.class}, properties = { 
        @Property(name = "asr.component.parallel", value = "true"),
        @Property(name = "asr.component.cpubound", value = "false")
})
public class JmDNSDiscoveryServiceImpl implements ServiceListener, Shutdownable
{
    /**
     * We wait for this service before starting, making sure log4j is initialized before us.
     */
    private LogService _log;

    /**
     * The JmDNS Api.
     */
    private JmDNS _jmdns = null;

    /**
     * The unique id four our running Jvm. Retrieved from system properties.
     */
    private String _myid = null;

    /* cluster name */
    private String _clusterName = null;
    
    /**
     * The domain part of our service type (starts with a "._tcp.asr" constants, and is followed by our cluster external ip.
     */
    private String DOMAIN;

    /**
     * A special object is registered in the OSGi registry with that property, in order to indicate that all existing remote services 
     * have been registered locally in the OSGi registry
     */
    private final static String INITIAL_ADVERTISEMENT = "as.service.reporter.initial.advertisements";
    
    /**
     * The special initial object registration.
     */
    private ServiceRegistration<?> _initialRegistration = null;

    /**
     *  Map of infoKey -> OSGi registrations, or a string for "in progress" operations.
     */
    private Hashtable<String, ServiceRegistration<Advertisement>> _registrations = new Hashtable<>();

    /**
     *  Map of infoKey -> Remote JmDNS service 
     */
    private Hashtable<String, String> _remoteRegistrations = new Hashtable<>();

    /**
     * Map(instanceName -> infoKey) to keep track of restarted instances
     */
    private Hashtable<String, String> _instances = new Hashtable<String, String>();

    /**
     * Our DM component executor queue (our component is started from the IO threadpool in the PlatformExecutors).
     * This queue is used to serialize all jmdns events to our own component queue. it is used to ensure thread safety between our 
     * component service dependency callbacks, and the jmdns service listener callbacks.
     */
    private Executor _queue;

    /**
     * Context needed to register local advertisements.
     */
    @Inject
    BundleContext _bundleContext;

    /**
     * System configuration.
     */
    @ServiceDependency(filter = "(service.pid=system)")
    private Dictionary<String, String> _system;

    /**
     * ASR PlatformExecutors service, using to retrieve the queue that is automatically created for our component.
     * We'll retrieve our queue from our @Start method.
     */
    @ServiceDependency
    private PlatformExecutors _pfexecs;

    /**
     * Log factory. We wait for this service to make sure log4j is really initialized.
     */
    @ServiceDependency
    private LogServiceFactory _logFactory;

    /**
     * JmDns Weith. TODO why using 12 by default ?
     */
    private static final int DEFAULT_WEIGHT = 12;
    
    /**
     * JmDns service priority. TODO why are we using 1 by default.
     */
    private static final int DEFAULT_PRIORITY = 1;

    /**
     * Separator for instance id / module name
     */
    private static final String NAME_SEP = "-";

    private ArrayList<ServiceInfo> proxyAndWebAppList = new ArrayList<ServiceInfo>();
    
    /**
     * default constants.
     */
    private static final String DEFAULT_CONSTANT[] = { 
            ConfigConstants.PLATFORM_NAME,
            ConfigConstants.PLATFORM_ID,
            ConfigConstants.GROUP_NAME, 
            ConfigConstants.GROUP_ID, 
            ConfigConstants.COMPONENT_NAME,
            ConfigConstants.COMPONENT_ID, 
            ConfigConstants.INSTANCE_NAME, 
            ConfigConstants.INSTANCE_ID,
            ConfigConstants.HOST_NAME
    };

    /**
     * All required dependencies are injected. We can now start our service. 
     * Current executor thread = a dedicated PlatformExecutor queue which is running within the IO blocking threadpool.
     */
    @Start
    void start() {
        try {
            //
            // Retrieve our component queue. We decorate the queue with an executor that will always 
            // schedule runnables immediately if the current thread is our current executor queue.
            //
            _queue = _pfexecs.getCurrentThreadContext().getCurrentExecutor().toExecutor(ExecutorPolicy.INLINE);

            //
            // Initialize log4j. Note logger "javax.jmdns.impl.JmDNSImpl" for JmDNS internals
            //
            _log = _logFactory.getLogger("as.service.discovery.jmdns");
            new Log4jHandler(_logFactory);

	    /* Config first choice, shell environement second */
	    _clusterName = _system.get(ConfigConstants.CLUSTER_NAME);
	    if (_clusterName == null) _clusterName = System.getenv("CLUSTER_NAME");

	    String EIP = (String) _system.get(ConfigConstants.ADMIN_EXTERNAL_IP);

	    /* Fall back to cluster name ? */
	    if (EIP == null) EIP = _clusterName;

	    /* We need a deterministic naming for this. Cannot be null and/or random */
	    if (EIP == null) {
		_log.error("Cannot initialize JmDNS discovery service. No system/environment.CLUSTER_NAME, system/environment.EXTERNAL_IP or shell environment variable CLUSTER_NAME available");
		_jmdns = null;
		return;
	    }

            // Make our DOMAIN unique, in order to protect against other cluster publish.
            DOMAIN = EIP.replaceAll("\\.", "") + ".";
            
            // Jvm unique instance id.
            _myid = _system.get(ConfigConstants.INSTANCE_ID);

	    // Retrieve our component queue. We decorate the queue with an executor that will always 
            // schedule runnables immediately if the current thread is our current executor queue.
            _queue = _pfexecs.getCurrentThreadContext().getCurrentExecutor().toExecutor(ExecutorPolicy.INLINE);

            // Initialize Jmdns with out host addr and host name.
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = InetAddress.getByName(addr.getHostName()).toString();
            _jmdns = JmDNS.create(addr, hostname);

            //
            // Add a service listener on the asr service type that is unique for our current cluster.
            // Notice that when adding our service listener, our serviceAdded/serviceResolved methods will be called
            // synchronously for all existing remote services that are currently cached by jmdns.
            //
            _log.info("Activating JmDNS multicast. My instance id: %s. Running on domain: %s [%s]", _myid, getASRServiceType(),
                _system.get(ConfigConstants.ADMIN_EXTERNAL_IP));            
            _jmdns.addServiceListener(getASRServiceType(), this); // will synchronously call serviceAdded, serviceRemoved

	    /* List all remote advert and populate local registry before declaring as ready */
	    int nbFound = getRemoteAdverts();
	    
            //
            // All existing cached services have been injected in our listener: now notify application that existing 
            // remote services are registered.
            //
	    
            initialRegistrationDone();
	    _log.warn("JmDNS successfully initialized. Have found " + nbFound + " remote services");

        }
        catch (Throwable e) {
            _log.error("JmDNS activatation failure.", e);
            _jmdns = null;
        }
    }

    private int getRemoteAdverts() {
	/* Synchronous call. Blocking ... */
	ServiceInfo[] services = _jmdns.list(getASRServiceType());
	int idx = 0;
	for (ServiceInfo info : services) {
	    String infoKey = info.getName();
	    if (infoKey != null) {
		if (checkAvailableServiceInfo(info)) {
		    printEventDetail("Service found ", null, info);
		    serviceEvent(false, null, info, true);
		    idx++;
		}
	    }
	}
	return(idx);
    }

    /**
     * Our bundle is being stopped. Shutdown our service.
     * All adverts have been automatically unbound (because our component is stopping).
     * Current = any thread.
     */
    @Stop
    void stop() {
        _log.warn("unloading jmdns discovery service");
        try {
            // Free jmdns resources 
            if (_jmdns != null)
                _jmdns.close();
        }
        catch (Exception e) {
            _log.warn("Cannot close jmdns", e);
        }
        
        _registrations.clear(); // services unregistered by DM
        _instances.clear();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // local Advertisements 
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * listen for local Advertisements in the white board and publish them to JmDNS
     * Current executor thread = a dedicated PlatformExecutor queue which is running within the IO blocking threadpool.
     */
    @ServiceDependency(required = false, removed = "unpublish", filter = "(!(provider=*))")
    public void publish(Map<String, Object> serviceProperties, Advertisement advert) throws IOException {
        publishEvent(serviceProperties, advert, true);
    }

    public void unpublish(Map<String, Object> serviceProperties, Advertisement advert) throws IOException {
        publishEvent(serviceProperties, advert, false);
    }

    /**
     * Thread safe, we are executed from our component queue.
     */
    private void publishEvent(final Map<String, Object> advertProperties, final Advertisement advert, final boolean pub) throws IOException {
        _log.info("%s advert %s, properties=%s", pub ? "publishing" : "unpublishing", advert, advertProperties);

        if (_jmdns == null) {
            _log.warn("JmDNS not activated ... See previous error.");
            return;
        }
	
	Map<String, String> serviceProperties = publishedServiceProperties(advertProperties, advert);
	
	/* Check advert type */
	String advertType =(String)  advertProperties.get(ConfigConstants.ADVERT_TYPE);

	boolean isWebApp = ((advertType != null) && (advertType.equals(ConfigConstants.ADVERT_PROXY_TYPE) || advertType.equals(ConfigConstants.ADVERT_WEBAPP_TYPE)));
	
	ServiceInfo info = null;
	String infoKey = null;
	
	if (isWebApp) {
	    info = ServiceInfo.create(getType(advertType),
				      instanceId(serviceProperties),
				      advert.getPort(),
				      DEFAULT_WEIGHT,
				      DEFAULT_PRIORITY,
				      true,
				      serviceProperties);

	    /* Add it to the list of registered service that need to be cleaned */
	    proxyAndWebAppList.add(info);
	    _log.debug("%sPublishing on jmdns %s [%s]", (pub ? "" : "Un-"), advert.toString(), serviceProperties.toString());
	} else {
	    infoKey = buildInfoKeyFromProperties(serviceProperties);
	    /* See JmDNS javadoc. The key is the lower case qualified name */
	    if ((infoKey == null) || (pub && (_registrations.get(infoKey) != null))) {
		_log.debug("Already have a published event for %s", infoKey);
		return; //don't republish our own registrations or not valid advertisement
	    }
	    
	    _log.debug("White board event on [" + infoKey + "]. " + (pub ? "Register" : "Unregister")
		       + " from instance id [" + serviceProperties.get(ConfigConstants.INSTANCE_ID) + "]");
	    
	    info = ServiceInfo.create(getASRServiceType(),
				      instanceId(serviceProperties),
				      advert.getPort(),
				      DEFAULT_WEIGHT,
				      DEFAULT_PRIORITY,
				      true,
				      serviceProperties);
	    
	    _log.debug("%sPublishing on jmdns %s/%s [%s]", (pub ? "" : "Un-"), infoKey, advert.toString(),
		       serviceProperties.toString());
	} 

        if (pub)
            try {
                // Add it to JmDNS cloud 
                _jmdns.registerService(info);
		// And register immediately back to whiteboard (adding missing system information)
                if (! isWebApp) registerToWhiteboard(infoKey, info);
            }
            catch (Exception e) {
                _log.warn("Publish failed! [%s]", e, infoKey);
            }
        else {
            // Remove it to from JmDNS cloud
            _jmdns.unregisterService(info);
            // and immediately from white board the same advert with a provider
            if (! isWebApp) unregisterFromWhiteboard(infoKey);
        }
    }

    private boolean alreadyRegistered(ServiceInfo info) {
	if (info != null) {
	    String infoKey = info.getName();
	    if ((infoKey != null) && (_remoteRegistrations.get(infoKey) != null)) return true;
	}
	return false;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // remote Advertisements 
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /* 
     * ServiceListener implementation :
     * A remote service is discovered: publish it as local Advertisement.
     *
     * If the added service does not contain necessary properties request to resolved it.
     * Current thread = JmDNS thread pool. We have to redispatch to our component queue.
     */
    @Override
    public void serviceAdded(final ServiceEvent event) {
        _queue.execute(new Runnable() {
		public void run() {
		    ServiceInfo info = event.getInfo();
		    _log.debug("Service added: " + event.getName());
		    if (checkAvailableServiceInfo(info)) {
			serviceEvent(true, event, info, true);
		    } else {
			info = _jmdns.getServiceInfo(event.getType(), event.getName());
			if (checkAvailableServiceInfo(info)) {
			    serviceEvent(true, event, info, true);
			} else {
			    _log.debug("Requesting resolution for [%s/%s]", event.getType(), event.getName());
			    try {
				Thread.sleep(2000);
			    } catch (Exception e) {}
			    _jmdns.requestServiceInfo(event.getType(), event.getName(), true);
			}
		    }
		}
	    });
    }

    /**
     * A previoulsy added service is now fully resolved.
     * Current thread = JmDNS thread pool. We have to redispatch to our component queue.
     */
    @Override
    public void serviceResolved(final ServiceEvent event) {
        _queue.execute(new Runnable() {
            public void run() {
                ServiceInfo info = event.getInfo();
                _log.debug("Service resolved: " + event.getName());
                if (checkAvailableServiceInfo(info)) {
                    serviceEvent(true, event, info, true);
                } else {
		    info = _jmdns.getServiceInfo(event.getType(), event.getName());
		    if (checkAvailableServiceInfo(info)) {
			serviceEvent(true, event, info, true);
		    }
		    _jmdns.requestServiceInfo(event.getType(), event.getName(), true);
		}
            }
        });
    }

    /**
     * Current thread = JmDNS thread pool. We have to redispatch to our component queue.
     */
    @Override
    public void serviceRemoved(final ServiceEvent event) {
        _queue.execute(new Runnable() {
            public void run() {
                _log.debug("Service removed: " + event.getName());
                try {
                    serviceEvent(false, event, event.getInfo(), false);
                }
                catch (Exception e) {
                    _log.warn("serviceRemoved", e);
                }
            }
        });
    }

    /**
     * Current thread = Component queue.
     */
    private void serviceEvent(final boolean checkIt, final ServiceEvent event, final ServiceInfo info, final boolean added) {
        printEventDetail((added) ? "Service added" : "Service removed", event, info);
        String infoKey = info.getName();
	
        if (infoKey == null) {
            /* Nore ASR or bad service advertissement. Trash it */
            _log.debug("Skipping un-supported jmdns %s on %s. Missing mandatory properties.",
		       (added) ? "serviceResolved" : "serviceRemoved", (event != null) ? event.toString() : infoKey);
            return;
        }
	
	if (checkIt) {
	    if (alreadyRegistered(info)) {
		_log.info("Remote service [" + infoKey + "] already registered. Skipping");
		return;
	    }
	}
	
        // Ignore if this service event comes from ourself.
        Dictionary<String, String> props = remoteServiceProperties(info);

	String eventInstanceId = getInstanceId(infoKey);

	_log.debug("checking self service event: event id=%s, self id=%s", eventInstanceId, _myid);
        
        if (_myid.equals(eventInstanceId)) {
	    _log.debug("Ignoring service event which comes from ourself: " + infoKey);
	    return;
        }

        _log.info("received %s advert: %s", added ? "active" : "inactive", props);

        if (added) {
	    /* Track it */
	    _remoteRegistrations.put(infoKey, infoKey);
	    _log.info("Tracking added on " + infoKey);
	    registerToWhiteboard(infoKey, info);
	    _log.info("Added done on " + infoKey);
        }
        else {
	    _log.info("Remove scheduled on " + infoKey);
            unregisterFromWhiteboard(infoKey);
	    _remoteRegistrations.remove(infoKey);
	    _log.info("Tracking removed on " + infoKey);
        }
	
	_log.debug("Service [%s] %s white board", infoKey, (added ? "registered in" : "unregistered from"));
    }

    public void shutdown(Shutdown shutdown) {
	/* We must clean the OSGi registry with plugins and proxy */
	for (Iterator<ServiceInfo> it = proxyAndWebAppList.iterator(); it.hasNext();) {
            ServiceInfo info = it.next();
	    _jmdns.unregisterService(info);
	    _log.debug("Cleaning "+info);
	    it.remove();
        }
	shutdown.done(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helpers
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * register Advertisement to white board.
     * Current thread = Component queue.
     */
    private boolean registerToWhiteboard(String infoKey, ServiceInfo info)
    {
        if (_registrations.get(infoKey) != null)
        {
            _log.debug("Do not re-register resolved service [%s]. Was already immediately registered.", infoKey);
            return false;
        }

        Dictionary<String, String> props = remoteServiceProperties(info);

        if ((props.get(ConfigConstants.SERVICE_IP) == null) || (props.get(ConfigConstants.SERVICE_PORT) == null))
        {
            _log.warn("Missing mandatory service properties %s and %s. Trashing event", ConfigConstants.SERVICE_IP,
		      ConfigConstants.SERVICE_PORT);
            return false;
        }

        // same instance.name and different instance.id means a restart;
        // cleanup previous registrations if any, otherwise they will only expire atfer 30mins (Jmdns cache)
        String instanceName = instanceName(props);
        String previousKey = _instances.remove(instanceName);
        if (previousKey != null)
        {
            ServiceRegistration<Advertisement> reg = _registrations.remove(previousKey);
            _log.info("Cleanup previous registration %s for restarted instance %s", previousKey, infoKey);
            if (reg != null) {
                try {
                    reg.unregister();
                } catch (IllegalStateException e) { /* already unregistered */ }
            }
        }

        Advertisement advert = new Advertisement(props.get(ConfigConstants.SERVICE_IP),
            props.get(ConfigConstants.SERVICE_PORT));
        _log.debug("Registering [%s] locally as %s %s", infoKey, advert, props);

        // For advertisement, take properties provided by service and not the one embedded in JmDNS API. 
        // We do not trust JmDNS for the source ip of publication, 
        // since the host publishing the information can be assign to an ip different from the one on which the real service is listening 
        _registrations.put(infoKey, _bundleContext.registerService(Advertisement.class, advert, props));

        if (!_myid.equals(props.get(ConfigConstants.INSTANCE_ID)))
            _instances.put(instanceName, infoKey);
        return true;
    }

    /** 
     * unregister Advertisement from white board.
     * Current thread = component queue. 
     */
    @SuppressWarnings("unchecked")
    private void unregisterFromWhiteboard(String infoKey)
    {
        Object reg = _registrations.remove(infoKey);
        if ((reg != null) && (reg instanceof ServiceRegistration))
        {
            ((ServiceRegistration<Object>) reg).unregister();
        }
        else
        {
            _log.debug("[%s] registration found for removed advertisement [%s]. Skipping unregister", reg, infoKey);
        }
        // cleanup entry in instances
        for (Iterator<Map.Entry<String, String>> it = _instances.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry<String, String> e = it.next();
            if (e.getValue().equals(infoKey))
            {
                it.remove();
                break;
            }
        }
    }

    /** 
     * Check that service info contains mandatories stuff
     * Mandatory: IP/PORT
     * Optionnal ConfigConstants.MODULE_NAME (thread number for IOH.
     * Current thread = component queue.
     * @return false if mandatory keys are not set
     */
    private boolean checkAvailableServiceInfo(ServiceInfo info)
    {
        if ((info == null) ||
	    (info.getPropertyString(ConfigConstants.SERVICE_IP) == null) ||
	    (info.getPropertyString(ConfigConstants.SERVICE_PORT) == null) ||
	    (info.getPropertyString(ConfigConstants.GROUP_NAME) == null) 
	    ) return false;
	return(true);
    }

    /** 
     * Rebuild info key from a service properties. Something like hs1._257._tcp.asr.
     * Mandatory: ConfigConstants.INSTANCE_NAME, ConfigConstants.MODULE_ID
     * Current thread = component queue.
     * @return null if mandatory keys are not set
     **/
    private String buildInfoKeyFromProperties(Map<String, String> serviceProperties)
    {
        /* Sanity check */
        if ((serviceProperties.get(ConfigConstants.INSTANCE_NAME) == null)
            || (serviceProperties.get(ConfigConstants.MODULE_ID) == null))
        {
            return null;
        }
        return instanceId(serviceProperties);
    }

    private String instanceId(Map<String, String> serviceProperties)
    {
	return(buildInstanceId(serviceProperties.get(ConfigConstants.INSTANCE_ID),
			       serviceProperties.get(ConfigConstants.MODULE_NAME)));
    }

    private String getInstanceId(String s) {
	int idx = s.indexOf(NAME_SEP);
	return((idx == -1) ? s : s.substring(0, idx));
    }
    
    private String buildInstanceId(String i, String subName) {
	return((subName != null) ? i + NAME_SEP + subName : i);
    }
    
    private String instanceName(Dictionary<String, String> serviceProperties)
    {
        String subName = serviceProperties.get(ConfigConstants.MODULE_NAME);
        return new StringBuilder(serviceProperties.get(ConfigConstants.PLATFORM_NAME)).append(
            serviceProperties.get(ConfigConstants.GROUP_NAME)).append(
                serviceProperties.get(ConfigConstants.COMPONENT_NAME)).append(
                    serviceProperties.get(ConfigConstants.INSTANCE_NAME)).append(
                        (subName != null) ? subName : "").toString();
    }

    /**
     * Build properties to be published out to JMDNS
     * @param serviceProperties comes for OSGi registration
     * @return a Map containing agregated Map. If serviceProperties Map does not contains ConfigConstants.xxx properties, it's retrieved from dico System
    **/
    private Map<String, String> publishedServiceProperties(Map<String, Object> serviceProperties, Advertisement advert)
    {
        Map<String, String> advertProperties = new HashMap<String, String>();
        // first make sure everything is a string.. or jmdns will complain
        for (Map.Entry<String, ?> e : serviceProperties.entrySet())
            advertProperties.put(e.getKey(), e.getValue().toString());

        // Fall back to System Properties, if they are not set 
        for (String name : DEFAULT_CONSTANT)
        {
            if ((advertProperties.get(name) == null) && (_system.get(name) != null))
            {
                advertProperties.put(name, _system.get(name));
            }
        }

        // Add ip and port if not set
        if (advertProperties.get(ConfigConstants.SERVICE_IP) == null)
        {
            advertProperties.put(ConfigConstants.SERVICE_IP, advert.getIp());
        }

        if (advertProperties.get(ConfigConstants.SERVICE_PORT) == null)
        {
            advertProperties.put(ConfigConstants.SERVICE_PORT, Integer.toString(advert.getPort()));
        }

        return advertProperties;
    }

    /**
     * Build properties for remote services to be registered in OSGi
     * @param info the ServiceInfo of the JMDNS resolution
     * @return a Dictionary containing the OSGi service properties
    **/
    private Dictionary<String, String> remoteServiceProperties(ServiceInfo info)
    {
        Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
        String moduleName = info.getPropertyString(ConfigConstants.MODULE_NAME);
        Enumeration<String> props = info.getPropertyNames();
        while (props.hasMoreElements())
        {
            String name = props.nextElement();
            serviceProperties.put(name, info.getPropertyString(name));
        }

        String instanceName = serviceProperties.get(ConfigConstants.INSTANCE_NAME);
        if (instanceName == null)
        {
            serviceProperties.put(ConfigConstants.INSTANCE_NAME, info.getName());
        }
        else
        {
            if (moduleName != null)
            {
                serviceProperties.put(ConfigConstants.INSTANCE_NAME, instanceName);
            }
        }

        if (serviceProperties.get(ConfigConstants.MODULE_ID) == null)
        {
            serviceProperties.put(ConfigConstants.MODULE_ID, info.getProtocol());
        }

        if (serviceProperties.get("provider") == null)
        {
            serviceProperties.put("provider", "JmDNS");
        }

        _log.debug("ServiceInfo properties: %s", serviceProperties.toString());

        return serviceProperties;
    }
    
    private void printEventDetail(String header, ServiceEvent event, ServiceInfo info) {
	StringBuilder sb;
	if (event != null) sb = new StringBuilder(header + " " + event.getType() + "/" + event.getName());
	else sb = new StringBuilder(header);
	
        if (info != null) {
            try {
                sb.append(info.getType() + "/" + info.getName() + " Port [" + info.getPort() + "]");
		InetAddress[] ips = info.getInetAddresses();
		 if (ips != null) {
		     for (int i = 0; i < ips.length; i++) {
			 sb.append("\n\tHost ip [" + i + "] = " + ips[i].getHostAddress());
		     }
		 }
		 
		 sb.append("\nhasData: " + info.hasData());
		 
		 Enumeration<String> props = info.getPropertyNames();
                if (props != null) {
                    sb.append(" {");
                    while (props.hasMoreElements()) {
                        String name = props.nextElement();
                        sb.append("\n\t" + name + ":" + info.getPropertyString(name));
                    }
                    sb.append("\n}\n");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            sb.append(". Empty event info !");
        }
        _log.debug("Event info: %s", sb.toString());
    }    
    
    private String getASRServiceType() {
        return ConfigConstants.ADVERT_SERVICE_TYPE + "." + DOMAIN;
    }

    private String getType(String advertType) {
	return advertType + "."+ _clusterName + ".";
    }
    
    private void initialRegistrationDone() {
        if (_initialRegistration == null) {
            Hashtable<String, Object> properties = new Hashtable<>();
            properties.put(INITIAL_ADVERTISEMENT, "done");
            _initialRegistration = _bundleContext.registerService(Object.class.getName(), new Object(), properties);
        }        
    }
}
