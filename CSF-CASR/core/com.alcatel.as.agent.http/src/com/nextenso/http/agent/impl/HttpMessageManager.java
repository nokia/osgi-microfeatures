package com.nextenso.http.agent.impl;

import com.nextenso.proxylet.http.HttpSession;

public class HttpMessageManager {
  public static HttpRequestFacade makeRequest(HttpSession session, int reqid) {
    HttpRequestFacade req = new HttpRequestFacade();
    HttpResponseFacade rsp = new HttpResponseFacade();
    
    // init both request/response messages and chain them.
    req.setSession(session);
    req.setId(reqid);
    req.setResponse(rsp);
    rsp.setRequest(req);
    rsp.setSession(req.getSession());
    
    return (req);
  }
}
