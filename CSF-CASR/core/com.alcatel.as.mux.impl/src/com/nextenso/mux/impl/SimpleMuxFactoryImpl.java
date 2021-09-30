package com.nextenso.mux.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import alcatel.tess.hometop.gateways.reactor.Reactor;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.SimpleMuxFactory;
import com.nextenso.mux.SimpleMuxHandler;

public class SimpleMuxFactoryImpl extends SimpleMuxFactory {
  protected volatile MuxFactory _muxFactory;
  
  private MuxFactory getMuxFactory() {
    return _muxFactory;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public MuxConnection newMuxConnection(Reactor reactor, SimpleMuxHandler smh, InetSocketAddress to, Map opts) {
    ConnectionListenerAdapter clAdapter = new ConnectionListenerAdapter(smh);
    MuxHandlerAdapter mhAdapter = new MuxHandlerAdapter(smh);
    Map muxHandlerOptions = setOptions(mhAdapter, opts);
    
    String appName = (opts != null) ? (String) opts.get(OPT_APP_NAME) : null;
    
    MuxFactory f = getMuxFactory();
    MuxConnectionImpl cnx = (MuxConnectionImpl) f.newMuxConnection(reactor, clAdapter, mhAdapter, to, 0,
                                                                   appName, null, null, muxHandlerOptions);
    return cnx;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void connect(MuxConnection cnx) {
    MuxFactory f = getMuxFactory();
    f.connect(cnx);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public InetSocketAddress accept(Reactor r, SimpleMuxHandler smh, InetSocketAddress from, Map opts)
      throws IOException {
    ConnectionListenerAdapter clAdapter = new ConnectionListenerAdapter(smh);
    MuxHandlerAdapter mhAdapter = new MuxHandlerAdapter(smh);
    Map muxHandlerOptions = setOptions(mhAdapter, opts);
    MuxFactory f = getMuxFactory();
    return f.accept(r, clAdapter, mhAdapter, from, muxHandlerOptions);
  }
  
  // --------------------- Private methods ----------------------------------------------
  
  @SuppressWarnings("unchecked")
  private Map setOptions(MuxHandler mh, Map simpleMuxOptions) {
    mh.getMuxConfiguration().put(MuxHandler.CONF_DEMUX, Boolean.FALSE);
    Map muxHandlerOptions = new HashMap();
    if (simpleMuxOptions != null) {
      mh.getMuxConfiguration().put(MuxHandler.CONF_USE_NIO,
                                   Boolean.TRUE.equals(simpleMuxOptions.get(OPT_USE_NIO)));
      
      Long keepAlive = (Long) simpleMuxOptions.get(OPT_KEEP_ALIVE);
      if (keepAlive != null && keepAlive.longValue() > 0) {
        mh.getMuxConfiguration().put(MuxHandler.CONF_KEEP_ALIVE, keepAlive);
      }
      
      Long keepAliveIdleFactor = (Long) simpleMuxOptions.get(OPT_KEEP_ALIVE_IDLE_FACTOR);
      if (keepAliveIdleFactor != null && keepAliveIdleFactor.longValue() > 0) {
        mh.getMuxConfiguration().put(MuxHandler.CONF_ALIVE_IDLE_FACTOR, keepAliveIdleFactor);
      }
      
      copy(simpleMuxOptions, SimpleMuxFactory.OPT_LOGGER, muxHandlerOptions, MuxFactory.OPT_LOGGER);
      
      copy(simpleMuxOptions, SimpleMuxFactory.OPT_LOCAL_ADDR, muxHandlerOptions, MuxFactory.OPT_LOCAL_ADDR);
      
      copy(simpleMuxOptions, SimpleMuxFactory.OPT_ATTACH, muxHandlerOptions, MuxFactory.OPT_ATTACH);
      
      copy(simpleMuxOptions, SimpleMuxFactory.OPT_CONNECTION_TIMEOUT, muxHandlerOptions,
           MuxFactory.OPT_CONNECTION_TIMEOUT);
      
      copy(simpleMuxOptions, SimpleMuxFactory.OPT_INPUT_EXECUTOR, muxHandlerOptions,
           MuxFactory.OPT_INPUT_EXECUTOR);
      
      copy(simpleMuxOptions, SimpleMuxFactory.OPT_FLAGS, muxHandlerOptions, MuxFactory.OPT_FLAGS);
    }
    return muxHandlerOptions;
  }
  
  @SuppressWarnings("unchecked")
  private void copy(Map from, Object fromKey, Map to, Object toKey) {
    if (from != null) {
      Object val = from.get(fromKey);
      if (val != null) {
        to.put(toKey, val);
      }
    }
  }
  
  static class ConnectionListenerAdapter implements ConnectionListener {
    
    private final SimpleMuxHandler _simpleMuxHandler;
    
    ConnectionListenerAdapter(SimpleMuxHandler simpleMuxHandler) {
      _simpleMuxHandler = simpleMuxHandler;
    }
    
    @Override
    public void muxConnected(MuxConnection cnx, Throwable error) {
      _simpleMuxHandler.muxConnected(cnx, error);
    }
    
    @Override
    public void muxAccepted(MuxConnection cnx, Throwable error) {
      _simpleMuxHandler.muxAccepted(cnx, error);
    }
    
    @Override
    public void muxClosed(MuxConnection cnx) {
      _simpleMuxHandler.muxClosed(cnx);
    }
  }
  
  static class MuxHandlerAdapter extends MuxHandler {
    private final SimpleMuxHandler _simpleMuxHandler;
    
    MuxHandlerAdapter(SimpleMuxHandler simpleMuxHandler) {
      super(0, null, null, null);
      _simpleMuxHandler = simpleMuxHandler;
    }
    
    @Override
    public void muxData(MuxConnection cnx, MuxHeader hdr, ByteBuffer msg) {
      _simpleMuxHandler.muxData(cnx, hdr, msg);
    }
    
    @Override
    public void muxData(MuxConnection cnx, MuxHeader hdr, byte[] data, int off, int len) {
      _simpleMuxHandler.muxData(cnx, hdr, data, off, len);
    }
    
    @Override
    public int[] getCounters() {
      return null;
    }
    
    @Override
    public int getMajorVersion() {
      return 0;
    }
    
    @Override
    public int getMinorVersion() {
      return 0;
    }
  }
}
