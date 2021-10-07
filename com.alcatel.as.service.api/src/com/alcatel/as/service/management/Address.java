package com.alcatel.as.service.management;

import java.util.Arrays;

/**
 * This class represents an address end point.
 * 
 * @internal
 * @deprecated
 */
public class Address
{
    private final String[] _addrs;
    private final int[] _ports;

    public Address(String[] addrs, int[] ports)
    {
        _addrs = addrs;
        _ports = ports;
    }

    public int getAddressesCount()
    {
        return _addrs.length;
    }

    public String[] getAddresses()
    {
        return _addrs;
    }

    public int[] getPorts()
    {
        return _ports;
    }

    public String getAddress(int index)
    {
        return _addrs[index];
    }

    public int getPort(int index)
    {
        return _ports[index];
    }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
	public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(Arrays.toString(_addrs));
    sb.append(Arrays.toString(_ports));
    return sb.toString();
  }


    public void setAddress(int i, String addr)
    {
        _addrs[i] = addr;
    }

    public void setPort(int i, int port)
    {
        _ports[i] = port;
    }
}
