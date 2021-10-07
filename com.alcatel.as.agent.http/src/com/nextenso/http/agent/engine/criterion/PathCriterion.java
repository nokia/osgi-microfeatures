package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpURL;

public class PathCriterion extends RegexpCriterion {
  
  private PathCriterion(String regexp) throws CriterionException {
    super(regexp);
  }
  
  public int match(ProxyletData data) {
    if (data instanceof HttpRequest)
      return matchRequest((HttpRequest) data);
    return matchResponse((HttpResponse) data);
  }
  
  public int matchRequest(HttpRequest req) {
    return (match(req.getProlog().getURL().getPath())) ? TRUE : FALSE;
  }
  
  public int matchResponse(HttpResponse resp) {
    return (match(resp.getProlog().getURL().getPath())) ? TRUE : FALSE;
  }
  
  public String toString() {
    return ("[" + Utils.PATH + "=" + getRegexp() + "]");
  }
  
  public static Criterion getInstance(String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    if (regexp.charAt(0) != '/' && regexp.charAt(0) != '*')
      regexp = '/' + regexp;
    return (c != null) ? c : new PathCriterion(regexp);
  }
  
  public boolean equals(Object o) {
    if (o instanceof PathCriterion)
      return super.equals(o);
    return false;
  }
  
  public static void main(String[] args) throws Exception {
    // java com.nextenso.proxylet.engine/criterion/http/PathCriterion "/x/*" "http://host/x/y"
    PathCriterion c = (PathCriterion) getInstance(args[0]);
    String path = new HttpURL(args[1]).getPath();
    System.out.println(c + " match \"" + path + "\" : " + c.match(path));
  }
}
