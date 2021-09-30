package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.ProxyletConstants;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;

public class HostCriterion extends RegexpCriterion {
  
  public static final int UNKNOWN = 2;
  
  private HostCriterion(String regexp) throws CriterionException {
    super(regexp.toLowerCase());
  }
  
  public int match(ProxyletData data) {
    if (data instanceof HttpRequest)
      return matchRequest((HttpRequest) data);
    return matchResponse((HttpResponse) data);
  }
  
  public int matchRequest(HttpRequest req) {
    String host = req.getProlog().getURL().getHost();
    char c = host.charAt(0);
    if (c < '0' || c > '9')
      // we have a host name
      return (match(host)) ? TRUE : FALSE;
    // we check if the DNS was resolved
    Object o = req.getAttribute(ProxyletConstants.ATTR_NAME_DNS_RESULT + host);
    if (o == null)
      // we need a DNS
      return UNKNOWN;
    String[] names = (String[]) o;
    for (int i = 0; i < names.length; i++)
      if (match(names[i]))
        return TRUE;
    return FALSE;
  }
  
  public int matchResponse(HttpResponse resp) {
    String host = resp.getProlog().getURL().getHost();
    char c = host.charAt(0);
    if (c < '0' || c > '9')
      // we have a host name
      return (match(host)) ? TRUE : FALSE;
    // we check if the DNS was resolved
    Object o = resp.getRequest().getAttribute(ProxyletConstants.ATTR_NAME_DNS_RESULT + host);
    if (o == null)
      // we need a DNS
      return UNKNOWN;
    String[] names = (String[]) o;
    for (int i = 0; i < names.length; i++)
      if (match(names[i]))
        return TRUE;
    return FALSE;
  }
  
  public String toString() {
    return ("[" + Utils.HOST + "=" + getRegexp() + "]");
  }
  
  public static Criterion getInstance(String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? c : new HostCriterion(regexp);
  }
  
  public boolean equals(Object o) {
    if (o instanceof HostCriterion)
      return super.equals(o);
    return false;
  }
}
