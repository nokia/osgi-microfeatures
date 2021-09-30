package com.nextenso.http.agent.demux;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.nextenso.http.agent.Utils;

public class NextHopEvaluator {
  
  private Invocable invocableEngine; 
  private final static String PROXY_FUNCTION = "nextProxy";
  final static String PROXY_PREFIX = "PROXY ";
  public final static String DEFAULT_NEXT_PROXY = "function nextProxy(host) { return \"DIRECT\"; }";

  private final static String TUNNEL_FUNCTION = "isTunnelingAllowed";
  public final static String DEFAULT_CONNECT_TUNNEL = "function isTunnelingAllowed(host, port) { return false; }";

  private final static String SERVER_FUNCTION = "nextServer";
  final static String SERVER_PREFIX = "SERVER ";
  public final static String DEFAULT_NEXT_SERVER = "function nextServer(host, path) { return \"DIRECT\"; }";

  public NextHopEvaluator(String nextProxy, String nextServer, String connectTunneling)   {
    ScriptEngine jsEngine = new ScriptEngineManager(ClassLoader.getSystemClassLoader()).getEngineByName("js");
    invocableEngine = (Invocable)jsEngine;
    try {
      jsEngine.eval(nextProxy);
    }
    catch (ScriptException e) {
      try {
        Utils.logger.error("invalid nextProxy configuration", e);
        jsEngine.eval(DEFAULT_NEXT_PROXY);
      }
      catch (ScriptException e1)
      {
        Utils.logger.debug(DEFAULT_NEXT_PROXY, e1);
      }
    }
    try {
      jsEngine.eval(connectTunneling);
    }
    catch (ScriptException e) {
      try {
        Utils.logger.error("invalid connectTunneling configuration", e);
        jsEngine.eval(DEFAULT_CONNECT_TUNNEL);
      }
      catch (ScriptException e1)
      {
        Utils.logger.debug(DEFAULT_CONNECT_TUNNEL, e1);
      }
    }

    try {
      jsEngine.eval(nextServer);
    }
    catch (ScriptException e) {
      try {
        Utils.logger.error("invalid nextServer configuration", e);
        jsEngine.eval(DEFAULT_NEXT_SERVER);
      }
      catch (ScriptException e1)
      {
        Utils.logger.debug(DEFAULT_NEXT_SERVER, e1);
      }
    }
  }

  public String getNextProxy(String host) {
    try {
      Object proxy = invocableEngine.invokeFunction(PROXY_FUNCTION, host);
      if (proxy != null) {
        String result = ((String) proxy).trim();
        if (result.startsWith(PROXY_PREFIX)) return result.substring(PROXY_PREFIX.length());
      }
    }
    catch (Exception e) {
      Utils.logger.warn("getNextProxy " + host, e);
    }
    return null;
  }

  public boolean isConnectTunneling(String host, int port) {
    try {
      return  (Boolean) invocableEngine.invokeFunction(TUNNEL_FUNCTION, host, port);
    }
    catch (Exception e) {
      Utils.logger.warn("isConnectTunneling " + host +":"+port, e);
      return false;
    }
  }

  public String getNextServer(String host, String path) {
    try {
      Object proxy = invocableEngine.invokeFunction(SERVER_FUNCTION, host, path);
      if (proxy != null) {
        String result = ((String) proxy).trim();
        if (! "DIRECT".equals(result)) return result;
      }
    }
    catch (Exception e) {
      Utils.logger.warn("getNextProxy " + host, e);
    }
    return null;
  }

}
