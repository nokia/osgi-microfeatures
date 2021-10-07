package com.nextenso.http.agent.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.nextenso.http.agent.impl.HttpResponseFacade;

;

public abstract class PushletOutputStream extends ByteArrayOutputStream {
  public static void setFactory(PushletOutputStreamFactory fac, HttpResponseFacade resp) {
    resp.setAttribute(ATTR_PUSHLET_OS_FACTORY, fac);
  }
  
  public static PushletOutputStream create(HttpResponseFacade resp) {
    PushletOutputStreamFactory fac = (PushletOutputStreamFactory) resp.getAttribute(ATTR_PUSHLET_OS_FACTORY);
    PushletOutputStream out = fac.createPushletOutputStream();
    return out;
  }
  
  public abstract void activate(boolean filterBody) throws IOException;
  
  private static final Object ATTR_PUSHLET_OS_FACTORY = new Object();
  
  public abstract void write(ByteBuffer ... bufs) throws IOException;

  public abstract boolean isDirect();

}
