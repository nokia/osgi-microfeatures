// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.ProxyletConstants;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;

public class HostIPCriterion extends RegexpCriterion {
  
  public static final int UNKNOWN = 3;
  
  private HostIPCriterion(String regexp) throws CriterionException {
    super(regexp);
    for (int i = 0; i < regexp.length(); i++) {
      char c = regexp.charAt(i);
      switch (c) {
      case '*':
        break;
      case '.':
        break;
      default:
        if (c > '9' || c < '0')
          throw new CriterionException(CriterionException.INVALID_IP, regexp);
      }
    }
  }
  
  public int match(ProxyletData data) {
    if (data instanceof HttpRequest)
      return matchRequest((HttpRequest) data);
    return matchResponse((HttpResponse) data);
  }
  
  public int matchRequest(HttpRequest req) {
    String host = req.getProlog().getURL().getHost();
    char c = host.charAt(0);
    if (c >= '0' && c <= '9')
      // we have a host addr
      return (match(host)) ? TRUE : FALSE;
    // we check if the DNS was resolved
    Object o = req.getAttribute(ProxyletConstants.ATTR_NAME_DNS_RESULT + host);
    if (o == null)
      // we need a DNS
      return UNKNOWN;
    String[] addrs = (String[]) o;
    for (int i = 0; i < addrs.length; i++)
      if (match(addrs[i]))
        return TRUE;
    return FALSE;
  }
  
  public int matchResponse(HttpResponse resp) {
    String host = resp.getProlog().getURL().getHost();
    char c = host.charAt(0);
    if (c >= '0' && c <= '9')
      // we have a host addr
      return (match(host)) ? TRUE : FALSE;
    // we check if the DNS was resolved
    Object o = resp.getRequest().getAttribute(ProxyletConstants.ATTR_NAME_DNS_RESULT + host);
    if (o == null)
      // we need a DNS
      return UNKNOWN;
    String[] addrs = (String[]) o;
    for (int i = 0; i < addrs.length; i++)
      if (match(addrs[i]))
        return TRUE;
    return FALSE;
  }
  
  public String toString() {
    return ("[" + Utils.HOST_IP + "=" + getRegexp() + "]");
  }
  
  public static Criterion getInstance(String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? c : new HostIPCriterion(regexp);
  }
  
  public boolean equals(Object o) {
    if (o instanceof HostIPCriterion)
      return super.equals(o);
    return false;
  }
}
