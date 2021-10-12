// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.appmbeans.implstandalone;

import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Locale;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.j2ee.statistics.TimeStatistic;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.apache.commons.modeler.AttributeInfo;
import org.apache.commons.modeler.BaseModelMBean;
import org.apache.commons.modeler.FieldInfo;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;
import org.apache.log4j.Logger;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.util.cl.ClassLoaderHelper;
import com.alcatel.as.util.config.ConfigHelper;

public class ApplicationMBeanFactoryImpl implements ApplicationMBeanFactory
{

    private final static Logger logger = Logger.getLogger(ApplicationMBeanFactoryImpl.class);

    private final static String DEFAULT_DESCRIPTOR_NAME = "default";

    private MBeanServer server;

    private ApplicationObjectNameFactory onameFactory;

    private final static int SERVLET_STATS_NB = 4;

    private final static String SERVLET_STATS_NAME[] = { "__MaxTime", "__MinTime", "__TotalTime", "__Count" };

    private final static String SERVLET_STATS_DESCRIPTION[] =
            {
                    "Maximum duration in milliseconds",
                    "Minimum duration in milliseconds",
                    "Total duration in milliseconds",
                    "Number of invocation" };

    private final static String SERVLET_STATS_TYPE[] = { "long", "long", "long", "long" };

    private final static String GAUGE_FIELD_NAME = "gauge";
    private final static String SNMP_FIELD_NAME = "snmp";

    /* --- Dependency lifecycle ------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    protected void bind(Dictionary systemConfig)
    {
        ApplicationObjectNameFactory.setParent(ConfigHelper.getString(systemConfig, GROUP_NAME) + "__"
                + ConfigHelper.getString(systemConfig, INSTANCE_NAME));
        ApplicationObjectNameFactory.setPid(ConfigHelper.getString(systemConfig, INSTANCE_PID));
    }

    protected void start()
    {
        // get the MBean server
        server = ManagementFactory.getPlatformMBeanServer();
        // ObjectName factory
        onameFactory = new ApplicationObjectNameFactory();
        // Use local registries (one registry per key)
        Registry.setUseContextClassLoader(true);
        // 
        logger.debug("ApplicationMBeanFactory started");
    }

    protected void stop()
    {
        logger.debug("ApplicationMBeanFactory deactivated");
    }

    /* --- ApplicationMBeanFactory service ------------------------------------------- */

    public void loadDescriptors(String key, URL url) throws IOException
    {
        Registry registry = Registry.getRegistry(key, null);
        try
        {
            registry.loadMetadata(url);
            if (logger.isDebugEnabled())
            {
                logger.debug("loadDescriptors " + key + " - " + url.toString());
                logger.debug("descriptors: " + Arrays.toString(registry.findManagedBeans()));
            }
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage());
        }
    }

    public void loadDescriptors(String key, InputStream inputStream) throws IOException
    {
        Registry registry = Registry.getRegistry(key, null);
        // the registry uses javax.xml.parsers.DocumentBuilderFactory and we should protect
        // against invalid/evil thread context class loader ...
        ClassLoader currCL = ClassLoaderHelper.setContextClassLoader(null);
        try
        {
            registry.loadMetadata(inputStream);
            if (logger.isDebugEnabled())
            {
                logger.debug("loadDescriptors " + key + " - " + inputStream.toString());
                logger.debug("descriptors: " + Arrays.toString(registry.findManagedBeans()));
            }
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage());
        }
        finally
        {
            ClassLoaderHelper.setContextClassLoader(currCL);
        }
    }

    public void unloadDescriptors(String key)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("unloadDescriptors " + key);
        }
        try
        {
            Registry.getRegistry(key, null).resetMetadata();
        }
        catch (Exception ignored)
        {
        }
    }

    public ModelMBean registerObject(String key,
                                     String protocol,
                                     Object source,
                                     String name,
                                     int major,
                                     int minor) throws JMException
    {
        ClassLoader currCL = ClassLoaderHelper.setContextClassLoader(this.getClass().getClassLoader());
        try
        {
            ManagedBean desc = getDescriptor(key, name, source);
            if (desc != null)
            {
                desc.setClassName(ApplicationModelMBean.class.getName());
                ModelMBean mbean = desc.createMBean(source);
                
                // If the mbean name is different from the name passed to the registerObject, then 
                // use the mbean name, instead of the one passed to our method.
                if (!name.equals(desc.getName()))
                {
                    // Here, the mbean name (specified in the mbeans-descriptor seems to match the
                    // servlet CLASSNAME, not the servlet name, specified in the web.xml file. 
                    // So, we'll use the class name as the mbean name.

                    if (logger.isInfoEnabled())
                    {
                        logger.info("Will register mbean with name=" + desc.getName());
                    }
                    name = desc.getName();
                }

                ObjectName oname = onameFactory.createObjectName(key, protocol, name, major, minor);
                server.registerMBean(mbean, oname);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Created mbean: " + oname.toString());
                }
                return mbean;
            }
            else
            {
                logger.info("No descriptor found for " + key + "/" + protocol + "/" + name);
            }
        }
        catch (InstanceAlreadyExistsException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            logger.debug(Thread.currentThread().getContextClassLoader().toString(), e);
            throw new JMException(e.getMessage());
        }
        finally
        {
            ClassLoaderHelper.setContextClassLoader(currCL);
        }
        return null;
    }

    public ModelMBean registerServlet(String key, String protocol, Object servlet) throws JMException
    {
        return registerServlet(key, protocol, servlet, null);
    }

    public ModelMBean registerServlet(String key, String protocol, Object source, Object holder)
        throws JMException
    {
        if (source instanceof Servlet)
        {
            Servlet servlet = (Servlet) source;
            ServletConfig config = servlet.getServletConfig();
            String name = config.getServletName();
            int majorVersion = config.getServletContext().getMajorVersion();
            int minorVersion = config.getServletContext().getMinorVersion();
            return registerServlet(key, protocol, source, holder, name, majorVersion, minorVersion);
        }
        return null;
    }

    public ModelMBean registerServlet(String key,
                                      String protocol,
                                      Object source,
                                      Object holder,
                                      String name,
                                      int majorVersion,
                                      int minorVersion) throws JMException
    {
        ClassLoader currCL = ClassLoaderHelper.setContextClassLoader(this.getClass().getClassLoader());
        try
        {
            if (source instanceof Servlet)
            {
                Servlet servlet = (Servlet) source;
                boolean servletStats = false;
                Method timeService = null;
                ManagedBean desc = getDescriptor(key, name, servlet);
                if (desc != null && !name.equals(desc.getName()))
                {
                    // Here, the mbean name (specified in the mbeans-descriptor seems to match the
                    // servlet CLASSNAME, not the servlet name, specified in the web.xml file. 
                    // So, we'll use the class name as the mbean name.
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Will register mbean with name=" + desc.getName());
                    }
                    name = desc.getName();
                }
                if ((desc != null) || (holder != null))
                {
                    // JSR077
                    if (holder != null)
                    {
                        // If the holder provides stats info, add some attributes
                        try
                        {
                            timeService =
                                    holder.getClass().getDeclaredMethod("getServiceTime", (Class[]) null);
                            if ((timeService != null)
                                    && (timeService.getReturnType().equals(TimeStatistic.class)))
                            {
                                servletStats = true;
                                // if desc is null, create a default descriptor
                                if (desc == null)
                                {
                                    desc = new ManagedBean();
                                    desc.setName(DEFAULT_DESCRIPTOR_NAME);
                                    desc.setType(servlet.getClass().getName());
                                    desc.setDescription(name);
                                }
                                for (int i = 0; i < SERVLET_STATS_NB; i++)
                                {
                                    AttributeInfo servletStatInfo = new AttributeInfo();
                                    servletStatInfo.setName(SERVLET_STATS_NAME[i]);
                                    servletStatInfo.setDescription(SERVLET_STATS_DESCRIPTION[i]);
                                    servletStatInfo.setType(SERVLET_STATS_TYPE[i]);
                                    servletStatInfo.setWriteable(false);
                                    FieldInfo gaugeField = new FieldInfo();
                                    gaugeField.setName(GAUGE_FIELD_NAME);
                                    gaugeField.setValue("true");
                                    servletStatInfo.addField(gaugeField);
                                    FieldInfo snmpField = new FieldInfo();
                                    snmpField.setName(SNMP_FIELD_NAME);
                                    snmpField.setValue(buildSnmpValue(key, protocol, name));
                                    servletStatInfo.addField(snmpField);
                                    desc.addAttribute(servletStatInfo);
                                }
                            }
                        }
                        catch (NoSuchMethodException e)
                        {
                            logger.debug(holder.getClass().getName() + " is not implementing ServletStats");
                        }
                    }
                    if (desc != null)
                    {
                        // Create the mbean
                        desc.setClassName(ApplicationModelMBean.class.getName());
                        ObjectName oname =
                                onameFactory.createObjectName(key, protocol, name, majorVersion, minorVersion);
                        ModelMBean mbean = desc.createMBean(servlet);
                        if (servletStats)
                            ((ApplicationModelMBean) mbean).setTimeService(holder, timeService);
                        // register the mbean
                        server.registerMBean(mbean, oname);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Mbean " + oname.toString() + " created. ServletContext attribute="
                                    + ApplicationMBeanFactory.ATTR_PREFIX_MODEL_MBEAN + name);
                        }
                        return mbean;
                    }
                }
                else
                {
                    logger.info("No descriptor found for " + key + "/" + protocol + "/" + name);
                }
            }
            else
            {
                logger.error(source.getClass().getName() + " is not an instance of "
                            + Servlet.class.getName());
                }
            }
        catch (InstanceAlreadyExistsException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            logger.debug(Thread.currentThread().getContextClassLoader().toString(), e);
            throw new JMException(e.getMessage());
        }
        finally
        {
            ClassLoaderHelper.setContextClassLoader(currCL);
        }
        return null;
    }

    public void unregisterObject(String key, String protocol, String name, int major, int minor)
        throws JMException
    {
        ObjectName oname = onameFactory.createObjectName(key, protocol, name, major, minor);
        server.unregisterMBean(oname);
        if (logger.isDebugEnabled())
        {
            logger.debug("Mbean " + oname.toString() + " destroyed");
        }
    }

    public void unregisterObjectAndChildren(String key, String protocol, String name) throws JMException
    {
        // TODO Implement method unregisterObjectAndChildren
        throw new JMException("Not yet implemented");
    }

    public void unregisterServlet(String key, String protocol, Object source) throws JMException
    {
        if (source instanceof Servlet)
        {
            Servlet servlet = (Servlet) source;
            ServletConfig config = servlet.getServletConfig();
            unregisterObject(key, protocol, config.getServletName(), config.getServletContext()
                    .getMajorVersion(), config.getServletContext().getMinorVersion());
        }
    }

    /**
     * @param o the object from which to find a MBean interface
     * @return The MBean class
     */
    // private Class getStandardMBean(Object o) {
    // List list = Arrays.asList(o.getClass().getInterfaces());
    // if (logger.isDebugEnabled()) {
    // logger.debug("Try to find a MBean interfaces for " + o + "=" + list);
    // }
    // try {
    // Class objectclass = Class.forName(o.getClass().getName() + "MBean");
    //
    // if (list.contains(objectclass)) {
    // return objectclass;
    // }
    // } catch (Exception ex) {
    // }
    // Iterator it = list.iterator();
    // while (it.hasNext()) {
    // Class c = (Class) it.next();
    // if (c.getName().endsWith("MBean")) {
    // return c;
    // }
    // }
    // return null;
    // }
    /* ---- Private ----------------------------------------------------------------- */

    private ManagedBean getDescriptor(String key, String name, Object o) throws Exception
    {
        Registry registry = Registry.getRegistry(key, null);
        if (logger.isInfoEnabled())
        {
            logger.info("getDescriptor: key=" + key + ", name=" + name + ", mbeans="
                    + Arrays.toString(registry.findManagedBeans()));
        }
        ManagedBean desc = registry.findManagedBean(name);

        if (desc == null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("NOT FOUND: looking for servlet name " + o.getClass().getSimpleName());
            }
            desc = registry.findManagedBean(o.getClass().getSimpleName());
            if (desc == null)
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("NOT FOUND: looking for " + DEFAULT_DESCRIPTOR_NAME);
                }
            // Class clazz = getStandardMBean(o);
            // if (clazz != null) {
            // desc = registry.findManagedBean(o, clazz, null);
            // if ((desc!=null) && logger.isInfoEnabled()) {
            // logger.info("MBean profile found for " + name + ": " + clazz);
            // }
            // }
            // else {
            desc = registry.findManagedBean(DEFAULT_DESCRIPTOR_NAME);
                if ((desc != null))
                {
                    if (logger.isInfoEnabled())
            {
                logger.info("use " + DEFAULT_DESCRIPTOR_NAME + " profile for " + name);
            }
                }
                else
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("NOT FOUND for " + DEFAULT_DESCRIPTOR_NAME);
                    }
                }
            }
            // }
        }

        if (desc != null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("FOUND MBEAN: name=" + desc.getName());
            }
        }
        return desc;
    }

    private String buildSnmpValue(String key, String protocol, String name)
    {
        StringBuffer buffer = new StringBuffer(SNMP_FIELD_NAME);
        buffer.append('.');
        buffer.append(protocol.replace(' ', '_'));
        buffer.append('.');
        buffer.append(key.replace(' ', '_'));
        buffer.append('.');
        buffer.append(name.replace(' ', '_'));
        return buffer.toString();
    }

    /*
     * ---- ApplicationModelMBean -----------------------------------------------------------------
     */

    public static class ApplicationModelMBean extends BaseModelMBean
    {

        private Object resource;
        private Object holder;
        private Method timeService;

        // Map containing the different Methods of our managed resource
        private HashMap<String, Method> mMap = new HashMap<String, Method>();
        private HashMap<String, Method> mMapHolder;

        public ApplicationModelMBean() throws MBeanException, RuntimeOperationsException
        {
            super();
        }

        public ApplicationModelMBean(ModelMBeanInfo info) throws MBeanException, RuntimeOperationsException
        {
            super(info);
        }

        public void setTimeService(Object holder, Method timeService)
        {
            this.holder = holder;
            this.timeService = timeService;
            mMapHolder = new HashMap<String, Method>();
            Method[] methods = holder.getClass().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++)
            {
                Method m = methods[i];
                mMapHolder.put(m.getName(), m);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("methods of " + holder.toString() + ":" + mMapHolder);
            }
            logger.info("timeService set");
        }

        /*
         * (non-Javadoc)
         * @see org.apache.commons.modeler.BaseModelMBean#setManagedResource(java.lang.Object,
         * java.lang.String)
         */
        @Override
        public void setManagedResource(Object instance, String type)
            throws InstanceNotFoundException, InvalidTargetObjectTypeException, MBeanException,
            RuntimeOperationsException
        {
            this.resource = instance;

            Method[] methods = instance.getClass().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++)
            {
                Method m = methods[i];
                mMap.put(m.getName(), m);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("methods of " + instance.toString() + ":" + mMap);
            }
            super.setManagedResource(instance, type);
        }

        /*
         * (non-Javadoc)
         * @see org.apache.commons.modeler.BaseModelMBean#getAttribute(java.lang.String)
         */
        @Override
        public Object getAttribute(String name)
            throws AttributeNotFoundException, MBeanException, ReflectionException
        {
            if (timeService != null)
            {
                // holder attribute ?
                if (name.compareTo(SERVLET_STATS_NAME[0]) == 0)
                {
                    try
                    {
                        TimeStatistic timeStatistic =
                                (TimeStatistic) timeService.invoke(holder, (Object[]) null);
                        return timeStatistic.getMaxTime();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        throw new ReflectionException(e);
                    }
                }
                if (name.compareTo(SERVLET_STATS_NAME[1]) == 0)
                {
                    try
                    {
                        TimeStatistic timeStatistic =
                                (TimeStatistic) timeService.invoke(holder, (Object[]) null);
                        return timeStatistic.getMinTime();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        throw new ReflectionException(e);
                    }
                }
                if (name.compareTo(SERVLET_STATS_NAME[2]) == 0)
                {
                    try
                    {
                        TimeStatistic timeStatistic =
                                (TimeStatistic) timeService.invoke(holder, (Object[]) null);
                        return timeStatistic.getTotalTime();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        throw new ReflectionException(e);
                    }
                }
                if (name.compareTo(SERVLET_STATS_NAME[3]) == 0)
                {
                    try
                    {
                        TimeStatistic timeStatistic =
                                (TimeStatistic) timeService.invoke(holder, (Object[]) null);
                        return timeStatistic.getCount();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        throw new ReflectionException(e);
                    }
                }
            }
            // resource attribute ?
            Method m = mMap.get(accessor(name, 'g'));
            if (m != null)
            {
                ClassLoader currThreadCL = Thread.currentThread().getContextClassLoader();
                try
                {
                    Thread.currentThread().setContextClassLoader(resource.getClass().getClassLoader());
                    Object o = m.invoke(resource, (Object[]) null);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("invoking " + m.getName() + " on " + resource.toString() + "=>"
                                + ((o == null) ? "null" : o.toString()));
                    }
                    return o;
                }
                catch (Throwable e)
                {
                    throw new ReflectionException(new Exception("Caught exception from " + resource, e));
                }
                finally
                {
                    Thread.currentThread().setContextClassLoader(currThreadCL);
                }
            }
            else
            {
                // holder attribute ?
                if (holder != null)
                {
                    m = mMapHolder.get(accessor(name, 'g'));
                    if (m != null)
                    {
                        ClassLoader currThreadCL = Thread.currentThread().getContextClassLoader();
                        try
                        {
                            Thread.currentThread().setContextClassLoader(holder.getClass().getClassLoader());
                            Object o = m.invoke(holder, (Object[]) null);
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("invoking " + m.getName() + " on " + holder.toString() + "=>"
                                        + ((o == null) ? "null" : o.toString()));
                            }
                            return o;
                        }
                        catch (Throwable e)
                        {
                            throw new ReflectionException(new Exception("Caught exception from " + holder, e));
                        }
                        finally
                        {
                            Thread.currentThread().setContextClassLoader(currThreadCL);
                        }
                    }
                }
                return super.getAttribute(name);
            }
        }

        /*
         * (non-Javadoc)
         * @see org.apache.commons.modeler.BaseModelMBean#setAttribute(javax.management.Attribute)
         */
        @Override
        public void setAttribute(Attribute attr)
            throws AttributeNotFoundException, MBeanException, ReflectionException
        {
            String name = attr.getName();
            Method m = mMap.get(accessor(name, 's'));
            if (m == null)
            {
                super.setAttribute(attr);
            }
            else
            {
                try
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("invoking " + m.getName() + " on " + resource.toString());
                    }
                    m.invoke(resource, new Object[] { attr.getValue() });
                }
                catch (Throwable t)
                {
                    throw new ReflectionException(new Exception("Exception caught from " + resource, t));
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see org.apache.commons.modeler.BaseModelMBean#invoke(java.lang.String, java.lang.Object[],
         * java.lang.String[])
         */
        @Override
        public Object invoke(String name, Object[] params, String[] signature)
            throws MBeanException, ReflectionException
        {
            Method m = mMap.get(name);
            if (m == null)
            {
                return super.invoke(name, params, signature);
            }
            else
            {
                try
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("invoking " + m.getName() + " on " + resource.toString());
                    }
                    return m.invoke(resource, params);
                }
                catch (Throwable e)
                {
                    throw new ReflectionException(new Exception("Caught exception from " + resource, e));
                }
            }
        }

        private static String accessor(String s, char f)
        {
            if (s.length() == 0)
            {
                return s;
            }
            else
            {
                StringBuffer sb = new StringBuffer();
                sb.append(f);
                sb.append("et");
                sb.append(s.substring(0, 1).toUpperCase(Locale.getDefault()));
                sb.append(s.substring(1));
                return sb.toString();
            }
        }

    }

    @Override
    public boolean loadDescriptors(String key) throws IOException
    {
        // TODO Auto-generated method stub
        return false;
    }
}
