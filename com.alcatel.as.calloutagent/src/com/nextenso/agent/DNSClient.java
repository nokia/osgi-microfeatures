// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import com.nextenso.mux.util.DNSManager;

public class DNSClient implements com.nextenso.proxylet.dns.DNSClient {
  
  public String[] getHostByAddr(String addr) {
    return DNSManager.getByAddr(addr, null);
  }
  
  public String[] getHostByName(String name) {
    return DNSManager.getByName(name, null);
  }
}
