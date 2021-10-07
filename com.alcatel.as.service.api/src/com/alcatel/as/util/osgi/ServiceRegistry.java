package com.alcatel.as.util.osgi;

import java.util.Dictionary;
import java.util.Map;
import java.util.List;

/** 
 * This interface abstracts the OSGi service registry and allows for dynamic registration of services
 * without direct manipulation of OSGi APIs.
 * Typical usage is as follows:
 * <ol>
 * <li> Use DependencyActivatorHelper as the base class of your activator: this will automatically register your own ServiceRegistry instance
 * <pre> public class MyActivator extends DependencyActivatorHelper </pre>
 * <li> In the component that will need registering dynamic services, express a dependency on your ServiceRegistry (filtered using your activator's package name)
 * <pre> 
 *     addService(createService()
 *          .setImplementation(MyDynamicServiceProvider.class)
 *          .add(createServiceDependency()
 *                .setService(com.alcatel.as.util.osgi.ServiceRegistry.class, "(name="+
 *                   this.getClass().getPackage().getName()+")")
 *                .setAutoConfig("_serviceRegistry")
 *                .setRequired(true)));
 * </pre>
 * <li> Use the injected ServiceRegistry to register services dynamically
 * <pre>
 *   public class MyDynamicServiceProvider {
 *     ServiceRegistry _serviceRegistry; //injected by activator
 *     Object _registration = null;
 *
 *     void myMethodRegisteringAService() {
 *       _registration = _serviceRegistry.registerService(
 *                           _registration, 
 *                           MyService.class.getName(),
 *                           new MyServiceImpl(),
 *                           myServicePropertiesIfAny);
 *     }
 *
 *     void myMethodUnRegisteringAService() {
 *       _serviceRegistry.unregisterService(_registration);
 *     }
 *   }
 * </pre>
 * </ol>
 */
public interface ServiceRegistry {
  /**
   * dynamically register a service in the OSGi service registry.
   * @param previousReg the object returned by a previous call to registerService (in case of a service replacement) or null.
   * @param clazz the interface under which the service is registered
   * @param o the service instance
   * @param props the service properties (or null)
   * @return a registration object to be reused for unregistering or replacing the service
   */
  @SuppressWarnings("unchecked")
  Object registerService(Object previousReg, String clazz, Object o, Dictionary props);
  /**
   * dynamically unregister a service from the OSGi service registry.
   * @param registration the object returned by registerService
   */
  void unregisterService(Object registration);


  /** @deprecated use lookupServices instead */
  <S> Map<S, Map<String, Object>> lookupService(Class<S> clazz, String filter) throws Exception;

	interface ServiceRef<S> {
		S get();
		Map<String,Object> properties();
	}
	<S> List<ServiceRef<S>> lookupServices(Class<S> clazz, String filter) throws Exception;
}
