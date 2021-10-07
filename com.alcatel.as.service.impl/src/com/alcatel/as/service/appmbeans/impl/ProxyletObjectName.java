package com.alcatel.as.service.appmbeans.impl;

import java.util.Hashtable;

import javax.management.ObjectName;

import org.apache.log4j.Logger;

public class ProxyletObjectName
{
    private final static String HOST = "host";
    private final static String APP_NAME = "appName";
    private final static String INSTANCE = "instance";
    private final static String PARENT = "parent";
    private final static String PID = "pid";
    private final static String VERSION = "version";

    private String host;
    private String className;
    private String appName;
    private String instance;
    private String parent;
    private int pid;
    private int version;
    private final static Logger _logger = Logger.getLogger("as.service.management.");

    @SuppressWarnings("unchecked")
    public ProxyletObjectName(String host, ObjectName oname) throws MissingKeyPropertyException
    {
        Hashtable properties = oname.getKeyPropertyList();

        this.host = host;
        this.className = oname.getDomain();
        this.appName = getProperty(properties, APP_NAME, "No application name provided in the key list");
        this.instance = getProperty(properties, INSTANCE, "No instance provided in the key list");
        this.parent = getProperty(properties, PARENT, "No parent provided in the key list");

        String sPid = null;
        try
        {
            sPid = getProperty(properties, PID, "No pid provided in the key list");
            this.pid = Integer.parseInt(sPid);
        }
        catch (NumberFormatException nfe)
        {
            _logger.warn("Invalid pid provided in proxy application MBean domain: " + sPid);
        }
        catch (MissingKeyPropertyException mkpe)
        {
            // TracerManager.getThreadTracer
            // ().logDebug("No pid provided in proxy application MBean domain. Using 0");
            this.pid = 0;
        }

        String sVersion = null;
        sVersion = getProperty(properties, VERSION, "No version provided in the key list");
        int index = sVersion.indexOf(".");
        if (index != -1)
        {
            // Only take major version
            sVersion = sVersion.substring(0, index);
        }
        try
        {
            this.version = Integer.parseInt(sVersion);
        }
        catch (NumberFormatException nfe)
        {
            _logger.warn("Invalid version provided in proxy application MBean domain: " + sVersion);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getProperty(Hashtable props, String key, String msg1)
        throws MissingKeyPropertyException
    {
        if (props == null)
            return null;

        String prop = (String) props.get(key);

        if (prop == null)
            throw new MissingKeyPropertyException(msg1);
        else
            return prop;
    }

    public String getHost()
    {
        return host;
    }

    public String getAppName()
    {
        return appName;
    }

    public String getInstance()
    {
        return instance;
    }

    public String getParent()
    {
        return parent;
    }

    public int getPid()
    {
        return pid;
    }

    public int getVersion()
    {
        return version;
    }

    public String getDomain()
    {
        return className;
    }

    @Override
    public String toString()
    {
        return getDomain() + ":" + getKeyProperties();
    }

    public String getKeyProperties()
    {
        StringBuffer buff = new StringBuffer();
        buff.append(HOST)
                .append('=')
                .append(host)
                .append(',')
                .append(APP_NAME)
                .append('=')
                .append(appName)
                .append(',')
                .append(INSTANCE)
                .append('=')
                .append(instance)
                .append(',')
                .append(PARENT)
                .append('=')
                .append(parent)
                .append(',')
                .append(PID)
                .append('=')
                .append(pid)
                .append(',')
                .append(VERSION)
                .append('=')
                .append(version);

        return buff.toString();
    }

    @SuppressWarnings("unchecked")
    public static boolean isPlatformObjectName(ObjectName oname)
    {
        Hashtable properties = oname.getKeyPropertyList();
        if (properties.get(APP_NAME) == null || properties.get(INSTANCE) == null
                || properties.get(PARENT) == null || properties.get(VERSION) == null)
        {
            return false;
        }

        return true;
    }

    public static class MissingKeyPropertyException extends Exception
    {

        private static final long serialVersionUID = 1L;

        public MissingKeyPropertyException()
        {
        }

        public MissingKeyPropertyException(String msg)
        {
            super(msg);
        }
    }

}
