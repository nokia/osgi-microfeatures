package com.nextenso.mux;

/**
 * This is the Version-2 of the MuxHeader.
 * <p/>
 * The fields are in the order:<br/>
 * <ul>
 * <li>the flags (1 byte) - inherited from Version-0
 * <li>the length (2 bytes) - inherited from Version-0
 * <li>the sessionId (8 bytes) - inherited from Version-0
 * <li>the channelId (4 bytes) - inherited from Version-0
 * <li>the remoteIP (4 bytes)
 * <li>the remotePort (2 bytes)
 * <li>the virtualIP (4 bytes)
 * <li>the virtualPort (2 bytes)
 * </ul>
 */
public class MuxHeaderV2 extends MuxHeaderV0
{

    private int _remoteIP, _remotePort, _virtualIP, _virtualPort;

    /**
     * Constructor for this class.
     */
    public MuxHeaderV2()
    {
    }

    public void set(long sessionId, int channelId, int flags, int remoteIP, int remotePort, int virtualIP,
                    int virtualPort)
    {
        super.set(sessionId, channelId, flags);
        _remoteIP = remoteIP;
        _remotePort = remotePort;
        _virtualIP = virtualIP;
        _virtualPort = virtualPort;
    }

    /**
     * @see com.nextenso.mux.MuxHeaderV0#getVersion()
     */
    @Override
    public int getVersion()
    {
        return 2;
    }

    /**
     * Gets the remote IP.
     * 
     * @return The remote IP.
     */
    public int getRemoteIP()
    {
        return _remoteIP;
    }

    /**
     * Gets the remote port.
     * 
     * @return The remote port.
     */
    public int getRemotePort()
    {
        return _remotePort;
    }

    /**
     * Gets the virtual IP.
     * 
     * @return The virtual IP.
     */
    public int getVirtualIP()
    {
        return _virtualIP;
    }

    /**
     * Gets the virtual port.
     * 
     * @return The virtual port.
     */
    public int getVirtualPort()
    {
        return _virtualPort;
    }

    /**
     * @see com.nextenso.mux.MuxHeaderV0#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(",remoteIP=").append(getRemoteIP());
        sb.append(",remotePort=").append(getRemotePort());
        sb.append(",virtualIP=").append(getVirtualIP());
        sb.append(",virtualPort=").append(getVirtualPort());
        return sb.toString();
    }
}
