// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine;


// JDK
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.Proxylet;

/**
 * A marker class - no useful method in it.
 */

public abstract class ProxyletEngine  {
  
  // contains the license ids
  // key is the licenseId - Value is the proxylet class
  private Map<Integer, String> _licenseIds = new HashMap<Integer, String>();
  
  // contains the counters for the different componants
  // Key is the componant key - Value is a Long representing the counter value
  private Map<String, Long> _componentCounters = new HashMap<String, Long>();
  
  // the Tracer to use
  private final static Logger LOGGER = Logger.getLogger("callout.pxlet");
  
  /**
   * Check license validity for all deployed proxylets.
   */
  public abstract void checkLicense();
    
  /**
   * Check license validity for the given context.
   */
  protected void checkLicenseForContext(Context ctx) {
	  return;
  }
  
  protected String getProxyletForLicenseId(Integer id) {
    return _licenseIds.get(id);
  }
  
  /**
   * This method is invoked by the engine once per proxylet processing.
   */
  protected void processedProxylet(Proxylet p) {
  }
  
  
  /**
   * @see com.nextenso.licensemgr.interfaces.ThroughputListener#getThroughputCounter(java.lang.String)
   */
  public long getThroughputCounter(String key) {
      return 0;
  }
  
}
