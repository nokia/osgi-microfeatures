package com.alcatel.as.service.appmbeans;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.management.JMException;
import javax.management.modelmbean.ModelMBean;

public interface ApplicationMBeanFactory {
  /**
   * Prefix of the name of the attribute in the ServletContext
   */
  public final static String ATTR_PREFIX_MODEL_MBEAN = "javax.management.modelmbean.";

  /**
   * Load all mbeans descriptors from an application into a registry
   * 
   * @param key application id. (also used as the registry id to isolate the metadata in a dedicated registry)
   * @throws IOException
   */
  public boolean loadDescriptors(String key) throws IOException;

  /**
   * Load the mbeans descriptors from an URL into a registry
   * 
   * @param key registry id. (used to isolate the metadata in a dedicated registry)
   * @param url location of mbeans-descriptors.xml
   * @throws IOException
   */
  public void loadDescriptors(String key, URL url) throws IOException;

  /**
   * Load the mbeans descriptors from an InputStream into a registry
   * 
   * @param key registry id. (used to isolate the metadata in a dedicated registry)
   * @param inputStream of mbeans-descriptors.xml
   * @throws IOException
   */
  public void loadDescriptors(String key, InputStream inputStream) throws IOException;

  /**
   * Reset the mbeans descriptors stored into a registry
   * 
   * @param key registry id.
   */
  public void unloadDescriptors(String key);

  /**
   * Create a mbean: 1) using the descriptor id. loaded in the registry 2) if no descriptor
   * found and if the bean implements a well-known MBean interface 3) else, using a "default"
   * profile loaded in the registry
   * 
   * @param key registry id.
   * @param protocol the container protocol (Http/Sip/...)
   * @param source bean
   * @param name descriptor id.
   * @param majorVersion bean major version
   * @param minorVersion bean minor version
   * @return returns a mbean or null if the mbean is not created
   * @throws JMException
   */
  public ModelMBean registerObject(String key,
				   String protocol,
				   Object source,
				   String name,
				   int majorVersion,
				   int minorVersion) throws JMException;

  /**
   * Create a mbean for a servlet: 1) using the descriptor id. loaded in the registry 2) if no
   * descriptor found and if the bean implements a well-known MBean interface 3) else, using a
   * "default" profile loaded in the registry
   * 
   * @param key registry id.
   * @param protocol the container protocol
   * @param servlet bean
   * @return returns a ModelMbean
   */
  public ModelMBean registerServlet(String key, String protocol, Object servlet) throws JMException;

  /**
   * Create a mbean for a servlet: 1) using the descriptor id. loaded in the registry 2) if no
   * descriptor found and if the bean implements a well-known MBean interface 3) else, using a
   * "default" profile loaded in the registry
   * 
   * @param key registry id.
   * @param protocol protocol
   * @param servlet bean
   * @param holder servlet holder
   * @return returns a ModelMbean
   */
  public ModelMBean registerServlet(String key, String protocol, Object servlet, Object holder) throws JMException;

  /**
   * Create a mbean for a servlet: 1) using the descriptor id. loaded in the registry 2) if no
   * descriptor found and if the bean implements a well-known MBean interface 3) else, using a
   * "default" profile loaded in the registry
   * 
   * @param key registry id.
   * @param protocol protocol
   * @param servlet bean
   * @param holder servlet holder
   * @param name descriptor id.
   * @param majorVersion bean major version
   * @param minorVersion bean minor version
   * @return returns a ModelMbean
   */
  public ModelMBean registerServlet(String key,
				    String protocol,
				    Object servlet,
				    Object holder,
				    String name,
				    int majorVersion,
				    int minorVersion) throws JMException;

  /**
   * Destroy a mbean
   * 
   * @param key registry id.
   * @param protocol protocol
   * @param name descriptor id.
   * @param majorVersion bean major version
   * @param minorVersion bean minor version
   * @throws JMException (ie if the mbean does not exist)
   */
  public void unregisterObject(String key,
			       String protocol,
			       String name,
			       int majorVersion,
			       int minorVersion) throws JMException;

  /**
   * Destroy a mbean and its children
   * 
   * @param key registry id.
   * @param protocol protocol
   * @param name descriptor id.
   * @throws JMException (ie if the mbean does not exist)
   */
  public void unregisterObjectAndChildren(String key, String protocol, String name) throws JMException;

  /**
   * Destroy a mbean
   * 
   * @param key registry id.
   * @param protocol protocol
   * @param servlet bean
   */
  public void unregisterServlet(String key, String protocol, Object servlet) throws JMException;
}
