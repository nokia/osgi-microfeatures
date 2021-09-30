package com.nextenso.http.agent.impl;

//import com.nextenso.http.agent.Utils;

//import alcatel.tess.hometop.gateways.utils.ObjectPool;

public class HttpMessageManager {
	
//  private final static ObjectPool _reqPool = new ObjectPool();
//  private final static ObjectPool _rspPool = new ObjectPool();
//  private static volatile boolean _useMessagePool = false;
//  
//  public static void useMessagePool(boolean useMessagePool) {
//	  _useMessagePool = useMessagePool;
//	  if (_useMessagePool) {
//		  Utils.logger.warn("Http Proxylet container configured in message pool mode");
//	  }
//  }

  public static HttpRequestFacade makeRequest(String remoteAddr) {	  
    HttpRequestFacade req;
    HttpResponseFacade rsp;
    
//    if (_useMessagePool) {
//    	req = (HttpRequestFacade) _reqPool.acquire(() -> new HttpRequestFacade());
//        rsp = (HttpResponseFacade) _rspPool.acquire(() -> new HttpResponseFacade());
//    } else {
    	req = new HttpRequestFacade();
    	rsp = new HttpResponseFacade();
//    }
    
    // init both request/response messages and chain them.
    req.setResponse(rsp);
    req.setRemoteAddress(remoteAddr);
    rsp.setRequest(req);
    
    return (req);
  }
  
  public static void release(HttpRequestFacade req) {
//	  if (_useMessagePool) {
//		  _rspPool.release((HttpResponseFacade) req.getResponse());
//		  _reqPool.release(req);
//	  }
  }
}
