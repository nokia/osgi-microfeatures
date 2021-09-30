package com.nextenso.proxylet.mgmt.impl;

import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.nextenso.agent.AgentConstants;
import com.nextenso.agent.MuxMonitorImpl;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.event.MuxMonitorable;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletInvocationHandler;
import com.nextenso.proxylet.mgmt.CommandEvent;
import com.nextenso.proxylet.mgmt.Monitor;
import com.nextenso.proxylet.mgmt.Monitorable;

public class ProxyletMonitorImpl implements Monitor {
  
  private MuxHandler handler;
  private Hashtable<Monitorable, MonDesc> monitorables = new Hashtable<Monitorable, MonDesc>();
  final static Logger _log = Logger.getLogger(ProxyletMonitorImpl.class);
  
  private interface MonDesc {
    String getAppName();
    
    String getAppInstance();
  }
  
  public ProxyletMonitorImpl(Context ctx) {
    this.handler = ctx.getMuxHandler();
  }
  
  private static int count = 1;
  
  private static synchronized int getCount() {
    return count++;
  }
  
  /***************************************
   * Implementation of Monitor interface
   **************************************/
  
  public boolean registerMonitorable(Monitorable monitorableObject) {
    // We are called here from the proxylet, which implements the Monitorable interface.
    // We now must decorate this monitorable with our dynamic proxy, which will set the
    // proper context class loader ...
    Monitorable monitorable = (Monitorable) ProxyletInvocationHandler.newInstance(monitorableObject
        .getClass().getClassLoader(), monitorableObject);
    
    String appName = ProxyletInvocationHandler.getProxyletClass(monitorable).getName();
    int index = appName.lastIndexOf('.');
    if (index != -1)
      appName = appName.substring(index + 1);
    
    final String appInstance = appName + "-" + String.valueOf(getCount());
    
    MuxMonitorImpl.startReporter(handler != null ? handler.getInstanceName() : AgentConstants.AGENT_INSTANCE,
        0, appName, appInstance, new MonitorableWrapper(monitorable));
    
    final String $appName = appName;
    monitorables.put(monitorable, new MonDesc() {
      public String getAppName() {
        return $appName;
      }
      
      public String getAppInstance() {
        return appInstance;
      }
    });
    
    return true;
  }
  
  @Deprecated
  @SuppressWarnings("deprecation")
  public void sendMessage(Monitorable monitorable, int level, String message) {
    sendAlarm(monitorable, 0, level, message);
  }
  
  public void sendAlarm(final Monitorable monitorable, final int alarmcode, final int level,
                        final String message) {
    if (message == null)
      return;
    
    _log.warn("sendAlarm not supported: " + level + ":" + message);
  }
  
  public String getInstanceName(Monitorable monitorable) {
    return monitorables.get(monitorable).getAppInstance(); //throws NullPointer if not found!
  }
  
  //
  // Class that wraps a Monitorable to make it a MuxMonitorable
  // 
  private static class MonitorableWrapper implements MuxMonitorable {
    private Monitorable _monitorable;
    
    private MonitorableWrapper(Monitorable monitorable) {
      this._monitorable = monitorable;
    }
    
    public int[] getCounters() {
      return _monitorable.getCounters();
    }
    
    public int getMajorVersion() {
      return _monitorable.getMajorVersion();
    }
    
    public int getMinorVersion() {
      return _monitorable.getMinorVersion();
    }
    
    public void commandEvent(int command, int[] intParams, String[] strParams) {
      _monitorable.commandEvent(new CommandEvent(command, intParams, strParams));
    }
    
    public void muxGlobalEvent(int identifierI, String identifierS, byte[] data, int off, int len) {
    }
    
    public void muxLocalEvent(int identifierI, String identifierS, Object data) {
    }
  }

}
