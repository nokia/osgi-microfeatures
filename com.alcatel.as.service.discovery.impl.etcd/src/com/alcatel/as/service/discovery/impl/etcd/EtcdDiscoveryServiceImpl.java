package com.alcatel.as.service.discovery.impl.etcd;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.osgi.DictionaryToMap;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mousio.client.promises.ResponsePromise;
import mousio.client.promises.ResponsePromise.IsSimplePromiseResponseHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdErrorCode;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;

/**
 * EtcdDiscovery service implementation.
 * 
 * When local Advertisement is registered in the OSGi registry, it will be :
 * <br/>
 * 1) re-registered using some required attributes (like provider=Etcd) in order
 * to notify other listeners in the same jvm. <br/>
 * 2) published to other remote Jvms using Etcd.<br/>
 * <br/>
 * 
 * <i>Etcd4j is currently working with etcd-v2. For etcd-v3, import jetcd (no
 * stable release on July 2017)</i>
 */
@Component(provides=Object.class)
@Property(name = "asr.component.parallel", value = "true")
@Property(name = "asr.component.cpubound", value = "false")
@Property(name=CommandProcessor.COMMAND_SCOPE, value="asr.discovery.etcd")
@Property(name=CommandProcessor.COMMAND_FUNCTION, value="stop")
public class EtcdDiscoveryServiceImpl {

	/**
	 * We wait for this service before starting, making sure log4j is
	 * initialized before us.
	 */
	private LogService _log;

	/**
	 * The Etcd Api.
	 */
	private EtcdClient _etcd = null;

	/**
	 * Our DM component executor queue (our component is started from the IO
	 * threadpool in the PlatformExecutors). This queue is used to serialize all
	 * etcd events to our own component queue. it is used to ensure thread
	 * safety between our component service dependency callbacks, and the etcd
	 * service listener callbacks.
	 */
	private Executor _queue;

	/**
	 * The unique id for our running Jvm. Retrieved from system properties.
	 */
	private String _myid = null;

	/**
	 * Unique instance
	 */
	private String _myinstance = null;
	
	/**
	 * Flag used to deactivate the Etcd callback at bundle stopping step
	 */
	private volatile boolean _stopping = false;
	
	/**
	 * The updater is mandatory to keep alive the advert produced in our Jvm. If
	 * the Jvm is killed, our adverts won't remains on etcd server.
	 */
	private EtcdRegistrationUpdater _updater;
	
	/**
	 * UUID for jvm
	 */
	private String _jvmUUid;
	
	private int _etcdTtl;
	
	/**
	 * Context needed to register local advertisements.
	 */
	@Inject
	BundleContext _bundleContext;

	/**
	 * System configuration.
	 */
	private Dictionary<String, String> _system;

	/**
	 * ASR PlatformExecutors service, using to retrieve the queue that is
	 * automatically created for our component. We'll retrieve our queue from
	 * our @Start method.
	 */
	@ServiceDependency
	private PlatformExecutors _pfexecs;

	/**
     *  Map of infoKey -> OSGi registrations, or a string for "in progress" operations.
     */
    private Map<String, ServiceRegistration<Advertisement>> _registrations = new HashMap<>();
	
    /**
     * List of registration keys done in our JVM, store unique keys/Etcd Path. When an Advertisement
     * is received, it is stored to Etcd Server and the watcher must not respond to our own 
     * Advertisements
     */
    private Set<String> _ownRegistration = new HashSet<>();
    
	/**
	 * Log factory. We wait for this service to make sure log4j is really
	 * initialized.
	 */
	@ServiceDependency
	private LogServiceFactory _logFactory;
	
	/**
	 * Etcd Callback at any change on Etcd Server (set, post, delete...)
	 */
	private volatile ResponseListener _responseListener;

	/**
	 * Etcd configuration containing properties such as the address or the root path to watch on
	 */
	private EtcdConfiguration _configuration;

//	TODO private static final String DEFAULT_CONSTANT[] = {
//			
//	}
	
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
			ConfigConstants.MODULE_NAME,
			ConfigConstants.MODULE_ID,
			ConfigConstants.HOST_NAME };

	/**
	 * Separator for instance id / module name
	 */
	private static final String NAME_SEP = "/";

	@ConfigurationDependency(pid = "system")
	void updated(Dictionary<String, String> system) {
		_system = system;
	}
	
	@ConfigurationDependency
	void loadEtcdConf(EtcdConfiguration conf) {
		_configuration = conf;
	}

	/**
	 * All required dependencies are injected. We can now start our service.
	 * Current executor thread = a dedicated PlatformExecutor queue which is
	 * running within the IO blocking threadpool.
	 */
	@Start
	void start() {
		try {
			// Retrieve our component queue. We decorate the queue with an
			// executor that will always schedule runnables immediately if
			// the current thread is our current executor queue.
			_queue = _pfexecs.getCurrentThreadContext()
							 .getCurrentExecutor()
							 .toExecutor(ExecutorPolicy.INLINE);
			_etcdTtl = _configuration.getPublishTtlSeconds();
			_stopping = false;
			_log = _logFactory.getLogger("as.service.discovery.etcd");

			_responseListener = new ResponseListener();

			// Jvm unique instance id.
			_myid = _system.get(ConfigConstants.INSTANCE_ID);
			_jvmUUid = UUID.randomUUID().toString();
			_myinstance = _system.get(ConfigConstants.INSTANCE_NAME);
			_log.debug("Connecting to %s", _configuration.getServerUrl());
			// TODO : accès sécurisé au serveur
//			SslContext sslContext = SslContext.newClientContext(certChainFile)
			
//			_etcd = new EtcdClient(URI.create(_configuration.getConnectUrl()));
			_etcd = new EtcdClient(new EtcdNettyClient(new EtcdNettyConfig().setMaxFrameSize(100 * 100 * 1024), URI.create(_configuration.getServerUrl())));
			_log.debug("Etcd version is %s", _etcd.getVersion());
			_updater = new EtcdRegistrationUpdater();
			endPointDiscoveryInit();
		}catch (NumberFormatException e){
			_etcdTtl = 15;
		} catch (Throwable t) {
			_log.debug("[ETCD TRACK ERROR] 1");
			t.printStackTrace();
		}
	}

	/**
	 * Our bundle is being stopped. Shutdown our service. All adverts have been
	 * automatically unbound (because our component is stopping). Current = any
	 * thread.
	 */
	@Stop
	public void stop() {
		try {
			_stopping = true;
			removeOwnAdvertisement();
			
			if (_etcd != null) {
				_etcd.close();
			}
		} catch (Exception e) {
			_log.error("Closing etcd client failed", e);
		}
		
		
	}

	/**
	 * Remove our own advertisements from Etcd Server before stopping the component
	 */
	private void removeOwnAdvertisement() {
		_registrations.keySet()
					  .forEach(key -> {
						  if (_ownRegistration.contains(key)){
							  try {
								_etcd.delete(key).recursive().send().get();
							} catch (Exception e) {
								_log.error("[unregisterAll] can't delete key=%s",key, e);
							}
						  }
					  });
		
		_ownRegistration.clear();
	}

	
	/**
	 * listen for local Advertisements in the white board and publish them to
	 * Etcd Current executor thread = a dedicated PlatformExecutor queue which
	 * is running within the IO blocking threadpool.
	 */
	@ServiceDependency(required = false, removed = "unpublish", filter = "(!(provider=*))")
	public void publish(Advertisement advert, Dictionary<String, Object> serviceProperties) throws IOException {
		publishAdvertOnEvent(serviceProperties, advert, true);
	}

	/**
	 * Unpublish an advert
	 * @param advert
	 * @param serviceProperties
	 */
	public void unpublish(Advertisement advert, Dictionary<String, Object> serviceProperties) {
		publishAdvertOnEvent(serviceProperties, advert, false);
	}

	/**
	 * Build a unique key for etcd
	 * @param advert : Our Advertisement
	 * @param properties : Advertisement properties
	 * @return the etcd key
	 */
	private String buildEtcdPathKey(Advertisement advert, Map<String, Object> properties) {
		if (properties.get(ConfigConstants.INSTANCE_ID) == null
				|| (properties.get(ConfigConstants.MODULE_ID) == null)
				|| (properties.get(ConfigConstants.MODULE_NAME) == null)
				|| (properties.get(ConfigConstants.GROUP_NAME) == null)
				|| (properties.get(ConfigConstants.INSTANCE_NAME) == null)) {
			return null;
		}
		String key;
		if (properties.containsKey(EtcdProperty.KEY) 
				&& properties.get(EtcdProperty.KEY) != null){
			key = (String) properties.get(EtcdProperty.KEY);
		} else {
			key = String.format("%s/%s/%s/%s/%s/%s%s", _configuration.getCasrPublishDir(),
					_jvmUUid,
					properties.get(ConfigConstants.MODULE_NAME),
					properties.get(ConfigConstants.GROUP_NAME),
					properties.get(ConfigConstants.INSTANCE_NAME),
					properties.get(ConfigConstants.INSTANCE_ID),
					UUID.randomUUID().toString());
		}
				
		return key;
	}

	/**
	 * Convert a Properties Map to a Json formated String as Service info
	 * @param propertiesMap
	 * @return Json string
	 */
	private String buildServiceInfo(Map<String, Object> properties) {
		try {
			String jsonInfo;
			if (properties.containsKey(EtcdProperty.VALUE) 
					&& properties.get(EtcdProperty.VALUE) != null){
				jsonInfo = (String) properties.get(EtcdProperty.VALUE);
			} else {
				jsonInfo = new ObjectMapper().writeValueAsString(properties);
			}
			return jsonInfo;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Parse a Json String
	 * @param jsonString
	 * @return map 
	 */
	public Map<String, Object> jsonStringToMap(final String jsonString){
		_log.debug("[jsonStringToMap] %s", jsonString);
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
    	// convert JSON string to Map
    	try {
			jsonMap = mapper.readValue(jsonString,
			        new TypeReference<Map<String, Object>>(){});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonMap;
	}
	
	
	/**
	 * Publish or Unpublish to etcd
	 * @param key : etcd Key
	 * @param value : etcd Value
	 * @param properties : Properties provided with The Advertisement
	 * @param advert : The Advertisement to (un)publish
	 * @param pub : Decide if it(s a publish or unpublish
	 */
	private void publishAdvertOnEvent(Dictionary<String, Object> serviceProperties, 
			Advertisement advert, boolean pub) {
		
		Map<String, Object> properties = new HashMap<>();
		DictionaryToMap<String, Object> tmpDict = new DictionaryToMap<>(serviceProperties);
		Map<String, Object> complementaryInfo = publishedServiceProperties(tmpDict, advert);
		properties.putAll(complementaryInfo);
		boolean isCustomKey = properties.containsKey(EtcdProperty.KEY) 
				&& properties.get(EtcdProperty.KEY) != null;
		boolean isCustomValue = properties.containsKey(EtcdProperty.VALUE) 
				&&	properties.get(EtcdProperty.VALUE) != null;
		
		if (isCustomKey != isCustomValue){
			_log.debug("[publishAdvertOnEvent] Bad Etcd Properties, Advert rejected!");
			return;
		}
		
		boolean isCustomAdvert = isCustomKey && isCustomValue;
		_log.debug("[publishAdvertOnEvent] Custom advert : %s", isCustomAdvert);
		
		String key = buildEtcdPathKey(advert, properties);
		String value = buildServiceInfo(properties);
		
		try {
			if (isCustomAdvert){
				if (pub){
					_etcd.put(key, value).send().get();
				} else{
					_etcd.delete(key).recursive().send().get();
				}
			} else {
				if (pub){
					_ownRegistration.add(key);
					_log.info("[publishAdvertOnEvent] publish to Etcd key = %s", key);
					EtcdKeysResponse response = _etcd.put(key, value).send().get();
					addServiceRegistration(key, advert, properties);
				} else{
					_ownRegistration.remove(key);
					_log.info("[publishAdvertOnEvent] unpublish to Etcd key = %s", key);
					_etcd.delete(key).recursive().send().get();
					deleteServiceRegistration(key);
				}
			}
			
			_log.info("[publishAdvertOnEvent] Advert successfully %s Etcd server <K,V> = <%s,%s>", 
					(pub)?"pushed to":"deleted from",key, value);
		} catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
			_log.debug("[ETCD TRACK ERROR] 4");
			_log.warn("Could not %s Advert !",(pub)?"publish":"unpublish", e);
		}
	}

	/**
	 * Build properties to be published out to etcd
	 * 
	 * @param serviceProperties
	 *            comes for OSGi registration
	 * @return a Map containing agregated Map. If serviceProperties Map does not
	 *         contains ConfigConstants.xxx properties, it's retrieved from dico
	 *         System
	 **/
	private Map<String, Object> publishedServiceProperties(Map<String, Object> serviceProperties,
			Advertisement advert) {
		Map<String, Object> advertProperties = new HashMap<String, Object>();
		final String muxInfo = "mux.factory.remote";
		_log.debug("[publishedServiceProperties] properties : %s", buildServiceInfo(serviceProperties));
		
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
		advertProperties.put(ConfigConstants.SERVICE_IP, advert.getIp());
		advertProperties.put(ConfigConstants.SERVICE_PORT, Integer.toString(advert.getPort()));

		return advertProperties;
	}

	
	/**
	 * Must be called at start. Initialize Service Registration
	 */
	private void endPointDiscoveryInit(){
		String easterEgg = "Hear my words and bear witness "
				+ "to my vow. Night gathers, and now my WATCH BEGINS";
		Long index = 0L, lastIndex = 0L;
		final String localDirectory = getLocalNodePath();
		final String rootDirectory = _configuration.getCasrPublishDir();
		
		try {
				// First create directories
				createDirectory(rootDirectory);
				createDirectory(localDirectory);
				
				EtcdKeysResponse response =
						_etcd.getDir(rootDirectory)
							 .recursive()
							 .send()
							 .get();
			
				_log.info("[endPointDiscoveryInit] get Etcd Index");
				index = getEtcdIndex(response);
				lastIndex = initRegistrations(response.getNode(), index);
				_log.info("[endPointDiscoveryInit] %s directory found with index : %s, last modification on index %s, %s",
						rootDirectory, index, lastIndex, easterEgg);
		}
		catch (EtcdException e){
			_log.error("[endPointDiscoveryInit] %s", e.getMessage());
//			if (e.isErrorCode(EtcdErrorCode.KeyNotFound)){
//				_log.info("[endPointDiscoveryInit] Creation of local directory...");
//				createDirectory(localDirectory);
//				endPointDiscoveryInit();
//			}
		}
		catch (IOException | EtcdAuthenticationException | TimeoutException e) {
			_log.debug("[ETCD TRACK ERROR] 2");
			e.printStackTrace();
		} finally {
			watchDirectory(_configuration.getCasrPublishDir(), lastIndex + 1);
		}
	}

	/**
	 * Should be used at start to register all existing Advertisement on Etcd Server
	 * @param root : Node containing all Advertisements
	 * @param index : Index of the root Node
	 * @return The last modified index
	 */
    private Long initRegistrations(EtcdNode root, final Long index) {
    	_log.info("[getLatestRegistrations] Starting search on etcd tree");
    	List<EtcdNode> etcdNodes = getAllLeaves(root);
		long lastIndex = index;
		_log.info("[initDiscoveryEtcd] %s existing services found on Etcd Server", etcdNodes.size());
		for(EtcdNode node  : etcdNodes ){
			if (!_registrations.containsKey(node.key)){
				Map<String, Object> properties = jsonStringToMap(node.value);
	        	if (isRegistrable(properties)){
	        		String ip = properties.get(ConfigConstants.SERVICE_IP).toString();
	        		String port = properties.get(ConfigConstants.SERVICE_PORT).toString();
	        		_log.info("[handleDiscoveryEndpointChange] Adding Advertisement... <IP, PORT> = <%s, %s>", ip, port);
	        		Advertisement advert = new Advertisement(ip, port);
	        		addServiceRegistration(node.key, advert, properties);
	        		lastIndex = (node.modifiedIndex > lastIndex)?node.modifiedIndex:lastIndex;
	        	}
			} else{
				_log.warn("[initDiscoveryEtcd] Skipping key %s...  // Reason: already registered", node.key);
			}
		}
    	return lastIndex;
	}

    /** 
     * Get Etcd Index from a Response
     * @param response
     * @return the index
     */
	private long getEtcdIndex(EtcdKeysResponse response) {
        long index = 0l;
        if (response != null) {
            if (response.etcdIndex != null) {
                index = response.etcdIndex;
            }
            else if (response.node.modifiedIndex != null) {
                index = response.node.modifiedIndex;
            }
            if (response.node.dir && response.node.nodes != null) {
                for (EtcdNode node : response.node.nodes) {
                    if (node.modifiedIndex > index) {
                        index = node.modifiedIndex;
                    }
                }
            }
        }
        return index;
    }
    
	/**
	 * Create (if it does not already exist) the given path on Etcd as a Directory
	 * @param rootPath
	 * @throws TimeoutException 
	 * @throws EtcdAuthenticationException 
	 * @throws EtcdException 
	 * @throws IOException 
	 */
	private void createDirectory(String rootPath) throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
		try {
			// Try to get the directory, if it does not exist, it will throw an Exception
			// with error "KeyNotFound". So we'll be able to create the directory
			_etcd.getDir(rootPath)
			 	 .send()
			 	 .get();
			_log.debug("[createDirectory] %s already exists", rootPath);
		} catch (EtcdException e){
			if (e.isErrorCode(EtcdErrorCode.KeyNotFound)){
				_log.info("[createDirectory] Creation of directory %s...", rootPath);
				_etcd.putDir(rootPath)
				 	 .send()
				 	 .get();
			} else {
				_log.error("[createDirectory] Error creating directory %s, %s",rootPath, e.getMessage());
			}
		}
		catch (Exception e) {
			_log.error("[createDirectory]",e);
		} 
	}
	
	/**
	 * Etcd Event Handler. It's only used for remote registrations as we already use 
	 * osgi register for our services
	 * @param response : 
	 */
	private void handleDiscoveryEndpointChange(EtcdKeysResponse response) {
		long index = 0l;
		try {
			index = response.node.modifiedIndex;
//			final String key = response.node.key;
			List<EtcdNode> nodes = getAllLeaves(response.node);
        	_log.info("[handleDiscoveryEndpointChange] Handling peer endpoint change (%s modifications) at etcd index %s", index, nodes.size());
        	for (EtcdNode node : nodes){
	        	if (!_ownRegistration.contains(node.key)&& (_registrations.get(node.key) == null)
        			&& (response.node.value != null)){
	        		// we get "set" on a watch response
	                if (response.action == EtcdKeyAction.set) {
	            		Map<String, Object> properties = jsonStringToMap(node.value);
	                	if (isRegistrable(properties)){
	                		String ip = properties.get(ConfigConstants.SERVICE_IP).toString();
	                		String port = properties.get(ConfigConstants.SERVICE_PORT).toString();
	                		_log.info("[handleDiscoveryEndpointChange] Adding Advertisement... <IP, PORT> = <%s, %s>", ip, port);
	                		Advertisement advert = new Advertisement(ip, port);
	                		addServiceRegistration(node.key, advert, properties);	
	                	} else {
	                		_log.warn("[handleDiscoveryEndpointChange] The event received does not match Advertisement properties");
	                	}
	                }
	                
	                // remove endpoint on "delete" or "expire", and it's not about ourself
	                else if (response.action == EtcdKeyAction.delete || response.action == EtcdKeyAction.expire) {
	                    deleteServiceRegistration(node.key);
	                }
	        	}
        	}
        }
        catch (Exception e) {
            _log.error("Could not handle peer discovery endpoint change! [index = %s]", index, e);
        }
		finally {
			watchDirectory(_configuration.getCasrPublishDir(), index + 1);
        }
	}
	
	/**
	 * Recursive method to retrieve all the leaves from an Etcd Node
	 * @param root : Master Node
	 * @return the list of leaves
	 */
	private List<EtcdNode> getAllLeaves(EtcdNode root){
		List<EtcdNode> nodes = new ArrayList<>();
		root.getNodes()
			.forEach(node -> {
				if (node.getValue() == null){
					nodes.addAll(getAllLeaves(node));
				} else{
					nodes.add(node);
				}
			});
		return nodes;
	}
	
	/**
	 * 
	 * @return
	 */
	private String getLocalNodePath() {
		return String.format("%s/%s", _configuration.getCasrPublishDir(), _jvmUUid);
	}
	
	/**
	 * Check if the properties map match with advetisement properties
	 * @param properties
	 * @return
	 */
	private boolean isRegistrable(Map<String, Object> properties){
		for (String name : DEFAULT_CONSTANT) {
			if (properties.get(name) == null) {
				return false;
			}
		}
		
		if (properties.get(ConfigConstants.SERVICE_IP) == null 
				|| properties.get(ConfigConstants.SERVICE_PORT) == null) {
			return false;
		}
		
		return true;
	}
	
	
	private void deleteServiceRegistration(final String key) {
		_registrations.get(key).unregister();
		_registrations.remove(key);
		_log.info("[deleteServiceRegistration]  %s removed from OSGi registry", key);
	}

	/**
	 * Register an advert to OSGi registry and our cache
	 * @param key
	 * @param advert
	 * @param properties
	 */
	private void addServiceRegistration(final String key, final Advertisement advert, 
			Map<String, Object> properties) {
		properties.put("provider", "etcd");
		Dictionary<String, Object> dict = new Hashtable<>();
		properties.entrySet()
				  .stream()
				  .filter(entry -> entry.getKey() != null && entry.getValue() != null)
				  .forEach(e -> dict.put(e.getKey(), e.getValue()));
		ServiceRegistration<Advertisement> serviceReg = 
				 _bundleContext.registerService(Advertisement.class, advert,
						 dict);
		_registrations.put(key,serviceReg);
		_log.info("[addServiceRegistration]  %s added to OSGi registry", key);
	}
	
	/**
	 * Basic Etcd watcher. When a modification is done on etcd server,
	 * onResponse method of ReponseListener is called
	 * @param directory : etcd path to watch
	 */
	private void watchDirectory(String directory, long index){
		try {
			_etcd.get(directory)
				 .waitForChange(index)
				 .recursive()
				 .send()
				 .addListener(_responseListener);
		} catch (Throwable e) {
			_log.warn("Could not set etcd watch to %s!", directory, e);
		}
	}
	
	private class EtcdRegistrationUpdater implements Runnable {

        private final ScheduledFuture<?> _future;
        
        public EtcdRegistrationUpdater() throws Exception {
        	_future = _pfexecs.getCurrentThreadContext()
        					   .getCurrentExecutor()
        					   .scheduleAtFixedRate(this, 0, _etcdTtl - 5, TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            try {
            	 _log.debug("[EtcdRegistrationUpdater] period update (each %s seconds)", _etcdTtl - 5);
            	EtcdResponsePromise<EtcdKeysResponse> responsePromise =
                    _etcd.putDir(getLocalNodePath())
                         .ttl(_etcdTtl)
                         .prevExist(true)
                         .send();
                _log.debug("registered at etcd index " + responsePromise.get().etcdIndex);
            }
            catch (EtcdException e) {
                _log.error("[EtcdRegistrationUpdater] ERROR : %s", e.getMessage());
            }
            catch (Exception e) {
                _log.error("Etcd registration update failed", e);
            }
        }

        public void cancel() {
            try {
                _future.cancel(false);
                _etcd.delete(getLocalNodePath())
                	 .recursive()
                	 .send()
                	 .get();
            }
            catch (Exception e) {
                _log.error("Etcd deregistration update failed", e);
            }
        }
    }
	
	/**
	 * Listener used for any etcd server modifications 
	 */
	private class ResponseListener implements IsSimplePromiseResponseHandler<EtcdKeysResponse> {
		
		/**
		 * As etcd4j use promise (!= listener), watchDirectory function must be called
		 * at the end of that function in order to watch continuously the server
		 */
		@Override
		public void onResponse(ResponsePromise<EtcdKeysResponse> promise) {
			_queue.execute(() -> {
				if (!_stopping){
					try {
						if (promise.getException() != null) {
							_log.warn("etcd watch received exception: %s", promise.getException().getMessage());
							endPointDiscoveryInit();
							return;
						}
						handleDiscoveryEndpointChange(promise.get());	
					} catch (Exception e) {
						_log.warn("Could not handle discovery endpoint change or set a new watch", e);
						endPointDiscoveryInit();
					} 
				}
				else {
					_log.warn("[EtcdCallback] Stopping component...");
				}
			});
		}
	}
}
