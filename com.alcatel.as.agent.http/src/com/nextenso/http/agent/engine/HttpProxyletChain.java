package com.nextenso.http.agent.engine;

import com.nextenso.http.agent.engine.criterion.HostCriterion;
import com.nextenso.http.agent.engine.criterion.HostIPCriterion;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.criterion.Criterion;

public class HttpProxyletChain extends ProxyletChain {
  
  public static final int REQUEST_CHAIN = 1;
  public static final int RESPONSE_CHAIN = 2;
  
  private static final int DNS_BY_NAME_BIT = LOCK_BIT >> 1;
  private static final int DNS_BY_ADDR_BIT = LOCK_BIT >> 2;
  
  private static final int MASK_NO_DNS = 0x0FFFFFFF;
  
  public HttpProxyletChain(Context context, int type) {
    super(context, type);
  }
  
  private static Proxylet getByNameProxylet = new GetByNameProxylet();
  private static Proxylet getByAddrProxylet = new GetByAddrProxylet();
  
  // we override the default implementation because of DNS issues
  @Override
  public Proxylet nextProxylet(ProxyletStateTracker target) {
    int state = target.getProxyletState();
    if ((state & DNS_BY_NAME_BIT) == DNS_BY_NAME_BIT) {
      return getByNameProxylet;
    }
    if ((state & DNS_BY_ADDR_BIT) == DNS_BY_ADDR_BIT) {
      return getByAddrProxylet;
    }
    Proxylet[] proxylets = getProxylets();
    int index = (state & INDEX_MASK);
    if (state != index)
      // locked
      return proxylets[index];
    // unlocked
    for (int k = index; k < proxylets.length; k++) {
      int match = match(k, target);
      switch (match) {
      case Criterion.FALSE:
        break;
      case Criterion.TRUE:
        // we lock
        target.setProxyletState(k | LOCK_BIT);
        return proxylets[k];
      default:
        if (match % HostCriterion.UNKNOWN == 0) {
          // getByAddr
          target.setProxyletState(k | DNS_BY_ADDR_BIT);
          return getByAddrProxylet;
        }
        if (match % HostIPCriterion.UNKNOWN == 0) {
          // getByName
          target.setProxyletState(k | DNS_BY_NAME_BIT);
          return getByNameProxylet;
        }
      }
    }
    target.setProxyletState(proxylets.length);
    return null;
  }
  
  public int match(int index, ProxyletStateTracker target) {
    return (getCriterion(index).match(target));
  }
  
  public static void restoreProxyletStateNoDNS(ProxyletStateTracker target) {
    int state = target.getProxyletState();
    target.setProxyletState(state & MASK_NO_DNS);
  }
  
}
