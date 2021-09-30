package com.alcatel.as.service.appmbeans.impl;

import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.j2ee.statistics.TimeStatistic;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.apache.commons.modeler.AttributeInfo;
import org.apache.commons.modeler.BaseModelMBean;
import org.apache.commons.modeler.FieldInfo;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.service.reporter.api.CommandScopes;
import com.alcatel.as.util.cl.ClassLoaderHelper;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

public abstract class ApplicationMBeanFactoryImplBase implements ApplicationMBeanFactory
{
    /**
     * key = bundle name (proxyapp name). value=array of *.mbd found from the bundle.
     */
    protected volatile BundleContext _bctx; // Injected by DM before start()
    protected final static Logger logger = Logger.getLogger("as.service.appmbeans.ApplicationMBeanFactoryImpl");
    protected final static String DEFAULT_DESCRIPTOR_NAME = "default";
    protected MBeanServer server;
    protected ApplicationObjectNameFactory onameFactory;
    protected final static int SERVLET_STATS_NB = 4;

    protected final static String SERVLET_STATS_NAME[] = { "__MaxTime", "__MinTime", "__TotalTime", "__Count" };

    protected final static String SERVLET_STATS_DESCRIPTION[] =
            {
                    "Maximum duration in milliseconds",
                    "Minimum duration in milliseconds",
                    "Total duration in milliseconds",
                    "Number of invocation" };

    protected final static String SERVLET_STATS_TYPE[] = { "long", "long", "long", "long" };

    protected final static String GAUGE_FIELD_NAME = "gauge";
    protected final static String SNMP_FIELD_NAME = "snmp";

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

    public boolean loadDescriptors(String key) throws IOException
    {
      InputStream[] descStreams = getDescriptors(key);
      if (descStreams != null)
      {
        // firt unload then reload mbeans descriptors
        unloadDescriptors(key);
        for (int i = 0; i < descStreams.length; i++)
        {
          loadDescriptors(key, descStreams[i]);
        }
        return true;
      }
      return false;
    }

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
                registerForNewReporter(key, protocol, desc.getName(), source, mbean);
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
                        registerForNewReporter(key, protocol, desc.getName(), source, mbean);
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
                logger.warn(source.getClass().getName() + " is not an instance of "
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

    protected abstract InputStream[] getDescriptors(String key);

    /* ---- Private ----------------------------------------------------------------- */

    public static String getBundleNameFromKey(String id)
    {
        if (id == null)
            return null;

        int index;

        if ((index = id.indexOf("_")) < 0)
            return id;
        else
            return id.substring(0, index);
    }
  
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
     * ---- Implementation for new Reporter ----------------------------------------------
     */

    private void registerForNewReporter(final String key, final String protocol, final String name, final Object source, final ModelMBean mbean)
    {
      MBeanAttributeInfo[] mbAttInfos = ((ModelMBeanInfo)mbean.getMBeanInfo()).getAttributes();
      if ((mbAttInfos == null) || (mbAttInfos.length == 0))
        return;

      Object proxy = null;
      try 
      {
        proxy = createCountersProxy(source, mbAttInfos);
      } 
      catch (Throwable t) 
      {
        logger.warn("Error while creating proxy for counters of "+source, t);
      }
      if (proxy != null) 
      {
        if (logger.isDebugEnabled()) logger.debug("Register application counter handler "+proxy);
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(ConfigConstants.MODULE_NAME, protocol+"/"+key+"/"+name);
        props.put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
        _bctx.registerService(Object.class.getName(), proxy, props);
      }
    }

    private Object createCountersProxy(Object source, MBeanAttributeInfo[] mbAttInfos) throws Exception
    {
      // first check if annotations already present!
      for (Method m : source.getClass().getMethods()) 
      {
        for (java.lang.annotation.Annotation a : m.getAnnotations()) 
        {
          if (Counter.class.equals(a.annotationType())
              || Gauge.class.equals(a.annotationType())) 
          {
            if (logger.isDebugEnabled()) logger.debug(source+" uses management annotations. Register as is. found "+a+" "+m.getName());
            return source;
          }
        }
      }

      // else, use javassist to create an annotated proxy class 
      // with a reference to the source object
      CtClass proxy = null;    
      
      // First, sort mbeans counter/gauges/commands.
      mbAttInfos = sortAttributes(mbAttInfos);
      
      // Now mbean attributes are in the correct order: we can generate annotations with proper index.
      for (int i = 0; i < mbAttInfos.length; i++)
      {
        MBeanAttributeInfo mbAttInfo = mbAttInfos[i];
        String attName = mbAttInfo.getName();
        if (isCounter(mbAttInfo))
        {
          if (proxy == null) proxy = initProxyClass(source);
          //re-create method with its original name!
          String method = new StringBuilder("get")
            .append(attName.substring(0,1).toUpperCase())
            .append(attName.substring(1,attName.length()))
            .toString();

          CtMethod counter = new CtMethod(CtClass.intType, method, null, proxy);
          counter.setBody("return source."+method+"();");
          // add the required management annotation
          javax.management.Descriptor desc = mbAttInfo.getDescriptor();
          String annotationType = "true".equals(desc.getFieldValue("gauge")) ? 
        		  Gauge.class.getName() : Counter.class.getName();
          ConstPool constpool = proxy.getClassFile().getConstPool();
          Annotation annot = new Annotation(annotationType, constpool);

          Object snmpStr = desc.getFieldValue("snmp");
          String snmp = snmpStr != null ? snmpStr.toString() : "";
          annot.addMemberValue("snmpName", new StringMemberValue(snmp, constpool));
          
          Object oidStr = desc.getFieldValue("oid");
          int oid = oidStr != null ? Integer.valueOf(oidStr.toString()) : -1;
          annot.addMemberValue("oid", new IntegerMemberValue(constpool, oid));

          int index = i; // attributes are sorted, we can set index to the current attribute index
          annot.addMemberValue("index", new IntegerMemberValue(constpool, index));

          annot.addMemberValue("desc", new StringMemberValue(mbAttInfo.getDescription(), constpool));
          
          AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
          attr.addAnnotation(annot);
          counter.getMethodInfo().addAttribute(attr);
          proxy.addMethod(counter);
        }
      }
      if (proxy == null)
      {
        if (logger.isDebugEnabled()) logger.debug("No counters found for "+source.getClass().getName());
        return null;
      }

      //instanciate and return the proxy
      byte[] cbytes = proxy.toBytecode();
      Class clazz = new ProxyClassLoader(source.getClass().getClassLoader(), getClass().getClassLoader())
        .overDefineClass(proxy.getName(), cbytes, 0, cbytes.length);
      Constructor constructor = clazz.getConstructor(source.getClass());
      return constructor.newInstance(source);
    }

  /**
   * Sort attributes. First sort by OID, then by Index, then by order.
   * @param attr the array to sort
   * @param highestIndex the highest index we have found, if any, during sorting.
   *        If no index has been foudn, then 0 is returned.
   */
  private MBeanAttributeInfo[] sortAttributes(MBeanAttributeInfo[] attr) {
    if (attr == null) {
      return attr;
    }
    MBeanAttributeInfo modelerType = attr[0];

    SortedMap<Integer, MBeanAttributeInfo> sortedByOid = new TreeMap<Integer, MBeanAttributeInfo>();
    List<MBeanAttributeInfo> sortedByOrder = new ArrayList<MBeanAttributeInfo>();
    List<MBeanAttributeInfo> sorted = new ArrayList<MBeanAttributeInfo>();    

    for (int i = 1; i < attr.length; i ++) {
      if (! isCounter(attr[i])) {
        sortedByOrder.add(attr[i]);
        continue;
      }
      
      Object oid = attr[i].getDescriptor().getFieldValue("oid");
      if (oid != null) {
        sortedByOid.put(Integer.valueOf(oid.toString()), attr[i]);
      } else {
        sortedByOrder.add(attr[i]);
      }
    }
    
    sorted.add(modelerType);
    sorted.addAll(sortedByOid.values());
    sorted.addAll(sortedByOrder);
    
    
    return sorted.toArray(new MBeanAttributeInfo[sorted.size()]);
  }
  
	private CtClass initProxyClass(Object source) throws Exception 
    {
      // create the class
      ClassPool cpool = ClassPool.getDefault();
      CtClass proxy = cpool.makeClass(source.getClass().getName()+source.hashCode()+"__AsrCounterProxy");
      // add the application classpath if needed
      cpool.insertClassPath(new ClassClassPath(source.getClass()));
      // add a field for the source object used inside the proxy
      CtClass srcType = cpool.get(source.getClass().getName());
      CtField srcField = new CtField(srcType, "source", proxy);
      proxy.addField(srcField);
      // add a constructor to set the source object
      CtConstructor construct = new CtConstructor(new CtClass[] {srcType}, proxy);
      construct.setBody("this.source = $1;");
      proxy.addConstructor(construct);
      return proxy;
    }

    // classloader used to delegate instanciation of proxy by the application 
    // (because defineClass is protected)
    static class ProxyClassLoader extends ClassLoader {
      private ClassLoader _parent;
      public ProxyClassLoader(ClassLoader cl1, ClassLoader cl2) { 
        super(cl1); 
        if (cl2 != null) _parent = cl2; 
      }
      public Class overDefineClass(String name, byte[] b, int o, int l) {
        return super.defineClass(name, b, o, l);
      }
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
          return super.loadClass(name);
        } catch(ClassNotFoundException e) {
          if (_parent != null) try {
            return _parent.loadClass(name);
          } catch(ClassNotFoundException e2) {
            return ClassLoader.getSystemClassLoader().loadClass(name);
          } 
          else 
            throw e;
        }
      }
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

     /**
      * Check if specified attribute is a counter.
      * 
      * @return true if attribute is a counter. False if attribute is not a counter.
      */
     private static boolean isCounter(MBeanAttributeInfo info)
     {
       if (!(info instanceof ModelMBeanAttributeInfo))
       {
         return false;
       }

       javax.management.Descriptor desc = ((ModelMBeanAttributeInfo) info).getDescriptor();

       String[] fieldNames = desc.getFieldNames();

       if ((fieldNames == null) || (fieldNames.length == 0))
       {
         return false;
       }

       if (info.getType().compareToIgnoreCase("int") != 0)
       {
         return false;
       }
                     
       return "true".equals(desc.getFieldValue("gauge")) || "true".equals(desc.getFieldValue("counter"));
     }
}
