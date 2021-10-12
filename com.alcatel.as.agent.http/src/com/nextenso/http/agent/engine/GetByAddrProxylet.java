// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.engine;

import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.dns.DNSClient;
import com.nextenso.proxylet.dns.DNSClientFactory;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;

public class GetByAddrProxylet implements StreamedHttpRequestProxylet, StreamedHttpResponseProxylet {
  
  private DNSClient dnsClient = DNSClientFactory.newDNSClient();
  
  public GetByAddrProxylet() {
  }
  
  public void init(ProxyletConfig cnf) {
  }
  
  public void destroy() {
  }
  
  public String getProxyletInfo() {
    return "GetByAddrProxylet/1.0";
  }
  
  public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
    return StreamedHttpRequestProxylet.ACCEPT_MAY_BLOCK & StreamedHttpRequestProxylet.IGNORE_BODY;
  }
  
  public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
    return StreamedHttpResponseProxylet.ACCEPT_MAY_BLOCK & StreamedHttpResponseProxylet.IGNORE_BODY;
  }
  
  public int doRequestHeaders(HttpRequestProlog prolog, HttpHeaders headers) throws ProxyletException {
    String host = prolog.getURL().getHost();
    String[] dns = dnsClient.getHostByAddr(host);
    HttpRequestFacade req = (HttpRequestFacade) prolog.getRequest();
    req.setAttribute(HttpProxyletUtils.ATTR_NAME_DNS_RESULT + host, dns);
    HttpProxyletChain.restoreProxyletStateNoDNS(req);
    return StreamedHttpRequestProxylet.SAME_PROXYLET;
  }
  
  public int doResponseHeaders(HttpResponseProlog prolog, HttpHeaders headers) throws ProxyletException {
    String host = prolog.getURL().getHost();
    String[] dns = dnsClient.getHostByAddr(host);
    HttpResponseFacade resp = (HttpResponseFacade) prolog.getResponse();
    resp.getRequest().setAttribute(HttpProxyletUtils.ATTR_NAME_DNS_RESULT + host, dns);
    HttpProxyletChain.restoreProxyletStateNoDNS(resp);
    return StreamedHttpResponseProxylet.SAME_PROXYLET;
  }
  
  public void doRequestBody(HttpBody body, boolean isLastChunk) {
    // never called
  }
  
  public void doResponseBody(HttpBody body, boolean isLastChunk) {
    // never called
  }
  
}
