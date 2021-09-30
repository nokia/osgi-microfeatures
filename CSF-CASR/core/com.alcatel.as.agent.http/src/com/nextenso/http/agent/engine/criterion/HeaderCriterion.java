package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpMessage;

public class HeaderCriterion extends RegexpCriterion {
  
  private String name;
  
  private HeaderCriterion(String name, String regexp) throws CriterionException {
    super(regexp);
    if (name == null || name.length() == 0)
      throw new CriterionException(CriterionException.INVALID_HEADER, null);
    this.name = name.toLowerCase();
  }
  
  private HeaderCriterion(String name) throws CriterionException {
    super();
    if (name == null || name.length() == 0)
      throw new CriterionException(CriterionException.INVALID_HEADER, null);
    this.name = name.toLowerCase();
  }
  
  public String getHeaderName() {
    return name;
  }
  
  public int match(ProxyletData data) {
    HttpMessage message = (HttpMessage) data;
    return (match(message.getHeaders().getHeader(name))) ? TRUE : FALSE;
  }
  
  public String toString() {
    String value = getRegexp();
    if (value == null)
      value = "*";
    return ("[" + Utils.HEADER + "=" + name + ':' + value + "]");
  }
  
  public static Criterion getInstance(String name, String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? new HeaderCriterion(name) : new HeaderCriterion(name, regexp);
  }
  
  public boolean includes(Criterion c) {
    if (c instanceof HeaderCriterion) {
      boolean b = ((HeaderCriterion) c).getHeaderName().equals(name);
      return (b && super.includes(c));
    }
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    if (c instanceof HeaderCriterion) {
      boolean b = ((HeaderCriterion) c).getHeaderName().equals(name);
      return (b && super.excludes(c));
    }
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof HeaderCriterion) {
      boolean b = ((HeaderCriterion) o).getHeaderName().equals(name);
      return (b && super.equals(o));
    }
    return false;
  }
}
