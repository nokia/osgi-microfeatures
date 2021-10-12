// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.mbeanparser.impl;

import static com.alcatel.as.service.metatype.PropertyDescriptor.DEFAULT_VALUE;
import static com.alcatel.as.service.metatype.PropertyDescriptor.FILEDATA;
import static com.alcatel.as.service.metatype.PropertyDescriptor.FILENAME;
import static com.alcatel.as.service.metatype.PropertyDescriptor.NAME;
import static com.alcatel.as.service.metatype.PropertyDescriptor.TYPE;
import static com.alcatel.as.service.metatype.PropertyDescriptor.VALUE;
import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.SERVICE_PID;

import com.alcatel_lucent.as.management.annotation.alarm.AlarmType ;
import com.alcatel_lucent.as.management.annotation.alarm.ProbableCause ;
import com.alcatel_lucent.as.management.annotation.alarm.DiscriminatingFields ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.modeler.AttributeInfo;
import org.apache.commons.modeler.NotificationInfo;
import org.apache.commons.modeler.OperationInfo;
import org.apache.commons.modeler.FieldInfo;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;

import com.alcatel.as.service.metatype.MetaData;
import com.alcatel.as.service.metatype.MetatypeParser;
import com.alcatel.as.service.metatype.PropertiesDescriptor;
import com.alcatel.as.service.metatype.CounterDescriptor;
import com.alcatel.as.service.metatype.AlarmDescriptor;
import com.alcatel.as.service.metatype.CommandDescriptor;
import com.alcatel.as.util.cl.ClassLoaderHelper;

public class MBeanParserImpl implements MetatypeParser
{
    static final String PROP_MBEAN = "ProxyAppPropMBean";
    static final String BUNDLE_LOCATION = "Bundle-Location";
    static final Logger _logger = Logger.getLogger("as.management.mbean.parser");

    private final Map<String, MetaData> cache = new Hashtable<String, MetaData>();
    
    //marker for parsed bundle with no metadata
    private final MetaData NOMETADATA = new MetaData() {
        public String getBundleName() { return null; }
        public String getBundleSymbolicName() { return null; }
        public String getBundleVersion() { return null; }
        public Map<String, PropertiesDescriptor> getProperties() { return null; }
        public Map<String, CounterDescriptor> getCounters() { return null; }
        public Map<String, AlarmDescriptor> getAlarms() { return null; }
        public Map<String, CommandDescriptor> getCommands() { return null; }
    };

    /////////////////////////////////////
    // implements MetatypeParser
    /////////////////////////////////////
    
    public MetaData loadMetadata(Object source, boolean usecache) throws Exception {
    	final JarBundle b = new JarBundle(source);

        MetaData cached = cache.get(b.getID());
        
        if(usecache) {
	        if (cached != null) 
	        {
	          if (_logger.isDebugEnabled()) _logger.debug("return cached metadata for "+b.getID());
	          return cached == NOMETADATA
	            ? null
	            : cached;
	        }
        }

        final MetaDataImpl metadata = new MetaDataImpl(b.getBundleName(), 
                                                       b.getBundleSymbolicName(), 
                                                       b.getBundleVersion());
        Map<String, List<Map<String, String>>> res = null;
        List<Map<String, String>> unsorted = null;
        Map<String, CounterDescriptor> counters = new HashMap<String, CounterDescriptor>();
        Map<String, AlarmDescriptor> alarms = new HashMap<String, AlarmDescriptor>();
        Map<String, CommandDescriptor> commands = new HashMap<String, CommandDescriptor>();

        for (MBDEntry mbd : b.getMBeansDescriptorsEntries())
        {
            Registry registry = loadMetadata(mbd, b);

            Map<String, CounterDescriptor> counts = loadCounters(b, mbd.pid, registry, metadata);
            if (counts != null) counters.putAll(counts);

            Map<String, AlarmDescriptor> alrms = loadAlarms(b, mbd.pid, registry, metadata);
            if (alrms != null) alarms.putAll(alrms);
            
            Map<String, CommandDescriptor> cmds = loadCommands(b, mbd.pid, registry, metadata);
            if (cmds != null) commands.putAll(cmds);
            
            List<Map<String, String>> props = loadProperties(b, mbd.pid, registry);
            if (props == null)
            {
                continue;
            }
            //pid could be specified in filename OR in domain attribute and set in a temporary fake attribute
            //(see loadProperties())
            //always remove this fake property
            String pid = props.remove(0).get(SERVICE_PID);
            if (_logger.isDebugEnabled())
                _logger.debug("bundle " + b.toString() + " adds properties "
                        + (pid != null ? pid : "unsorted") + ":" + props);

            if (pid != null)
            {
                if (res == null)
                    res = new HashMap<String, List<Map<String, String>>>();
                res.put(pid, props);
            }
            else
            {
                if (unsorted == null)
                    unsorted = new ArrayList<Map<String, String>>();
                unsorted.addAll(props);
            }
        }

        // now sort remaining properties with no pid
        if (unsorted != null)
            for (Map<String, String> prop : unsorted)
            {
                String pid = prop.get(NAME);
                if (pid == null)
                {
                    _logger.warn("skipping invalid property with no name in " + b.toString() + ":" + prop,
                                 new Exception());
                    continue;
                }
                int dot = pid.indexOf('.');
                if (dot == -1)
                {
                    _logger.warn("skipping invalid property with no prefix in name in " + b.toString() + ":"
                            + prop, new Exception());
                    continue;
                }
                pid = pid.substring(0, dot);
                if (res == null)
                    res = new HashMap<String, List<Map<String, String>>>();
                List<Map<String, String>> list = res.get(pid);
                if (list == null){
                    list = new ArrayList<Map<String, String>>();
		    res.put(pid,list);
		}
                list.add(prop);
            }

        if (res != null)
        {
            Map<String, PropertiesDescriptor> properties = new HashMap<String, PropertiesDescriptor>();
            if (res != null) for (Map.Entry<String, List<Map<String, String>>> entry : res.entrySet())
            {
                properties.put(entry.getKey(), newPropertiesDescriptor(b, entry.getKey(), entry.getValue()));
            }
            metadata.setProperties(properties);
        }
        if (counters != null) metadata.setCounters(counters);
        if (alarms != null) metadata.setAlarms(alarms);
        if (commands != null) metadata.setCommands(commands);

        if (!metadata.isEmpty())
        {
            if (_logger.isDebugEnabled()) _logger.debug("caching parsed metadata for "+b.getID());
            cache.put(b.getID(), metadata);
            return metadata;
        }
        if (_logger.isDebugEnabled())
            _logger.debug("No metadata found in " + b.toString());
        cache.put(b.getID(), NOMETADATA); // remember this as well
        return null;
    }
    

    /** load MetaData (properties, counters, alarms) from a jar or bundle */
    public MetaData loadMetadata(Object source) throws Exception
    {
        return loadMetadata(source, true);
    }

    /////////////////////////////////////
    // helpers
    /////////////////////////////////////

    /** one PropertiesDescriptor = one pid */
    private PropertiesDescriptor newPropertiesDescriptor(final JarBundle b, String pid, List list)
    {
        return new MBeanDescriptorImpl(pid, list, 
            b.getBundleName(), b.getBundleSymbolicName(), b.getBundleVersion(), b.getFileName());
    }

    /** parse one mbeans-descriptors and return the raw Registry */
    private Registry loadMetadata(MBDEntry mbd, JarBundle b)
    {
        Registry registry = Registry.getRegistry(mbd.hashCode(), null);//FIXME use of "guard" as second argument???
        registry.resetMetadata();

        ClassLoader currCL = ClassLoaderHelper.setContextClassLoader(null);
        try
        {
            registry.loadMetadata(mbd.in);
        }
        catch (Exception e)
        {
            _logger.warn("Cannot load metadata from "
                    + (mbd.pid != null ? mbd.pid : "mbeans-descriptors.xml") + " in " + b.getLocation(), e);
        }
        finally
        {
            ClassLoaderHelper.setContextClassLoader(currCL);
            try
            {
                mbd.in.close();
            }
            catch (IOException e)
            {
            }
        }
        return registry;
    }

    /** load Registry entries into a list 
     *  each Map is a single property with its description attributes 
     *  @param pid the filename pid, if any or null (if the file is mbeans-descriptors.xml)
     */
    private List<Map<String, String>> loadProperties(JarBundle b, String pid, Registry registry)
        throws Exception
    {
        // PROP_MBEAN has been set in loadMetadata()
        // if properties were found in the mbeans-descriptors
        // (but it may only contain counters and alarms)
        ManagedBean mBean = registry.findManagedBean(PROP_MBEAN);
        if (mBean == null)
        {
            return null;
        }

        List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        AttributeInfo[] properties = mBean.getAttributes();

        // use "domain" attribute of the mbean tag (if specified) 
        // as the service.pid used to register this config
        final String domain = mBean.getDomain();
        if (domain != null)
        {
            if (_logger.isDebugEnabled())
                _logger.debug("override filename pid '" + pid + "' with specified domain pid '" + domain
                        + "'");
            pid = domain;
        }

        final String spid = pid;
        list.add(new HashMap()
        {
            {
                put(SERVICE_PID, spid); //maybe null if argument pid was null but it's ok
            }
        });

        // WARNING: The first attribut of the ManagedBean is always added by the implementation
        // with the name "modelerType". So, we have to skip it !
        final int SKIP_NONREALDATA = 1;
        for (int i = SKIP_NONREALDATA; i < properties.length; i++)
        {
            AttributeInfo propInfo = properties[i];

            HashMap map = new HashMap();
            list.add(map);

            map.put(NAME, propInfo.getName());
            String defaultValue = "";
            String type = null;
            for (Iterator it = propInfo.getFields().iterator(); it.hasNext();)
            {
                FieldInfo field = (FieldInfo) it.next();
                String fname = (String) field.getName();
                String fvalue = (String) field.getValue();
                if (DEFAULT_VALUE.equalsIgnoreCase(fname))
                {
                    defaultValue = fvalue;
                    map.put(VALUE, fvalue);
                }
                else
                {
                    map.put(fname, fvalue);
                    if (TYPE.equalsIgnoreCase(fname))
                        type = fvalue;
                }
            }
            //FIXME.. see everything done by com.nextenso.mgmt.bos.ProxyAppPropDesc
            if (FILEDATA.equalsIgnoreCase(type) && defaultValue != null && defaultValue.length() > 0)
            {
                map.put(FILENAME, defaultValue); //keep filename so it can be saved again elsewhere
                map.put(VALUE, loadFile(defaultValue, b));
            }
        }

        return list;
    }

    private Map<String, CounterDescriptor> loadCounters(JarBundle b, String pid, Registry registry, final MetaData md)
    {
        Map<String, CounterDescriptor> counters = new HashMap<String, CounterDescriptor>();
        for (String beanName : registry.findManagedBeans()) if (!PROP_MBEAN.equals(beanName)) //all except properties
        {
          ManagedBean mbean = registry.findManagedBean(beanName);
          AttributeInfo[] attrs = mbean.getAttributes();
          if (attrs != null) for (AttributeInfo attr : attrs)
          {
            final String source = mbean.getType();
            final String name = attr.getName();
            final String desc = attr.getDescription();
            String type = null;

            for (Iterator<FieldInfo> it = attr.getFields().iterator(); it.hasNext(); )
            {
              FieldInfo field = it.next();
              if ("Counter".equalsIgnoreCase(field.getName())) type = "Counter";
              else if ("Gauge".equalsIgnoreCase(field.getName())) type = "Gauge";
            }
            final String $type = type;

            if (type != null) counters.put(name, new CounterDescriptor() {
              public MetaData getParent() { return md; }
              public String getSource() { return source; }
              public String getName() { return source+"."+name; }
              public String getType() { return $type; }
              public String getDescription() { return desc; }
            });
          }
        }
        if (_logger.isDebugEnabled() && !counters.isEmpty()) _logger.debug(b+" declares counters "+counters.keySet());
        return counters;
    }

    private Map<String, AlarmDescriptor> loadAlarms(JarBundle b, String pid, Registry registry, final MetaData md)
    {
        Map<String, AlarmDescriptor> alarms = new HashMap<String, AlarmDescriptor>();
        for (String beanName : registry.findManagedBeans()) if (!PROP_MBEAN.equals(beanName)) //all except properties
        {
          ManagedBean mbean = registry.findManagedBean(beanName);
          NotificationInfo[] notifs = mbean.getNotifications();
          if (notifs != null) for (NotificationInfo notif : notifs)
          {
            final String source = mbean.getType();
            final String name = notif.getName();
            final String desc = notif.getDescription();
            int code = -1;
            int severity = -1;
            String msg = "";
            AlarmType alarmType = null ;
            ProbableCause probableCause = null ;
            int discriminatingFields = DiscriminatingFields.DEFAULT ;

            for (Iterator<FieldInfo> it = notif.getFields().iterator(); it.hasNext(); )
            {
              FieldInfo field = it.next();
              if ("messageID".equalsIgnoreCase(field.getName())) {
                try {
                  code = Integer.parseInt(field.getValue().toString());
                } catch (Exception e) {
                  _logger.warn("Invalid messageID in notification "+name+" of bundle "+b, e);
                }
              } else if ("severity".equalsIgnoreCase(field.getName())) {
                try {
                  severity = Integer.parseInt(field.getValue().toString());
                } catch (Exception e) {
                  _logger.warn("Invalid severity in notification "+name+" of bundle "+b, e);
                }
              } else if ("alarmType".equalsIgnoreCase(field.getName())) {
                alarmType = AlarmType.get (field.getValue().toString()) ;
              } else if ("probableCause".equalsIgnoreCase(field.getName())) {
                probableCause = ProbableCause.get (field.getValue().toString()) ;
              } else if ("discriminatingFields".equalsIgnoreCase(field.getName())) {
                try {
                  discriminatingFields = Integer.parseInt(field.getValue().toString());
                } catch (Exception e) {
                  _logger.warn("Invalid discriminatingFields in notification "+name+" of bundle "+b, e);
                }
              } else if ("message".equalsIgnoreCase(field.getName())) {
                msg = field.getValue().toString();
              }
            }
            final int $code = code;
            final int $severity = severity;
            final String $msg = msg;
            final AlarmType $alarmType = (alarmType == null) ? AlarmType.DEFAULT : alarmType ;
            final ProbableCause $probableCause = (probableCause == null) ? ProbableCause.DEFAULT : probableCause ;
            final int $discriminatingFields = discriminatingFields;

            alarms.put(name, new AlarmDescriptor() {
              public MetaData getParent() { return md; }
              public String getSource() { return source; }
              public String getName() { return name; }
              public int getCode() { return $code; }
              public int getSeverity() { return $severity; }
              public String getMessage() { return $msg; }
              public String getDescription() { return desc; }
              public AlarmType getAlarmType() { return $alarmType; }
              public ProbableCause getProbableCause() { return $probableCause; }
              public int getDiscriminatingFields() { return $discriminatingFields; }
            });
          }
        }
        if (_logger.isDebugEnabled() && !alarms.isEmpty()) _logger.debug(b+" declares alarms "+alarms.keySet());
        return alarms;
    }

    private Map<String, CommandDescriptor> loadCommands(JarBundle b, String pid, Registry registry, final MetaData md)
    {
        Map<String, CommandDescriptor> commands = new HashMap<String, CommandDescriptor>();
        for (String beanName : registry.findManagedBeans()) if (!PROP_MBEAN.equals(beanName)) //all except properties
        {
          ManagedBean mbean = registry.findManagedBean(beanName);
          OperationInfo[] ops = mbean.getOperations();
          if (ops != null) for (OperationInfo operation : ops)
          {
            final String source = mbean.getType();
            final String name = operation.getName();
            final String desc = operation.getDescription();

            commands.put(name, new CommandDescriptor() {
              public MetaData getParent() { return md; }
              public String getSource() { return source; }
              public String getName() { return name; }
              public String getDescription() { return desc; }
            });
          }
        }
        if (_logger.isDebugEnabled() && !commands.isEmpty()) _logger.debug(b+" declares commands "+commands.keySet());
        return commands;
    }

    private String loadFile(String filename, JarBundle b)
    {
        String def = "";
        InputStream in = null;
        try
        {
            in = b.getInputStream(filename);
            if (in == null)
            {
                _logger.info("Entry " + filename + " not found from bundle " + b);
                return def;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String tmp;
            while ((tmp = reader.readLine()) != null)
            {
                sb.append(tmp).append("\n");
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            _logger.warn("Entry " + filename + " cannot be loaded from bundle " + b, e);
        }
        finally
        {
            if (in != null)
                try
                {
                    in.close();
                }
                catch (Exception ee)
                {
                    _logger.warn("Failed to close stream: " + filename + " in " + b, ee);
                }
        }
        return def;
    }

    // FIXME (should we really keep this method here ?)
    /*
    private boolean useMonitor(JarBundle b) {
      // Check whether the bundle imports the Monitors service. 
      String imports = (String) b.getHeaders().get("Import-Package");
      if (imports != null && imports.indexOf(Sensors.class.getPackage().getName()) != -1) {
        String name = b.getSymbolicName();
        _logger.debug("bundle " + name + " imports the package " + Sensors.class.getPackage() +
    	    ": Automatic generation and registration of MBeans.");
        return true;
      }
      return false;
    }

    // FIXME (should we really keep this method here ?)
    private void registerCounters(JarBundle b) {
      String version = (String)b.getHeaders().get(BUNDLE_VERSION);
      if(version == null) version = "1.0.0";
      String bsn = (String)b.getHeaders().get(BUNDLE_SYMBOLICNAME);
      String key = bsn+"_"+version;
      // lookup ManagedBean in the registry
      Registry registry = Registry.getRegistry(key, null);
      String[] mBeanNames = registry.findManagedBeans();
      if(mBeanNames != null) for(String mBeanName : mBeanNames) {
        if(PROP_MBEAN.equals(mBeanName))
    continue; //skip properties mbean

        ManagedBean mBean = registry.findManagedBean(mBeanName);

        // use javassist to generate the mbean that implements counters based on Monitors API
        try {
    ClassPool cp = ClassPool.getDefault();
    CtClass mbean = cp.makeClass(key+"."+mBeanName);
    for(AttributeInfo counter : mBean.getAttributes()) {
      CtMethod getter = new CtMethod(CtClass.longType, "get"+counter.getName(), null, mbean);
      StringBuffer body = new StringBuffer();
      body.append("return Monitors.getInstance().getCounter(\"")
        .append(counter.getName())
        .append("\").get()");
      getter.setBody(body.toString());
      mbean.addMethod(getter);
    }

    // register this mbean with the MBeanServer
    String[] v = version.split(".");
    int major = 1, minor = 0, micro = 0;
    try { major = Integer.parseInt(v[0]); } catch(Throwable t) {}
    try { minor = Integer.parseInt(v[1]); } catch(Throwable t) {}
    try { micro = Integer.parseInt(v[2]); } catch(Throwable t) {}
    _appMBeanFactorfy.registerObject(key, "UnknownProtocol", mbean.toClass().newInstance(), mBeanName, major, minor); //FIXME micro..
        } catch(Throwable t) {
    _logger.warn("Failed registering generated MBean "+mBeanName+" for "+key, t);
        }
      }
    }
    */

    private static class JarBundle
    {
        Bundle _b;
        JarFile _jf;
        final String _location;
        final String _bn, _bsn, _bv, _id;

        public JarBundle(Object source) throws Exception
        {
            if (source instanceof Bundle)
            {
                _b = (Bundle) source;
                _location = _b.getLocation();
            }
            else if (source instanceof JarFile)
            {
              _jf = (JarFile)source;
              _location = _jf.getName();
            }
            else if (source instanceof JarURLConnection)
            {
              _jf = ((JarURLConnection)source).getJarFile();
              _location = ((JarURLConnection)source).getJarFileURL().toString();
            }
            else if (source instanceof URL)
                try
                {
                    URL jarUrl = new URL("jar:" + source.toString() + "!/");
                    JarURLConnection jarCnx = (JarURLConnection) jarUrl.openConnection();
                    _jf = jarCnx.getJarFile();
                    _location = jarUrl.toString();
                }
                catch (Throwable e)
                {
                    throw (IOException) new IOException("Failed to open " + source).initCause(e);
                }
            else
                throw new IllegalArgumentException("Unsupported source "
                        + (source == null ? null : source.getClass().getName()));
            Dictionary headers = getHeaders();
            _bsn = (String) headers.get(BUNDLE_SYMBOLICNAME);
            _bn = (String) headers.get(BUNDLE_NAME);
            String ver = (String) headers.get(BUNDLE_VERSION);
            _bv = (ver != null ? ver : "0.0.0");
            _id = _bsn+"-"+_bv;
        }

        public String getBundleName() { return _bn; }
        public String getBundleSymbolicName() { return _bsn; }
        public String getBundleVersion() { return _bv; }
        public String getID() { return _id; }

        public Dictionary getHeaders()
        {
            if (_b != null)
                return _b.getHeaders();

            Properties headers = new Properties();
            try
            {
              Manifest mf = _jf.getManifest();
              for(Map.Entry e : mf.getMainAttributes().entrySet()) 
              {
                Object v = e.getValue();
                headers.put(e.getKey().toString(), 
                    (v instanceof Object[] 
                     ? Arrays.asList(v).toString()
                     : v.toString()));
              }
            }
            catch (Exception e)
            {
                _logger.warn("Error reading manifest from " + _jf.getName(), e);
            }
            return headers;
        }

        public List<MBDEntry> getMBeansDescriptorsEntries()
        {
            List<MBDEntry> res = new ArrayList<MBDEntry>();
            if (_b != null)
            {
                Enumeration<URL> e = _b.findEntries("META-INF", "mbeans-descriptors.xml", false);
                if (e != null) for (; e.hasMoreElements();)
                {
                    try
                    {
                        res.add(new MBDEntry(null, e.nextElement().openStream()));
                    }
                    catch (IOException ioe)
                    {
                        _logger.warn("Cannot open META-INF/mbeans-descriptors.xml from " + getLocation(), ioe);
                    }
                }
                e = _b.findEntries("META-INF", "*.mbd", true);
                if (e != null) for (; e.hasMoreElements();)
                {
                    URL url = e.nextElement();
                    String pid = getPidFromPath(url.toString());
                    try
                    {
                        res.add(new MBDEntry(pid, url.openStream()));
                    }
                    catch (IOException ioe)
                    {
                        _logger.warn("Cannot open " + url + " from " + getLocation(), ioe);
                    }
                }
            }
            else
            {
                ZipEntry ze = _jf.getEntry("META-INF/mbeans-descriptors.xml");
                if (ze != null)
                    try
                    {
                        res.add(new MBDEntry(null, _jf.getInputStream(ze)));
                    }
                    catch (IOException ioe)
                    {
                        _logger.warn("Cannot open META-INF/mbeans-descriptors.xml from " + getLocation(), ioe);
                    }
                for (Enumeration<JarEntry> e = _jf.entries(); e.hasMoreElements();)
                {
                    JarEntry je = e.nextElement();
                    if (je.getName().endsWith(".mbd"))
                        try
                        {
                            res.add(new MBDEntry(getPidFromPath(je.getName()), _jf.getInputStream(je)));
                        }
                        catch (IOException ioe)
                        {
                            _logger.warn("Cannot open " + je.getName() + " from " + getLocation(), ioe);
                        }
                }
            }
            return res;
        }

        private String getPidFromPath(String path)
        {
            String pid = path.substring(path.lastIndexOf('/') + 1);
            return pid.substring(0, pid.lastIndexOf('.'));
        }

        public InputStream getInputStream(String path) throws IOException
        {
            if (_b != null)
            {
                URL u = _b.getEntry(path);
                if (u == null)
                {
                    u = _b.getEntry("META-INF/" + path); // try default location
                }
                if (u == null)
                {
                    if (_logger.isDebugEnabled())
                        _logger.debug(path + " not found in bunde " + _b);
                    return null;
                }
                else
                {
                    return u.openStream();
                }
            }
            else
            {
                ZipEntry ze = _jf.getEntry(path);
                if (ze == null)
                {
                    ze = _jf.getEntry("META-INF/" + path); // try default location
                }
                if (ze == null)
                {
                    if (_logger.isDebugEnabled())
                        _logger.debug(path + " not found in jar " + _jf.getName());
                    return null;
                }
                else
                    return _jf.getInputStream(ze);
            }
        }

        public String toString()
        {
            return getLocation();
        }

        public String getLocation()
        {
            return _location;
        }

        public String getFileName()
        {
            String loc = getLocation();
            String fname = loc.substring(loc.lastIndexOf('/') + 1);
            if (fname.startsWith("jar_cache")) _logger.warn("suspecting wrong filename "+loc+" from "+(_b != null ? _b.getClass().getName() : _jf.getClass().getName()), new Throwable());//FIXME remove when fixed!
            return fname;
        }
    }

    private static class MBDEntry
    {
        public String pid;
        public InputStream in;

        public MBDEntry(String pid, InputStream in)
        {
            this.pid = pid;
            this.in = in;
        }
    }
}
