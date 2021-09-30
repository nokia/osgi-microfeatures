package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.TrueCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;

public class PortCriterion extends Criterion {
  
  private int port;
  
  private PortCriterion(String port) throws NumberFormatException, CriterionException {
    this(Integer.parseInt(port));
  }
  
  public PortCriterion(int port) throws CriterionException {
    if (port <= 0)
      throw new CriterionException(CriterionException.INVALID_PORT, String.valueOf(port));
    this.port = port;
  }
  
  protected int getPort() {
    return port;
  }
  
  public int match(ProxyletData data) {
    if (data instanceof HttpRequest)
      return matchRequest((HttpRequest) data);
    return matchResponse((HttpResponse) data);
  }
  
  public int matchRequest(HttpRequest req) {
    return (match(req.getProlog().getURL().getPort())) ? TRUE : FALSE;
  }
  
  public int matchResponse(HttpResponse resp) {
    return (match(resp.getProlog().getURL().getPort())) ? TRUE : FALSE;
  }
  
  public boolean match(int p) {
    return (p != -1) ? (p == port) : (port == 80);
  }
  
  public String toString() {
    return ("[" + Utils.PORT + "=" + port + "]");
  }
  
  public static Criterion getInstance(String port) throws CriterionException {
    if ("*".equals(port))
      return TrueCriterion.getInstance();
    try {
      return new PortCriterion(port);
    } catch (NumberFormatException e) {
      throw new CriterionException(CriterionException.INVALID_PORT, port);
    }
  }
  
  public boolean includes(Criterion c) {
    // (port=80) implies (port=80)
    if (c instanceof PortCriterion)
      return (port == ((PortCriterion) c).getPort());
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // (port=80) rejects (port=8080)
    if (c instanceof PortCriterion)
      return (port != ((PortCriterion) c).getPort());
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof PortCriterion)
      return (port == ((PortCriterion) o).getPort());
    return false;
  }
  
  public int getDepth() {
    return 1;
  }
}
