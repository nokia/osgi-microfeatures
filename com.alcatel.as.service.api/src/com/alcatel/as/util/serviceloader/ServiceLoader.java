// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.serviceloader;

// Jdk
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


/**
 * Helper class used to load a service from the OSGi service registry. This class has two
 * purpose:
 * <p>
 * <ul>
 * <li>Used to load a service within an OSGi environment. this method simply looks up a service
 * from the OSGi registry. However, it will return null if the service is not available, you a
 * slightly better way to get services is to use a dependency injection framework (like
 * SCR/DM/Spring/iPOJO, etc ...)
 * <li>Used to load a service outside OSGi. The services are tracked by looking for
 * META-INF/services files from the classpath.
 * </ul>
 * 
 * <p> This class is a component in order to get injected with a bundle context. If it used before
 * it is activated, then we'll do a best effort and use low level OSGi API in order to get a bundle context.
 */
@Component(provides=ServiceLoader.class)
public class ServiceLoader {
  /** Automatically injected by dependency manager, or manually retrieved using low level osgi API. */
  @Inject
  private static volatile BundleContext _bc;
  
  /** List of already loaded services (non OSGi mode) */
  private final static Map<String, Object> _services = new ConcurrentHashMap<String, Object>();
  
  /** lock internally used for thread safety. */
  private final static ReentrantLock _lock = new ReentrantLock();
  
  /** Our logger */
  private final static Logger _logger = Logger.getLogger(ServiceLoader.class);
  
  /**
   * Returns the service associated to the given class. If the service is not ready, then this
   * method returns null. Application may track for the service availability, using any OSGi
   * dependency injection framework (like DM, SCR, iPOJO, Spring, ...)
   * 
   * @param c The service interface name
   * @return the Service, or null if the service is not yet started
   */
  @SuppressWarnings("unchecked")
  public static <T> T getService(Class<T> c) {
    return (T) getService(c.getName(), null);
  }
  
  /**
   * Returns the service associated to the given class. If the service is not ready, then this
   * method returns null. Application may track for the service availability, using any OSGi
   * dependency injection framework (like DM, SCR, iPOJO, Spring, ...)
   * 
   * @param c The service interface name
   * @param filter the OSGi filter used to lookup the correct service
   * @return the Service, or null if the service is not yet started
   */
  @SuppressWarnings("unchecked")
  public static <T> T getService(Class<T> c, String filter) {
    return (T) getService(c.getName(), filter);
  }
  
  /**
   * Returns an implementation object implementing the interface provided in arguments.
   * 
   * @param serviceInterface the interface of the service to load
   * @return null if the service implementation is not found
   */
  @SuppressWarnings("unchecked")
  public static <T> T getService(String serviceInterface) {
    return (T) getService(serviceInterface, null);
  }
  
  /**
   * Loads a class from the classpath, outside OSGi. This method is only meant to be used
   * outside OSGi, in junit autotests, for instance, which are not running inside an OSGi
   * container. This method will lookup the specified class name from all jars found in the
   * classpath. When a jar contains a meta file (META-INF/services/clazz) which corresponds to
   * the clazz parameter, then the content of this file will be loaded in order to retrieve the
   * service implementation. The service meta file can also contains some specific service
   * properties (comma separated) which can also be specified in the filter parameter.
   * 
   * @param clazz the class to load
   * @param filter an OSGi filter matching some keys from the SPI META-INF/services file. 
   * SPI implementation classes can be appended with some specific service properties 
   * (key=value pairs, comma separated). Example: com.foo.bar.MyInterfaceImpl;foo=bar;foo2=bar2
   * @param paramObjects objects to be passed to the constructor of the service implementation
   * @param paramClasses classes of objects passed to the constructor of the service implementation
   * @return a service loaded from the classpath
   */
  @SuppressWarnings("unchecked")
  public static <T> T loadClass(Class<?> clazz, String filter, Object[] paramObjects, Class<?>[] paramClasses) {
    T service = (T) loadFromClassPath(clazz.getName(), filter, paramObjects, paramClasses);
    if (service != null) {
      // Register the service in our map, so we can return it from our getService methods
      _lock.lock();
      try {
        String serviceKey = clazz.getName() + ((filter == null) ? "" : filter);
        _services.put(serviceKey, service);
      } finally {
        _lock.unlock();
      }
    }
    return service;
  }
  
  /**
   * Returns the service associated to the given class. If the service is not ready, then this
   * method returns null. Application may track for the ervice availability, using any OSGi
   * dependency injection framework (like DM, SCR, iPOJO, Spring, ...)
   * 
   * @param serviceInterface The interface of the service to load
   * @param filter an optional OSGi filter
   * @return the service, or null if the service has bot been found.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getService(String serviceInterface, String filter) {
    String serviceKey = serviceInterface + ((filter == null) ? "" : filter);
    initBundleContext();
    
    if (_bc == null) {
      // It looks like we are not running inside an OSGi framework: try to load an
      // implementation from the classpath.
      
      // First, see if we have already loaded the service in our cache.
      Object service = (T) _services.get(serviceKey);
      if (service != null) {
        return (T) service;
      }
        
      // The service has not yet been loaded from the classpath, load it.
      
      _lock.lock();
      try {
        Object s = _services.get(serviceKey);
        if (s != null) {
          return (T) s;
        }
        
        service = loadFromClassPath(serviceInterface, filter, new Object[0], new Class[0]);
        if (service != null) {
          _services.put(serviceKey, service);
        }
        return (T) service;
      } finally {
        _lock.unlock();
      }
    }
    
    // Try to load the service from the OSGi registry ...
    ServiceReference<?>[] refs;
    try {
      refs = _bc.getServiceReferences(serviceInterface, filter);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid OSGi service filter: " + filter, e);
    }
    if (refs != null) {
      return (T) _bc.getService(refs[0]);
    }
    return null;
  }
  
  private static void initBundleContext() {
	if (_bc == null) {
		try {
			Bundle b = FrameworkUtil.getBundle(ServiceLoader.class);
			if (b != null) {
				b.start();
				_bc = b.getBundleContext();
			}
		} catch (Throwable t) {
			// we are probably running outside osgi.
		}
	}	
}

  private static Object loadFromClassPath(String serviceInterface, String filter, Object[] paramObjects,
                                          Class<?>[] paramClasses) {
    String[] filterKeyVal = parseFilter(filter);
    String factoryClassName = null;
    
    try {
      URL[] urls = getResources("META-INF/services/" + serviceInterface,
                                new ClassLoader[] { Thread.currentThread().getContextClassLoader(),
                                    ServiceLoader.class.getClassLoader(), ClassLoader.getSystemClassLoader(), });
      
      if (urls.length == 0) {
        return null;
      }
      
      for (URL u : urls) {
        if ((factoryClassName = lookForFactory(u, filterKeyVal)) != null) {
          break;
        }
      }
      
      if (factoryClassName == null) {
        return null;
      }
      
      Class<?> clazz = getClass(factoryClassName,
                                new ClassLoader[] { Thread.currentThread().getContextClassLoader(),
                                    ServiceLoader.class.getClassLoader(), ClassLoader.getSystemClassLoader() });
      
      return instantiate(clazz, paramObjects, paramClasses);
    }
    
    catch (Throwable t) {
      Throwable lastCause = t;
      while (lastCause.getCause() != null) {
        lastCause = lastCause.getCause();
      }
      throw new RuntimeException("Can not load service " + serviceInterface + ": " + lastCause.toString(), t);
    }
  }
  
  private static String[] parseFilter(String filter) {
    String original = filter;
    if (filter == null) {
      return null;
    }
    filter = filter.trim();
    if (!filter.startsWith("(") && !filter.endsWith(")")) {
      throw new IllegalArgumentException("invalid osgi filter: " + filter);
    }
    filter = filter.substring(1, filter.length() - 1);
    if (filter.indexOf("(") != -1 || filter.indexOf(")") != -1) {
      throw new IllegalArgumentException("complex OSGi filter not currently supported: " + original);
    }
    String[] keyval = filter.split("=");
    if (keyval == null || keyval.length != 2) {
      throw new IllegalArgumentException("invalid osgi filter: " + original);
    }
    keyval[0] = keyval[0].trim();
    keyval[1] = keyval[1].trim();
    return keyval;
  }
  
  /**
   * Parse a factory class name from inside a SPI file, and check if it matches the
   * ldap filter provided by the application.
   * The SPI file may contain either a fully qualified java class file, optionally followed
   * by some optiona osgi service properties.
   * Example:
   * 
   * com.alcatel.as.service.concurrent.impl.jdkTimerServiceImplStandalone;strict=true
   * com.alcatel.as.service.concurrent.impl.WheelTimerServiceImplStandalone;strict=false
   * 
   * @param u the URL of the SPI file containing a given factory class
   * @param filterKeyVal an array of strings (array[0]=filter key name, array[1]=filter key val),
   * or null if no filters are specified.
   * 
   * @return the factory class name, or null if the filter does not match.
   */
  private static String lookForFactory(URL u, String[] filterKeyVal) throws IOException {
    InputStream in = null;
    try {
      String factoryClassName;
      in = u.openStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF8"));
      while ((factoryClassName = br.readLine()) != null) {
        factoryClassName = factoryClassName.trim();
        if (factoryClassName.startsWith("#")) {
          continue;
        }
        
        // Check OSGi service properties, if any.
        int semiColumn = factoryClassName.indexOf(";");
        
        String[] factoryClassNameProps = null;
        if (semiColumn != -1) {
          factoryClassNameProps = factoryClassName.substring(semiColumn + 1).trim().split(";");
          if (factoryClassNameProps.length == 0) {
            _logger.warn("Found invalid properties in file " + u + ": "
                + Arrays.toString(factoryClassNameProps));
            continue;
          }
          factoryClassName = factoryClassName.substring(0, semiColumn);
        }
        
        // Check if the user has specified a filter.
        if (filterKeyVal != null) {
          if (semiColumn == -1) {
            // Ignore the factory if it does contain any properties
            continue;
          }
          
          // The factory class name contains some properties: check if they match our filter.       
          Properties factoryProps = new Properties();
          for (int i = 0; i < factoryClassNameProps.length; i++) {
            String[] keyval = factoryClassNameProps[i].trim().split("=");
            factoryProps.put(keyval[0].trim(), keyval[1].trim());
          }
          if (!filterKeyVal[1].equals(factoryProps.get(filterKeyVal[0]))) {
            continue;
          }
          return factoryClassName;
        } else {
          // The user did not provide any service filters: return the parsed factory class name.
          return factoryClassName;
        }
      }
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
    }
    
    return null;
  }
  
  private static URL[] getResources(String resource, ClassLoader[] loaders) {
    List<URL> urls = new ArrayList<URL>();
    URL u;
    for (ClassLoader cl : loaders) {
      if (cl != null) {
        if ((u = cl.getResource(resource)) != null) {
          urls.add(u);
        }
      }
    }
    
    return urls.toArray(new URL[urls.size()]);
  }
  
  private static Class<?> getClass(String factoryClassName, ClassLoader[] loaders)
      throws ClassNotFoundException {
    for (ClassLoader cl : loaders) {
      try {
        if (cl != null) {
          return cl.loadClass(factoryClassName);
        }
      } catch (ClassNotFoundException e) {
      }
    }
    
    throw new ClassNotFoundException(factoryClassName);
  }
  
  private static Object instantiate(Class<?> clazz, Object[] paramObjects, Class<?>[] paramClasses)
      throws InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException,
      IllegalArgumentException, InvocationTargetException {
    if (paramObjects == null || paramObjects.length == 0) {
      return clazz.newInstance();
    }
    
    Constructor<?> constr = clazz.getConstructor(paramClasses);
    return (constr.newInstance(paramObjects));
  }
}
