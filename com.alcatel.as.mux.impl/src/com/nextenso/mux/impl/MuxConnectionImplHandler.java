package com.nextenso.mux.impl;

import com.nextenso.mux.MuxConnection;

/**
 * @deprecated don't use this class. please use the new official com.nextenso.mux.MuxFactory and
 *             com.nextenso.mux.SimpleMuxFactory classes.
 */
@Deprecated
public interface MuxConnectionImplHandler {
  public void muxConnectionFailed(MuxConnection connection);
  
  public void muxConnectionOpened(MuxConnection connection);
  
  public void muxConnectionClosed(MuxConnection connection);
}
