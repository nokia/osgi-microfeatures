package com.nextenso.agent;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.tracer.Log4jTracer;
import alcatel.tess.hometop.gateways.tracer.Tracer;
import alcatel.tess.hometop.gateways.tracer.TracerManager;
import alcatel.tess.hometop.gateways.utils.Utils;

public class MuxHandlerLogger {
  public static final String CALLOUT_MODULE = "CalloutAgent";
  public static final Logger calloutLogger = Logger.getLogger("callout");
  
  public static void init() {
  }
  
  /**
   * Creates the 2 Tracers for a MuxHandler: msg.log and pxlet.log
   */
  public static Logger[] makeMuxHandlerTracers(String protocol) {
    String p = protocol.toLowerCase();
    
    Logger msgLogger = Logger.getLogger("agent." + p);
    Logger pxletLogger = Logger.getLogger("agent." + p + ".pxlet");
    
    return new Logger[] { msgLogger, pxletLogger };
  }
  
  /**
   * Creates a Tracer for a MuxConnection
   */
  public static Logger makeMuxConnectionTracer(String stackName) {
    String sn = stripInstanceName(stackName);
    String logname = "callout.mux." + sn;
    return Logger.getLogger(logname);
  }
  
  private MuxHandlerLogger() {
  }
  
  /** 
   * Remove the session name from the agent instance name.
   */
  private static String stripInstanceName(String instance) {
    String inst = Utils.removeSpaces(instance);
    int groupSep = inst.indexOf("__");
    if (groupSep != -1) {
      inst = inst.substring(groupSep + 2);
    }
    return inst;
  }
}
