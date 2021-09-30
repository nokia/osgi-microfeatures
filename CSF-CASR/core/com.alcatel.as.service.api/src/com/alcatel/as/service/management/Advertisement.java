package com.alcatel.as.service.management;

/**
 * A network advertisement which let components discover from one another. Advertisement
 * contains component addresses (IP/port), as well as component description (component name,
* component id, instance name, etc ...).
 * 
 * @internal
 * @deprecated
 */
public class Advertisement
{
    private String _host;
    private String _appName;
    private int _appId;
    private String _instanceName;
    private boolean _active;
    private Address _addr;

    /**
     * Makes a new Advertisement
     * @param addr the address to advertise
     * @param host the host name of the advertised address
     * @param appName the advertised application name (i.e: "CalloutAgent")
     * @param appId the advertised application id
     * @param instanceName the advertised application instance name
     * @param active true if the advertised application is active, false if not
     */
    public Advertisement(Address addr,
                         String host,
                         String appName,
                         int appId,
                         String instanceName,
                         boolean active)
    {
        _addr = addr;
        _host = host;
        _appName = appName;
        _appId = appId;
        _instanceName = instanceName;
        _active = active;
    }

    /**
     * Gets the address of the advertised application
     * @return the address of the advertised application
     */
    public Address getAddress()
    {
        return _addr;
    }

    /**
     * Returns the host name of the advertised application
     * @return the host name of the advertised application
     */
    public String getHost()
    {
        return _host;
    }

    /**
     * Returns the name of the advertised application
     * @return the name of the advertised application
     */
    public String getAppName()
    {
        return _appName;
    }

    /**
     * Returns the id of the advertised application
     * @return the id of the advertised application
     */
    public int getAppId()
    {
        return _appId;
    }

    /**
     * Returns the instance name of the advertised application
     * @return the instance name of the advertised application
     */
    public String getInstanceName()
    {
        return _instanceName;
    }

    /**
     * Returns true if the advertised application is active, false if not
     * @return true if the advertised application is active, false if not
     */
    public boolean isActive()
    {
        return _active;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("addr=").append(_addr);
        sb.append(",host=").append(_host);
        sb.append(",appName=").append(_appName);
        sb.append(",appId=").append(_appId);
        sb.append(",instanceName=").append(_instanceName);
        sb.append(", active=").append(_active);
        return sb.toString();
    }
}
