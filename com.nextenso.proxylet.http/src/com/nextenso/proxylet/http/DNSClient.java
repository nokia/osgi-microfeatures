// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

/**
 * <b>This Class is deprecated. All DNS Classes are in package com.nextenso.proxylet.dns</b>
 * <p/>This is a utility Class used to perform DNS requests.
 * <p/>An instance can be retrieved via <code>DNSClientFactory.newDNSClient()</code>.
 */

public interface DNSClient {

    /**
     * Returns the list of IP addresses associated to the given host name.
     * @param name the host name.
     * @return an array with the matching IP addresses or an empty array if the DNS request failed for any reason (no match, timeout...).
     * @deprecated
     */
    public String[] getHostByName (String name);

    /**
     * Returns the list of host names associated to the given IP address.
     * @param addr the IP address
     * @return an array with the matching host names or an empty array if the DNS request failed for any reason (no match, timeout...).
     * @deprecated
     */
    public String[] getHostByAddr (String addr);

}
