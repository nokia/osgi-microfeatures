package com.nextenso.http.agent.engine.criterion;

import alcatel.tess.hometop.gateways.utils.InetAddr;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpMessage;
import com.nextenso.proxylet.http.HttpSession;

public class ClientIPCriterion extends RegexpCriterion {
  
  private ClientIPCriterion(String regexp) throws CriterionException {
    super(regexp);
    if (regexp.indexOf('*') != -1) {
      return;
    }
    if (!InetAddr.isIPAddress(regexp)) {
      throw new CriterionException(CriterionException.INVALID_IP, regexp);
    }
  }
  
  public int match(ProxyletData data) {
    return matchSession(((HttpMessage) data).getSession());
  }
  
  public int matchSession(HttpSession session) {
    return match(session.getRemoteAddr()) ? TRUE : FALSE;
  }
  
  public String toString() {
    return ("[" + Utils.CLIENT_IP + "=" + getRegexp() + "]");
  }
  
  public static Criterion getInstance(String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? c : new ClientIPCriterion(regexp);
  }
  
  public boolean equals(Object o) {
    if (o instanceof ClientIPCriterion)
      return super.equals(o);
    return false;
  }
}
