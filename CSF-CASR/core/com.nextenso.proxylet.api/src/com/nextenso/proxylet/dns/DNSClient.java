package com.nextenso.proxylet.dns;

/**
 * This is a utility Class used to perform DNS requests.
 * <p/>An instance can be retrieved via <code>DNSClientFactory.newDNSClient()</code>.
 */

public interface DNSClient {

    /**
     * Returns the list of IP addresses associated to the given host name.
     * @param name the host name.
     * @return an array with the matching IP addresses or an empty array if the DNS request failed for any reason (no match, timeout...).
     */
    public String[] getHostByName (String name);

    /**
     * Returns the list of host names associated to the given IP address.
     * @param addr the IP address
     * @return an array with the matching host names or an empty array if the DNS request failed for any reason (no match, timeout...).
     */
    public String[] getHostByAddr (String addr);

}
