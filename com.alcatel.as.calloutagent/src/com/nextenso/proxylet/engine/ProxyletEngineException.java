// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine;

// ProxyletAPI
import com.nextenso.proxylet.Proxylet;

public class ProxyletEngineException extends Exception {
  
  // the exception occurred in the engine
  public static final int ENGINE = 1;
  // the exception occurred while running a proxylet
  public static final int PROXYLET = 2;
  
  private ProxyletChain chain;
  private Proxylet proxylet;
  private Throwable throwable;
  private int type;
  
  /**
   * Thrown when an identified error occurs in the engine.
   */
  public ProxyletEngineException(ProxyletChain chain, Proxylet proxylet, String message) {
    super(message);
    this.type = ENGINE;
    this.chain = chain;
    this.proxylet = proxylet;
  }
  
  /**
   * Thrown when a throwable occurs in a Proxylet
   */
  public ProxyletEngineException(ProxyletChain chain, Proxylet proxylet, Throwable throwable) {
    super("Proxylet Exception: proxylet=" + (proxylet != null ? proxylet.getProxyletInfo() : null), throwable);
    this.type = PROXYLET;
    this.chain = chain;
    this.proxylet = proxylet;
    this.throwable = throwable;
  }
  
  public int getType() {
    return type;
  }
  
  public ProxyletChain getProxyletChain() {
    return chain;
  }
  
  public Context getContext() {
    return chain.getContext();
  }
  
  public Proxylet getProxylet() {
    return proxylet;
  }
  
  public Throwable getThrowable() {
    return throwable;
  }
}
