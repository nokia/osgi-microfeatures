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
