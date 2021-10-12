// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl;

import java.net.InetSocketAddress;

import com.nextenso.mux.MuxHandler;

/**
 * @deprecated don't use this class. please use the new official com.nextenso.mux.MuxFactory and
 *             com.nextenso.mux.SimpleMuxFactory classes.
 */
@Deprecated
public interface MuxServerConnectionImplHandler extends MuxConnectionImplHandler {
  public void muxServerConnectionFailed(MuxHandler handler, InetSocketAddress local);
  
  public void muxServerConnectionOpened(MuxHandler handler, InetSocketAddress local);
  
  public void muxServerConnectionClosed(MuxHandler handler, InetSocketAddress local);
}
