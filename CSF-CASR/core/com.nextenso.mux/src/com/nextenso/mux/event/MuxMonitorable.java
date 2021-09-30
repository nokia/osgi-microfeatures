package com.nextenso.mux.event;

public interface MuxMonitorable
{

    public int[] getCounters();

    public int getMajorVersion();

    public int getMinorVersion();

    public void commandEvent(int command, int[] intParams, String[] strParams);

    public void muxGlobalEvent(int identifierI, String identifierS, byte[] data, int off, int len);

    public void muxLocalEvent(int identifierI, String identifierS, Object data);
}
