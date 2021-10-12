// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine;

import java.util.List;

import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.engine.criterion.Criterion;

public abstract class ProxyletChain {
  
  private Proxylet[] _proxylets = new Proxylet[0];
  private Criterion[] _criteria = new Criterion[0];
  private ProxyletEnv[] _proxyletEnvs = new ProxyletEnv[0];
  private Context _context;
  private int _type;
  
  public ProxyletChain(Context context, int type) {
    _context = context;
    _type = type;
  }
  
  public void init() throws ProxyletException {
    int i = _proxyletEnvs.length;
    for (int k = 0; k < i; k++) {
      _proxyletEnvs[k].init();
    }
  }
  
  public int getType() {
    return _type;
  }
  
  protected void setType(int type) {
    _type = type;
  }
  
  public Context getContext() {
    return _context;
  }
  
  public ProxyletEnv[] getProxyletEnvs() {
    return _proxyletEnvs;
  }
  
  public int getSize() {
    return _proxyletEnvs.length;
  }
  
  protected Proxylet[] getProxylets() {
    return _proxylets;
  }
  
  protected Criterion getCriterion(int index) {
    return _criteria[index];
  }
  
  public void destroy() {
    int i = _proxyletEnvs.length;
    for (int k = 0; k < i; k++) {
      _proxyletEnvs[k].destroy();
    }
  }
  
  public void setValue(List list) {
    int i = list.size();
    _proxyletEnvs = new ProxyletEnv[i];
    _proxylets = new Proxylet[i];
    _criteria = new Criterion[i];
    for (int k = 0; k < i; k++) {
      _proxyletEnvs[k] = (ProxyletEnv) list.get(k);
      _proxylets[k] = _proxyletEnvs[k].getProxylet();
      _criteria[k] = _proxyletEnvs[k].getCriterion();
    }
  }
  
  public ProxyletEnv[] getValue() {
    return _proxyletEnvs;
  }
  
  /**
   * The message stores a proxyletState which is an int. This int lets the chain
   * find out which proxylet is the next one to check. The proxyletState is
   * formatted like this: bit0 (sign bit) : always '0' bit1 (0x40000000) : '1'
   * if the chain is locked bit2-31 : the index of the next proxylet to check
   */
  
  protected static final int LOCK_BIT = 0x40000000;
  protected static final int INDEX_MASK = 0x3FFFFFFF;
  
  public abstract Proxylet nextProxylet(ProxyletStateTracker target);
  
  public void shift(ProxyletStateTracker target, int i) {
    int state = target.getProxyletState();
    int index = (state & INDEX_MASK);
    // we unlock
    target.setProxyletState(index + i);
  }
  
  public void pad(ProxyletStateTracker target) {
    target.setProxyletState(_proxylets.length);
  }
  
  public void reset(ProxyletStateTracker target) {
    target.setProxyletState(0);
  }
  
  public boolean hasMore(ProxyletStateTracker target) {
    return (target.getProxyletState() != _proxylets.length);
  }
  
  /*******************************************************************
   * An inner interface that keeps track of a Proxylet state (an int).
   ******************************************************************/
  
  public interface ProxyletStateTracker extends ProxyletData {
    
    public int getProxyletState();
    
    public void setProxyletState(int state);
  }
}
