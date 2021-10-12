// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.lang.reflect.Constructor;

/**
 * <b>This Class is deprecated. All DNS Classes are in package com.nextenso.proxylet.dns</b>
 * <p/>This class is used to create a new instance of DNSClient.
 * <p/>The name of the class used for the DNSClient is taken from the
 * System property <i>com.nextenso.proxylet.http.DNSClient.class</i>
 */
public class DNSClientFactory {
    
    /**
     * The System property name indicating the DNSClient implementation.
     */
    public static final String DNS_CLIENT_CLASS = com.nextenso.proxylet.dns.DNSClientFactory.DNS_CLIENT_CLASS;

    private static Constructor constructor;
    private static Object[] args = new Object[0];

    /**
     * Constructs a new instance of DNSClientFactory.
     * <br/>Instanciating the class is not needed to use it.
     * @deprecated
     */
    public DNSClientFactory (){}

    /**
     * Returns a new instance of the current DNSClient implementation.
     * @return a new instance of DNSClient or <code>null</code> if an instanciation problem occurred.
     * @deprecated
     */
    public static DNSClient newDNSClient(){
	// we load the constructor only once - when the method is called the first time
	if (constructor == null){
	    String className = System.getProperty (DNS_CLIENT_CLASS);
	    if (className == null)
		return null;
	    try{
		Class dnsClientClass = Class.forName (className);
		constructor = dnsClientClass.getConstructor (new Class[0]);
	    }catch(Exception e){
		constructor = null;
		return null;
	    }
	}
	try{
	    Object client = constructor.newInstance (args);
	    if (client instanceof DNSClient)
		return (DNSClient) client;
	    return new DNSClientWrapper ((com.nextenso.proxylet.dns.DNSClient)client);
	}catch(Throwable t){
	    return null;
	}
    }

    /**
     * Inner class that provides compatibility between com.nextenso.proxylet.dns.DNSClient and com.nextenso.proxylet.http.DNSClient
     */
    private static class DNSClientWrapper implements DNSClient {
	private com.nextenso.proxylet.dns.DNSClient client;
	private DNSClientWrapper (com.nextenso.proxylet.dns.DNSClient client){
	    this.client = client;
	}
	public String[] getHostByName (String name){
	    return client.getHostByName (name);
	}
	public String[] getHostByAddr (String addr){
	    return client.getHostByAddr (addr);
	}
    }
}

