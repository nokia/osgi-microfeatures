// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.discovery;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Advertisement for notifying about the availabity/unavailability of
 * a remote peer.
 */
public class Advertisement {
  /**
   * OSGi Event topic used to notify the discovery service about a stale advert.
   */
  public final static String STALE_ADVERT = "com/alcatel/as/service/discovery/STALE";

  private final String ip;
  private final int port;
  private final String key;
  
  public Advertisement(String ip, int port) {
    this.ip = ip;
    this.port = port;
    this.key = ip + ":" + port; //stored for performance
  }
  
  public Advertisement(String ip, String port) {
    this(ip, Integer.parseInt(port));
  }
  
  public String toString() {
    return key;
  }
  
  public String getIp() {
    return ip;
  }
  
  public int getPort() {
    return port;
  }
  
  // Override hashCode & equals for use as map key
  
  @Override
  public int hashCode() {
    return key.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    return o != null ? key.equals(o.toString()) : false;
  }
}
