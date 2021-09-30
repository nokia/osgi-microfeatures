package com.nextenso.mux.util;

import java.util.Dictionary;

import com.alcatel.as.util.config.ConfigHelper;
import static com.alcatel.as.util.config.ConfigConstants.*;

public class MuxIdentification
{
    private String _appName;
    private String _instanceName;
    private long _keepAlive = -1;
    private long _idleFactor = -1;
    private long _agentID = -1;
    private long _groupID = -1;
    private long _ringID = -1;
    private int _containerIndex = -1;
    private String _hostName;

    public MuxIdentification()
    {
    }

    @SuppressWarnings("unchecked")
    public MuxIdentification load(Dictionary system)
    {
        _appName = ConfigHelper.getString(system, COMPONENT_NAME, null);
        String grpName = ConfigHelper.getString(system, GROUP_NAME, null);
        String instName = ConfigHelper.getString(system, INSTANCE_NAME, null);
        _instanceName = (grpName != null && instName != null) ? (grpName + "__" + instName) : null;
        _agentID = ConfigHelper.getLong(system, INSTANCE_ID, -1);
        _groupID = ConfigHelper.getLong(system, GROUP_ID, -1);
	_hostName = ConfigHelper.getString(system, HOST_NAME, null);
        return this;
    }

    public MuxIdentification setDefaultKeepAlive()
    {
        String keepAliveInterval = System.getProperty("platform.agent.iohKeepAliveInterval", "3");
        try
        {
            _keepAlive = Long.parseLong(keepAliveInterval);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid value for parameter platform.agent.iohKeepAliveInterval");
        }

        String keepAliveIdleFactor = System.getProperty("platform.agent.iohIdleFactor", "3");
        try
        {
            _idleFactor = Long.parseLong(keepAliveIdleFactor);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid value for parameter platform.agent.iohIdleFactor");
        }
        return this;
    }

    /**
     * @return the hostName
     */
    public String getHostName()
    {
        return _hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public MuxIdentification setHostName(String hostName)
    {
        _hostName = hostName;
        return this;
    }

    /**
     * @return the instanceName
     */
    public String getInstanceName()
    {
        return _instanceName;
    }

    /**
     * @param instanceName the instanceName to set
     */
    public MuxIdentification setInstanceName(String instanceName)
    {
        _instanceName = instanceName;
        return this;
    }

    /**
     * @return the appName
     */
    public String getAppName()
    {
        return _appName;
    }

    /**
     * @param appName the appName to set
     */
    public MuxIdentification setAppName(String appName)
    {
        _appName = appName;
        return this;
    }

    /**
     * @return the keepAlive
     */
    public long getKeepAlive()
    {
        return _keepAlive;
    }

    /**
     * @param keepAlive the keepAlive to set
     */
    public MuxIdentification setKeepAlive(long keepAlive)
    {
        _keepAlive = keepAlive;
        return this;
    }

    /**
     * 
     * @param keepAlive
     * @return The Mux identification.
     */
    public MuxIdentification setkeepAlive(String keepAlive)
    {
        try
        {
            _keepAlive = Long.parseLong(keepAlive);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid value for keep alive: " + keepAlive);
        }
        return this;
    }

    /**
     * @return the idleFactor
     */
    public long getIdleFactor()
    {
        return _idleFactor;
    }

    /**
     * @param idleFactor the idleFactor to set
     */
    public MuxIdentification setIdleFactor(long idleFactor)
    {
        _idleFactor = idleFactor;
        return this;
    }

    /**
     * 
     * @param idleFactor
     * @return The Mux identification.
     */
    public MuxIdentification setIdleFactor(String idleFactor)
    {
        try
        {
            _idleFactor = Long.parseLong(idleFactor);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid value for idle factor: " + idleFactor);
        }
        return this;
    }

    /**
     * @return the agent identifier.
     */
    public long getAgentID()
    {
        return _agentID;
    }

    /**
     * @param agentID the agentID to set
     * @return The Mux identification.
     */
    public MuxIdentification setAgentID(long agentID)
    {
        _agentID = agentID;
        return this;
    }

    /**
     * 
     * @param agentID
     * @return The Mux identification.
     */
    public MuxIdentification setAgentID(String agentID)
    {
        try
        {
            _agentID = Long.parseLong(agentID);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid value for group id: " + agentID);
        }
        return this;
    }

    /**
     * @return the groupID
     */
    public long getGroupID()
    {
        return _groupID;
    }

    /**
     * @param groupID the groupID to set
     */
    public MuxIdentification setGroupID(long groupID)
    {
        _groupID = groupID;
        return this;
    }

    /**
     * 
     * @param groupID
     * @return The Mux identification.
     */
    public MuxIdentification setGroupID(String groupID)
    {
        try
        {
            _groupID = Long.parseLong(groupID);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid value for group id: " + groupID);
        }
        return this;
    }

    /**
     * @return the ringID
     */
    public long getRingID()
    {
        return _ringID;
    }

    /**
    * @param ringID the ringID to set
    */
    public MuxIdentification setRingID(long ringID)
    {
        _ringID = ringID;
        return this;
    }

    /**
     * @return the container id
     */
    public int getContainerIndex()
    {
        return _containerIndex;
    }

    /**
    * @param containerID the container id to set
    */
    public MuxIdentification setContainerIndex(int containerIndex)
    {
        _containerIndex = containerIndex;
        return this;
    }

    /**
     * @return a String representation of this {@link MuxIdentification} instance.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
	sb.append ("MuxIdentification[");
	boolean addComma = false;
        if (_appName != null){
	    addComma = true;
	    sb.append("application.name=").append(_appName); // note that we dont use COMPONENT_NAME...
	}
        if (_instanceName != null){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append(INSTANCE_NAME).append ('=').append(_instanceName);
        }
	if (_hostName != null){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append(HOST_NAME).append ('=').append(_hostName);
        }
        if (_keepAlive != -1){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append("keepAliveInterval=").append(_keepAlive);
        }
        if (_idleFactor != -1){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append("idleFactor=").append(_idleFactor);
        }
        if (_groupID != -1){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append(GROUP_ID).append ('=').append(_groupID);
        }
        if (_agentID != -1){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append("agent.id").append ('=').append(_agentID); // we dont show "instance.id"
        }
        if (_ringID != -1){
	    if (addComma) sb.append (", ");
	    addComma = true;
	    sb.append(", ring.id=").append(_ringID);
        }
	if (addComma) sb.append (", ");
        sb.append("container.index=").append(_containerIndex);
	sb.append (']');
        return sb.toString();
    }
}
