package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpMessage;
import com.nextenso.proxylet.http.HttpSession;

public class ClientIDCriterion extends RegexpCriterion {
  
  private ClientIDCriterion(String regexp) throws CriterionException {
    super(regexp);
  }
  
  public int match(ProxyletData data) {
    return matchSession(((HttpMessage) data).getSession());
  }
  
  public int matchSession(HttpSession session) {
    return match(session.getRemoteId()) ? TRUE : FALSE;
  }
  
  public String toString() {
    return ("[" + Utils.CLIENT_ID + "=" + getRegexp() + "]");
  }
  
  public static Criterion getInstance(String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? c : new ClientIDCriterion(regexp);
  }
  
  public boolean equals(Object o) {
    if (o instanceof ClientIDCriterion)
      return super.equals(o);
    return false;
  }
}
