// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.etcd;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.osgi.ServiceRegistry.ServiceRef;

import mousio.client.promises.ResponsePromise;
import mousio.client.promises.ResponsePromise.IsSimplePromiseResponseHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdErrorCode;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;

@Component(provides=Object.class)
@Property(name = "asr.component.parallel", value = "true")
@Property(name = "asr.component.cpubound", value = "false")
public class K8sPodDiscovery {
	
	private static final String ROOT_PATH= "/registry/pods/";
	
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
	 * Flag used to deactivate the Etcd callback at bundle stopping step
	 */
	private volatile boolean _stopping = false;
	
	/**
     *  Map of infoKey -> OSGi registrations, or a string for "in progress" operations.
     */
    private Map<String, List<ServiceRegistration<Advertisement>>> _registrations = new HashMap<>();
	
	/**
	 * System configuration.
	 */
	private Dictionary<String, String> _system;
	
	/**
	 * Etcd configuration containing properties such as the address or the root path to watch on
	 */
	private K8sEtcdConfiguration _configuration;
	
	private List<String> _etcdPaths;
	
	/**
	 * Etcd Callback at any change on Etcd Server (set, post, delete...)
	 */
	private volatile List<ResponseListener> _responseListeners;
	
	@ServiceDependency
	private PlatformExecutors _pfexecs;
	
	/**
	 * Log factory. We wait for this service to make sure log4j is really
	 * initialized.
	 */
	@ServiceDependency
	private LogServiceFactory _logFactory;
	
	/**
	 * Context needed to register local advertisements.
	 */
	@Inject
	BundleContext _bundleContext;
	
	@ConfigurationDependency(pid = "system")
	void updated(Dictionary<String, String> system) {
		_system = system;
	}
	
	@ConfigurationDependency
	void loadEtcdConf(K8sEtcdConfiguration conf) {
		_configuration = conf;
	}
	
	private final static String CONTAINER_PORT_NAME = "container.port.name";
	private final static String CONTAINER_NAME = "container.name";
	private final static String CONTAINER_PORT_PROTOCOL = "container.port.protocol";	
	private final static String NAMESPACE = "namespace";
	private final static String POD_NAME = "pod.name";
	/**
	 * default constants.
	 */
	private static final String DEFAULT_CONSTANT[] = { 
			ConfigConstants.SERVICE_IP, 
			ConfigConstants.SERVICE_PORT,
			CONTAINER_PORT_NAME,
			CONTAINER_NAME,
			NAMESPACE,
			CONTAINER_PORT_PROTOCOL};
	
	@Start
	void start() {
		try {
			// Retrieve our component queue. We decorate the queue with an
			// executor that will always schedule runnables immediately if
			// the current thread is our current executor queue.
			_queue = _pfexecs.getCurrentThreadContext()
							 .getCurrentExecutor()
							 .toExecutor(ExecutorPolicy.INLINE);
			_log = _logFactory.getLogger("as.service.discovery.k8s");
			_stopping = false;
			
			_log.debug("Connecting to %s", _configuration.getServerUrl());
			
			// TODO : accès sécurisé au serveur
//			SslContext sslContext = SslContext.newClientContext(certChainFile)
//			_etcdPath = new StringBuilder(ROOT_PATH).append(_configuration.getNamespace()).toString();
			_etcd = new EtcdClient(new EtcdNettyClient(new EtcdNettyConfig().setMaxFrameSize(100 * 100 * 1024), URI.create(_configuration.getServerUrl())));
			_log.debug("Etcd version is %s", _etcd.getVersion());
			// TODO: plusieurs path/namespace à écouter
			_responseListeners = new ArrayList();
			_configuration.getNamespaces().forEach(path -> {
				String etcdPath = new StringBuilder(ROOT_PATH).append(path).toString();
				_responseListeners.add(new ResponseListener(etcdPath, path));
				_log.debug("listening on -> %s", etcdPath);
			});
//			_responseListeners = new ResponseListener(_etcdPath);
			_responseListeners.forEach(listener -> endPointDiscoveryInit(listener));
//			endPointDiscoveryInit();
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
			if (_etcd != null) {
				_etcd.close();
			}
		} catch (Exception e) {
			_log.error("Closing etcd client failed", e);
		}
		
		
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
	 * Must be called at start. Initialize Service Registration
	 */
	private void endPointDiscoveryInit(ResponseListener listener){
		Long index = 0L, lastIndex = 0L;
		
		try {
			// If the path does not exist, we have to create it in order to be able to watch it*
			createDirectory(listener._directory);
			EtcdKeysResponse response = _etcd.getDir(listener._directory)
											 .recursive()
											 .send()
											 .get();

			index = getEtcdIndex(response);
			_log.info("[endPointDiscoveryInit] Listen on %s, get Etcd Index -> %s", listener._directory, index);
			lastIndex = initRegistrations(response.getNode(), index, listener);
		}
		catch (EtcdException e){
			_log.error("[endPointDiscoveryInit] %s", e.getMessage());
		}
		catch (IOException | EtcdAuthenticationException | TimeoutException e) {
			_log.error("[endPointDiscoveryInit]", e);
		} finally {
			watchDirectory(listener, lastIndex + 1);
		}
	}
	
	
	/**
	 * Should be used at start to register all existing Advertisement on Etcd Server
	 * @param root : Node containing all Advertisements
	 * @param index : Index of the root Node
	 * @return The last modified index
	 */
    private Long initRegistrations(EtcdNode root, final Long index, ResponseListener listener) {
    	_log.info("[getLatestRegistrations] Starting search on etcd tree");
    	List<EtcdNode> etcdNodes = getAllLeaves(root);
		long lastIndex = index;
		_log.info("[initDiscoveryEtcd] %s existing services found on Etcd Server", etcdNodes.size());
		for(EtcdNode node  : etcdNodes ){
			if (!_registrations.containsKey(node.key)){
				List<Map<String, Object>> propertiesList = jsonStringToMap(node.value, listener._namespace, node.key);
	        	if (propertiesList != null){
	        		propertiesList.forEach(properties ->{
	        			String ip = properties.get(ConfigConstants.SERVICE_IP).toString();
		        		String port = properties.get(ConfigConstants.SERVICE_PORT).toString();
		        		_log.info("[handleDiscoveryEndpointChange] Adding Advertisement... <IP, PORT> = <%s, %s>", ip, port);
		        		Advertisement advert = new Advertisement(ip, port);
		        		addServiceRegistration(node.key, advert, properties);
	        		});
	        		lastIndex = (node.modifiedIndex > lastIndex)?node.modifiedIndex:lastIndex;
	        	}
			} else{
				_log.warn("[initDiscoveryEtcd] Skipping key %s...  // Reason: already registered", node.key);
			}
		}
    	return lastIndex;
	}
	
	
	/**
	 * Basic Etcd watcher. When a modification is done on etcd server,
	 * onResponse method of ReponseListener is called
	 * @param directory : etcd path to watch
	 */
	private void watchDirectory(ResponseListener listener, long index){
		try {
			_etcd.get(listener._directory)
				 .waitForChange(index)
				 .recursive()
				 .send()
				 .addListener(listener);
		} catch (Throwable e) {
			_log.warn("Could not set etcd watch to %s!", listener._directory, e);
		}
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
	 * Etcd Event Handler. It's only used for remote registrations as we already use 
	 * osgi register for our services
	 * @param response : 
	 */
	private void handleDiscoveryEndpointChange(EtcdKeysResponse response, ResponseListener listener) {
		long index = 0l;
		try {
			index = response.node.modifiedIndex;
//			final String key = response.node.key;
			List<EtcdNode> nodes = getAllLeaves(response.node);
			if (response.node.getValue() != null && !nodes.contains(response.node))
				nodes.add(response.node);
        	_log.info("[handleDiscoveryEndpointChange] Handling peer endpoint change on %s (%s modifications) at etcd index %s", response.node.key, nodes.size(), index);
        	for (EtcdNode node : nodes){
	        	if (response.node.value != null){
	        		// we get "set" on a watch response
	                if (response.action == EtcdKeyAction.set) {
	            		List<Map<String, Object>> propertiesList = jsonStringToMap(node.value, listener._namespace, node.key);
	            		if (propertiesList != null){
	            			if (_registrations.containsKey(node.key)){
	            				updateServiceRegistration(node.key, propertiesList);
	            			} else {
	            				propertiesList.forEach(properties -> {
		            				String ip = properties.get(ConfigConstants.SERVICE_IP).toString();
									String port = properties.get(ConfigConstants.SERVICE_PORT).toString();
									Advertisement advert = new Advertisement(ip, port);
									addServiceRegistration(node.key, advert, properties);	
	            				});
	            			}
	            			
	            		}
	                }
	                else if (response.action == EtcdKeyAction.update){
	                	List<Map<String, Object>> propertiesList = jsonStringToMap(node.value, listener._namespace, node.key);
	            		if (propertiesList != null){
	            			if (_registrations.containsKey(node.key)){
	            				updateServiceRegistration(node.key, propertiesList);
	            			} 
//		            		propertiesList.forEach(properties -> {
//		            			String ip = properties.get(ConfigConstants.SERVICE_IP).toString();
//								String port = properties.get(ConfigConstants.SERVICE_PORT).toString();
//								Advertisement advert = new Advertisement(ip, port);
//								updateServiceRegistration(node.key, advert, properties);
//		            		});
	            		}
	                }
	                // remove endpoint on "delete" or "expire", and it's not about ourself
	                else if (response.action == EtcdKeyAction.delete || response.action == EtcdKeyAction.expire) {
	                    deletePodRegistration(node.key);
	                } else {
	                	_log.debug("[handleDiscoveryEndpointChange] RAS on %s", node.key);
	                }
	        	}
        	}
        }
        catch (Exception e) {
            _log.error("Could not handle peer discovery endpoint change! [index = %s]", index, e);
        }
		finally {
			watchDirectory(listener, index + 1);
        }
	}
	
	
	private void updateServiceRegistration(String key, List<Map<String, Object>> podsPropertiesList){
		List<ServiceRegistration<Advertisement>> localAdverts = _registrations.get(key);
		Map<Map<String, Object>, ServiceRegistration<Advertisement>> propertiesToReg = new HashMap<>();
		
		localAdverts.forEach(advert -> propertiesToReg.put(buildProperties(advert), advert));
		
		List<Map<String, Object>> localProperties = new ArrayList<>(propertiesToReg.keySet());
		_log.debug("[updateServiceRegistration] LOCAL PROPERTIES %s", localProperties);
		propertiesToReg.keySet().forEach(properties -> {
			if (!isPresent(properties, podsPropertiesList)){
				ServiceRegistration<Advertisement> reg = propertiesToReg.get(properties);
				reg.unregister();
				localAdverts.remove(reg);
				_log.debug("[updateServiceRegistration] (%s -> %s)removed %s -> %s", localAdverts.size(), _registrations.get(key).size(),
						properties.get("container.port.name"),properties.get(ConfigConstants.SERVICE_PORT));
			}
		});
		
		podsPropertiesList.forEach(properties ->{
			if (!isPresent(properties, localProperties)){
				String ip = properties.get(ConfigConstants.SERVICE_IP).toString();
				String port = properties.get(ConfigConstants.SERVICE_PORT).toString();
				Advertisement advert = new Advertisement(ip, port);
				addServiceRegistration(key, advert, properties);
			}
		});
		
	}
	
	private boolean isPresent(Map<String, Object> properties, List<Map<String, Object>> propertiesList){
		for (Map<String, Object> p : propertiesList){
			boolean isEquals = true;
			for (String key :Arrays.asList(DEFAULT_CONSTANT)){
				isEquals = isEquals && p.get(key).equals(properties.get(key));
			}
			if(isEquals) return true;
		}
		return false;
	}
	
	private Map<String, Object> buildProperties(ServiceRegistration<Advertisement> reg){
		Map<String, Object> properties = new HashMap<>();
		ServiceReference<Advertisement> ref = reg.getReference();
		Arrays.asList(ref.getPropertyKeys())
			  .forEach(key -> properties.put(key, ref.getProperty(key)));
		return properties;
	}
	
	private List<Map<String, Object>> jsonStringToMap(String value, String namespace, String key) {
		try {
			JSONObject jsonObject = new JSONObject(value);
			_log.debug("[jsonStringToMap] Started decoding...");
			JSONArray statusesList = jsonObject.getJSONObject("status")
											   .getJSONArray("containerStatuses");
			String podName = jsonObject.getJSONObject("metadata").getString("name");
			List<Map<String, Object>> propertiesList = new ArrayList<>();
			String podIP = jsonObject.getJSONObject("status")
									 .getString("podIP");
			
			JSONArray containers = jsonObject.getJSONObject("spec")
					   .getJSONArray("containers");
			for (int i = 0; i < containers.length(); i++) {
				Integer containerIndex = null;
				
				String containerName = containers.getJSONObject(i).getString("name");
				JSONArray containerPorts = containers.getJSONObject(i).getJSONArray("ports");
				
				
				for (int k = 0; k < statusesList.length(); k++) {
					String statusContainerName = statusesList.getJSONObject(k).getString("name");
					if (statusContainerName.equals(containerName)){
						containerIndex = k;
					}
				}
				
				if (containerIndex == null) throw new JSONException("Malformed JSON");
				
				boolean isReady = statusesList.getJSONObject(containerIndex).getBoolean("ready");
				
				
				for (int j = 0; j < containerPorts.length(); j++) {
					Map<String, Object> properties = new ConcurrentHashMap<>();
					
					Long containerPort = containerPorts.getJSONObject(j).getLong("containerPort");
					String containerPortName = containerPorts.getJSONObject(j).getString("name");
					String portProtocol = containerPorts.getJSONObject(j).getString("protocol");
					properties.put(ConfigConstants.SERVICE_PORT, containerPort);
					properties.put(ConfigConstants.SERVICE_IP, podIP);
					properties.put(CONTAINER_NAME, containerName);
					properties.put(CONTAINER_PORT_NAME, containerPortName);
					properties.put(CONTAINER_PORT_PROTOCOL, portProtocol);
					properties.put(NAMESPACE, namespace);
					properties.put(POD_NAME, podName);
					if (Boolean.TRUE.equals(isReady)){
						propertiesList.add(properties);
					} else {
						_log.debug("[jsonStringToMap] Pod[container %s, port %s] not ready...", containerName, containerPort);
						removeIfRegistered(key, properties);
					}
				}
			}
			
			_log.debug("[jsonStringToMap] %s", propertiesList);
			return propertiesList;
			
		} catch (JSONException e) {
			_log.error("Error decoding JSON provided by etcd, malformed JSON...");
		}
		return null;
	}

	private void removeIfRegistered(String key, Map<String, Object> properties) {
		List<ServiceRegistration<Advertisement>> localAdverts = _registrations.get(key);
		if (localAdverts != null){
			Map<Map<String, Object>, ServiceRegistration<Advertisement>> propertiesToReg = new HashMap<>();
			localAdverts.forEach(advert -> propertiesToReg.put(buildProperties(advert), advert));
			List<Map<String, Object>> localProperties = new ArrayList<>(propertiesToReg.keySet());
			List<Map<String, Object>> remoteProperties = new ArrayList<>();
			remoteProperties.add(properties);
			localProperties.forEach(p -> {
				if (isPresent(p, remoteProperties)){
					ServiceRegistration<Advertisement> reg = propertiesToReg.get(p);
					_log.debug("[removeIfRegistered] remove %s - %s", p.get("container.port.name"), p.get(ConfigConstants.SERVICE_PORT));
					reg.unregister();
					localAdverts.remove(reg);
				}
			});
			
		}
	}

	private void deletePodRegistration(String key) {
		List<ServiceRegistration<Advertisement>> registrationList = _registrations.get(key);
		if (!registrationList.isEmpty() && registrationList != null){
			registrationList.forEach(registration -> registration.unregister());
			_registrations.remove(key);
			_log.debug("[deleteServiceRegistration]  %s removed from OSGi registry (%s adverts)", key, registrationList.size());
		}
		
	}


	private void addServiceRegistration(String key, Advertisement advert, Map<String, Object> properties) {
		properties.put("provider", "kubernetes");
		Dictionary<String, Object> dict = new Hashtable<>();
		properties.entrySet()
				  .stream()
				  .filter(entry -> entry.getKey() != null && entry.getValue() != null)
				  .forEach(e -> dict.put(e.getKey(), e.getValue()));
		ServiceRegistration<Advertisement> serviceReg = 
				 _bundleContext.registerService(Advertisement.class, advert,
						 dict);
		List<ServiceRegistration<Advertisement>> serviceList = new ArrayList<>();
		serviceList.add(serviceReg);
		List<ServiceRegistration<Advertisement>> currentList = _registrations.putIfAbsent(key,serviceList);
		if (currentList != null){
			currentList.add(serviceReg);
			_log.debug("[addServiceRegistration] already registered list %s, adding %s", key, properties.get(ConfigConstants.SERVICE_PORT));
		}
		_log.debug("[addServiceRegistration]  %s added to OSGi registry", key);
		
	}

	private void updateServiceRegistration(String key, Advertisement advert, Map<String, Object> properties) {
		_log.debug("[updateServiceRegistration]  %s Updating...", key);
		deletePodRegistration(key);
		addServiceRegistration(key, advert, properties);
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
	 * Listener used for any etcd server modifications 
	 */
	private class ResponseListener implements IsSimplePromiseResponseHandler<EtcdKeysResponse> {
		private String _directory, _namespace;
		public ResponseListener(String directory, String namespace) {
			_directory = directory;
			_namespace = namespace;
		}
		
		/**
		 * As etcd4j use promises (!= listeners), watchDirectory function must be called
		 * at the end of that function in order to watch continuously the server
		 */
		@Override
		public void onResponse(ResponsePromise<EtcdKeysResponse> promise) {
			_queue.execute(() -> {
				if (!_stopping){
					try {
						if (promise.getException() != null) {
							_log.warn("etcd watch received exception: %s", promise.getException().getMessage());
							endPointDiscoveryInit(this);
							return;
						}
						handleDiscoveryEndpointChange(promise.get(), this);	
					} catch (Exception e) {
						_log.warn("Could not handle discovery endpoint change or set a new watch", e);
						endPointDiscoveryInit(this);
					} 
				}
				else {
					_log.warn("[EtcdCallback] Stopping component...");
				}
			});
		}
	}

}
