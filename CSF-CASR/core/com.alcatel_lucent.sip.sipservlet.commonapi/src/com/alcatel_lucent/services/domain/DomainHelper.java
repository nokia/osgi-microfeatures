/**
 * 
 */
package com.alcatel_lucent.services.domain;

import java.util.List;

import javax.servlet.sip.URI;

/**
 * Domain Helper : determines if an URI or a string is part of the supported domain of the platform
 * The domain comparison is working for
 * A) sip: host.toto.com
 * B) sip: IP:port;transport=
 * C) sip:host.toto.com;phone=+333
 * D)tel:+34444
 *  
 *  The domain is expressing has a list of names or ranges : toto.com, IP1/IP2, +333/+4444
 *
 */
public interface DomainHelper {
    boolean isLocalDomain(URI uri);
    boolean isLocalDomain(String address);
    List<String> localDomains();
    
}
