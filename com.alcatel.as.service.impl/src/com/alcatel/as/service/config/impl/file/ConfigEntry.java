// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.config.impl.file;

import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;
import static com.alcatel.as.util.config.ConfigConstants.SYSTEM_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;

import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;

public class ConfigEntry
{    
    public final static String CONFIG_EXT = ".cfg";
    public final static String FILEDATA_PREFIX = "file-";
    static volatile boolean _propagateSystemConfig = false;
    private final Logger _logger;
    private final String _pid;
    private final String _factoryPid;
    private volatile Dictionary<String, Object> _dic = new Hashtable<>();
    private volatile ServiceRegistration<?> _registration;
    private volatile long _lastModified = -1;
    private volatile File _file;
    private volatile String _confdir;
    private volatile Configuration _cmConfig;
    private volatile ConfigurationAdmin _cm;
    private volatile ConfigurationPlugin _cmPlugin;
    private volatile BundleContext _sr;
    private volatile boolean _enabled = true;
    private volatile Map<File, Long> _filesData;
    
    public static void propagateSystemProperties(boolean propagate) {
      _propagateSystemConfig = propagate;
    }

    ConfigEntry(BundleContext sr, String pid, String confdir, ConfigurationAdmin cm, ConfigurationPlugin cmPlugin)
    {
        _sr = sr;
        _confdir = confdir;
        _cm = cm;
        _lastModified = -1;
        _file = new File(confdir + File.separator + pid + CONFIG_EXT);
        _logger = Logger.getLogger(ConfigEntry.class.getName());
        _logger.info("init");
        _cmPlugin = cmPlugin;   
        
        // Parse the pid:
        // Syntax of or pid file names '.cfg) is "<pid> ( '-' <subname> )? .cfg"
        // subname, if present, means we'll create a factory configuration. if no subname, it means
        // a simple configuration is created.
        
        int dash = pid.indexOf("-");
        if (dash == -1) {
        	_pid = pid;  
        	_factoryPid = null;
    	} else {
			_pid = pid.substring(0, dash);
    		if (dash >= pid.length()-1) {
    			// there is a dash at the end of the pid, without any subname. consider it's not a factory pid    		
    			_factoryPid = null;
    		} else {
    			_factoryPid = pid.substring(dash+1);
    		}
    	}  
    }

    Dictionary<String, Object> getProperties()
    {
        return _dic;
    }

    String getPid()
    {
        return _pid;
    }

    void bindCM(ConfigurationAdmin cm)
    {
        _cm = cm;
        if (_registration != null)
        {
            updateCM(_pid, _dic);
        }
    }
    
    public synchronized boolean fileExists() {
    	if (! _file.exists()) {
    		return false;
    	}
    	
    	if (_filesData != null) {
    		for (File fileData : _filesData.keySet()) {
    			if (! fileData.exists()) {
    				return false;
    			}
    		}
    	}
    	
    	return true;
    }

    public synchronized boolean needsUpdate()
    {
        if (_file.lastModified() > _lastModified)
        {
            return true;
        }
        
    	if (_filesData != null) {
    		List<File> list = new ArrayList<>(_filesData.size());
    		for (Map.Entry<File, Long> entry : _filesData.entrySet()) {
    			File fileData = entry.getKey();
    			Long lastModified = entry.getValue();
    			
    			if (fileData.lastModified() > lastModified) {
    				return true;
    			}
    		}
    	}

        return false;
    }

    public void update()
    {
		_lastModified = _file.lastModified(); // 0 if the file does not exists

		if (_filesData != null) {
			List<File> list = new ArrayList<>(_filesData.size());
			for (File file : _filesData.keySet()) {
				list.add(file);
			}
			list.forEach(file -> _filesData.put(file, file.lastModified()));
		}

        // If a property file exists, overload our MBD default values with it.
        try
        {
            if (_file.exists())
            {
                loadConfig(_file, _confdir, _pid, _dic);
            }
        }
        catch (IOException e)
        {
            _logger.error("Got IO exception while reading property file " + _file, e);
        }

        if (! _enabled) {
            // unregister configuration if it has previously been registered, and not enable the configuration.
            _logger.info("configuration not enabled by operation for pid " + _pid);
            close();
            return;
        }
        
        if (_factoryPid != null) {
        	if (_cmPlugin != null) {
        		_cmPlugin.modifyConfiguration(null, _dic);   
        	}
            updateFactoryCM(_pid, _dic);

            if (_logger.isInfoEnabled())
            	if (_cmConfig == null) {
                    _logger.info("Creating factory configuration for pid " + _pid + ", conf=" + _dic);            		
            	} else {
                    _logger.info("Updating factory configuration for pid " + _pid + ", conf=" + _dic);            		            		
            	}
        	return;
        }
        
        // Now, register/update dictionary to the OSGi registry.
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(SERVICE_PID, _pid);
        ServiceRegistration<?>previousReg = _registration;
        
        // For "system" configurations, we also set them to the jvm system properties
        if (_propagateSystemConfig && "system".equals(_pid)) {
          for (String key : Collections.list(_dic.keys())) {
            System.setProperty(key, _dic.get(key).toString());
          }
        }
        
        if (_cmPlugin != null)
        	_cmPlugin.modifyConfiguration(null, _dic);   
        _registration = _sr.registerService(java.util.Dictionary.class.getName(), _dic, props);
        unregister(previousReg);

        updateCM(_pid, _dic);

        if (_logger.isInfoEnabled())
            _logger.info("Configuration " + (previousReg != null ? "updated" : "registered")
                    + " for service.pid \"" + _pid + "\": \n" + _dic);
    }

    private void unregister(ServiceRegistration<?> previousReg) {
    	if (previousReg != null) {
    		try {
    			previousReg.unregister();
    		} catch (IllegalStateException ignored) {}
    	}
	}

	private void updateCM(String PID, Dictionary<String, Object> props)
    {
        try
        {
            if (_cm != null)
            {
            	_cmConfig = _cm.getConfiguration(PID, "?");
            	_cmConfig.update(props);
            }
        }
        catch (Throwable t)
        {
            _logger.error("Could not update OSGi Config Admin Service for service.pid \"" + PID + "\"", t);
        }
    }

	private void updateFactoryCM(String PID, Dictionary<String, Object> props) {
		try {
			if (_cm != null) {
				if (_cmConfig == null) {
					_cmConfig = _cm.createFactoryConfiguration(_pid, "?");
					_cmConfig.update(props);
				} else {
					_cmConfig.update(props);
				}
			}
		} catch (Throwable t) {
			_logger.error("Could not update OSGi Config Admin Service for service.pid \"" + PID + "\"", t);
		}
	}

    private void removeCM()
    {
        if (_cmConfig != null)
        {
            try
            {
                _cmConfig.delete();
            }
            catch (IOException e)
            {
                _logger.warn("Could not unregister config " + _pid + " from CM", e);
            }
            finally
            {
                _cmConfig = null;
            }
        }
    }

    public void close()
    {
        if (_registration != null)
        {
            try {
                if (_registration != null) {
                	_registration.unregister();
                }
            } catch (Throwable t) {}
            _registration = null;
        }
        removeCM();
    }

    private void setProperty(String pid, Dictionary<String, Object> dic, String name, String value)
    {
    	Object objValue;
    	
		if (name.startsWith("java.lang.Boolean-") || name.startsWith("java.lang.Byte-") || name.startsWith("java.lang.Character-") ||
			name.startsWith("java.lang.Short-") || name.startsWith("java.lang.Integer-") || name.startsWith("java.lang.Long-") ||
			name.startsWith("java.lang.Float-") || name.startsWith("java.lang.Double-") || name.startsWith("java.lang.String-")) 
		{		
			int index = name.indexOf("-");
			String type = name.substring(0, index);
			name = name.substring(index+1);
			objValue = getPropertyValue(name, value, type);	
		} 
		else {
			objValue = value;
		}
		dic.put(name, objValue);
		if (_logger.isDebugEnabled())
		{
			_logger.debug("setProperty for service pid \"" + pid + "\": " + name + "=" + value);
		}
		if (name.endsWith(ConfigConstants.COMPONENT_ENABLED)) {
			_enabled = ConfigHelper.getBoolean(dic, name, true);
		}
    }
    
	private Object getPropertyValue(String name, String value, String type) {
		if (type == null) {
			return value;
		}

		try {
			if (type.equals("java.lang.Char"))
				type = "java.lang.Character";

			Class<?> c = Class.forName(type);
			if (c == String.class)
				return value;

			value = value.trim();
			if (c == Character.class)
				c = Integer.class;
			Method m = c.getMethod("valueOf", String.class);
			return m.invoke(null, value);
		} catch (Exception e) {
			_logger.error("Invalid property type " + type + " for property " + name + " for pid " + _pid);
		}
		return value;
	}

    boolean hasProperty(String name)
    {
        return _dic.get(name) != null;
    }

    public void loadConfig(File f, String dir, String pid, Dictionary<String, Object> dic) throws IOException
    {
        Properties config = new Properties();
        FileInputStream in = new FileInputStream(f);
        try
        {
            config.load(in);
        }
        finally
        {
            in.close();
        }

        for (Enumeration<?> e = config.propertyNames(); e.hasMoreElements();)
        {
            String name = (String) e.nextElement();
            String value = config.getProperty(name);

            if (name.startsWith(FILEDATA_PREFIX)) 
            {
                if (value.endsWith(CONFIG_EXT))
                {
                    _logger.warn("Config Error", 
                            new Exception("File name value for FileData properties, must NOT use the "+CONFIG_EXT+" extension. This extension is reserved for internal use."));
                }
                value = loadFileData(config.getProperty(name), dir);
                name = name.substring(5);
            }
            if (value != null)
            {
                value = parseEnvironmentVariables(value);
                setProperty(pid, dic, name, value);
            }
        }
        
        // override dynamic attributes of system.cfg
        if (SYSTEM_PID.equals(pid))
        {
          String procId = (String) dic.get(INSTANCE_PID);
          if (procId == null || procId.trim().equals("${instance.pid}"))
          {
        	  procId = getProcessId();
              setProperty(pid, dic, INSTANCE_PID, procId);   
              _logger.info("System properties overridden with "+INSTANCE_PID+"="+procId);
          }
          String instanceId = (String) dic.get(INSTANCE_ID);
          if (instanceId == null) 
          {        	
            String instId = ""+hashString((String) dic.get(PLATFORM_NAME) 
                    + dic.get(GROUP_NAME) 
                    + dic.get(COMPONENT_NAME) 
                    + dic.get(INSTANCE_NAME) 
                    + procId 
                    + System.nanoTime());
            setProperty(pid, dic, INSTANCE_ID, instId);
            _logger.info("System properties overridden with "+INSTANCE_ID+"="+instId);
          }
        }
        
        setProperty(pid, dic, "service.pid", pid);
    }

    private String parseEnvironmentVariables(String value) {
	final String regex = "\\$(\\{([^}]+)\\})(\\{([^}]+?)\\})?";
        Matcher m = Pattern.compile(regex).matcher(value);

        StringBuffer buf = new StringBuffer();
        while(m.find()) {
            String val = System.getProperty(m.group(2));
	    if (val == null) val = System.getenv(m.group(2));
            if(val != null) {
                m.appendReplacement(buf, Matcher.quoteReplacement(val));
            } else if(m.group(3) != null) {
                m.appendReplacement(buf, Matcher.quoteReplacement(m.group(4)));
            } else {
                m.appendReplacement(buf, Matcher.quoteReplacement(m.group()));
            }
        }
        m.appendTail(buf);
	return buf.toString();
    }
    
    private String getProcessId() {
        try {
          java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();
          java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
          jvm.setAccessible(true);
          sun.management.VMManagement mgmt = (sun.management.VMManagement) jvm.get(runtime);
          java.lang.reflect.Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
          pid_method.setAccessible(true);
          Integer pid = (Integer) pid_method.invoke(mgmt);
          return pid.toString();
        } catch (Throwable t) {
         _logger.error("Could not get process id", t);
          return null;
        }
    }
    
    private synchronized String loadFileData(String fname, String confdir)
    {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(confdir + File.separator + fname)));
			try {
				StringBuilder sb = new StringBuilder();
				String tmp;
				while ((tmp = reader.readLine()) != null) {
					sb.append(tmp);
					sb.append("\n");
				}

				// Register this file data before returning the content
				File fileData = new File(confdir + File.separator + fname);
				if (_filesData == null) {
					_filesData = new HashMap<>();				 
				}
				_filesData.put(fileData, fileData.lastModified());

				return sb.toString();
			} finally {
				reader.close();
			}
			
		} catch (FileNotFoundException fnf) {
			_logger.info("File " + fname + " not provided in " + confdir + ". Assuming empty property.");
			return "";
		} catch (Exception e) {
			_logger.warn("Error loading " + fname, e);
		}
		return null;
    }

    private String hashPropValue(Dictionary<String, String> props, String propName)
    {
        String val = (String) props.get(propName);
        if (val == null)
        {
            throw new IllegalArgumentException("Missing mandatory property \"" + propName
                    + "\" from system properties");
        }
        return Long.valueOf(hashString(val)).toString();
    }

    private String getUID()
    {
        return Long.valueOf(hashString(new VMID().toString())).toString();
    }

    private long hashString(String val)
    {
        CRC32 crc = new CRC32();
        try
        {
            crc.update(val.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        { /* Ignored */
        }
        return crc.getValue();
    }
}
