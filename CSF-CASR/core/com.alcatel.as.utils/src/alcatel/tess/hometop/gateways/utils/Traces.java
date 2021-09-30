package alcatel.tess.hometop.gateways.utils;

import alcatel.tess.hometop.gateways.tracer.Level;
import alcatel.tess.hometop.gateways.tracer.Tracer;
import alcatel.tess.hometop.gateways.tracer.TracerManager;

public class Traces implements Level {
  
  public static Tracer def = null;
  public static Tracer http = null;
  public static Tracer pxlet = null;
  public static Tracer connect = null;
  public static Tracer access = null;
  
  public static void flush() {
    TracerManager.flush();
  }
  
  public static void clear() {
    TracerManager.clear();
  }
  
  static {
    def = TracerManager.getTracer();
    
    http = TracerManager.getTracer("http");
    if (http == null)
      http = def;
    
    pxlet = TracerManager.getTracer("pxlet");
    if (pxlet == null)
      pxlet = def;
    
    connect = TracerManager.getTracer("connect");
    if (connect == null)
      connect = def;
    
    access = TracerManager.getTracer("access");
    if (access == null)
      access = def;
  }
}
