// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.impl;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.agent.event.AsynchronousEvent;
import com.nextenso.agent.event.AsynchronousEventListener;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.SuspendListener;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.ProxyletInvocationHandler;
import com.nextenso.proxylet.event.ProxyletEvent;
import com.nextenso.proxylet.event.ProxyletEventListener;

import alcatel.tess.hometop.gateways.utils.Hashtable;

public class ProxyletDataImpl implements AsynchronousEventListener, ProxyletChain.ProxyletStateTracker {
  
  protected static final int EVENT_DATA_CONSTRUCTED = 1;
  protected static final int EVENT_DATA_NOT_CONSTRUCTED = 2;
  
  private static final ProxyletEventListener[] INIT_LISTENERS = new ProxyletEventListener[0];
  
  protected final Object listenersLock = new Object();
  protected ProxyletEventListener[] listeners = INIT_LISTENERS; // we avoid a NullPointerException in case setStaticListeners is not called
  private final Hashtable _attributes = new Hashtable();
  private ProxyletContext _context;
  private int _id;
  private int _proxyletState = 0;
  private boolean _syncAttrs = true;
  
  private SuspendListener _suspendListener;
  private final AtomicReference<Future> _future = new AtomicReference<Future>();
  
  private static class DataRunnable implements Runnable {
    ProxyletDataImpl _data;
    
    public DataRunnable(ProxyletDataImpl data) {
      _data = data;
    }
    
    @Override
    public void run() {
      SuspendListener listener = _data.getSuspendListener();
      if (listener != null) {
        _data.cancelSuspendListener();
        listener.notResumedData(_data);
      }
    }
    
  }
  
  protected ProxyletDataImpl() {
    this (true);
  }
  protected ProxyletDataImpl(boolean syncAttrs){
    _syncAttrs = syncAttrs;
  }

  public void recycled() {
	  listeners = INIT_LISTENERS;
	  _context = null;
	  _id = 0;
	  _proxyletState = 0;
	  _syncAttrs = true;
	  _suspendListener = null;
	  _attributes.clear();
  }
    
  protected void syncAttrs(boolean syncAttrs) {
	  _syncAttrs = syncAttrs;
  }
  
  // we set the listeners to a static read-only value at initialization
  // if the list needs to be modified, setPrivateListeners must be called
  // to get a private list of listeners
  public void setStaticListeners(ProxyletEventListener[] listenersArray) {
    // no need to synchronize - done at initialization time
    if (listenersArray == null) {
      listeners = INIT_LISTENERS;
      return;
    }
    listeners = new ProxyletEventListener[listenersArray.length];
    System.arraycopy(listenersArray, 0, listeners, 0, listenersArray.length);
  }
  
  /**
   * Reset all our attributes.
   */
  public void reset() {
    initProxyletState();
    _attributes.clear();
    listeners = INIT_LISTENERS;
    setProxyletContext(null);
    setId(0);
  }
  
  /******************************************************
   * Implementation of ProxyletData
   *******************************************************/
  
  public void setId(int id) {
    _id = id;
  }
  
  public int getId() {
    return _id;
  }
  
  public void setProxyletContext(ProxyletContext context) {
    _context = context;
  }
  
  public ProxyletContext getProxyletContext() {
    return _context;
  }
  
  protected void asynchronousEventFired() {
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#fireProxyletEvent(java.lang.Object,
   *      boolean)
   */
  public void fireProxyletEvent(Object source, boolean asynchronous) {
    if (listeners.length == 0) {
      return;
    }
    
    if (asynchronous) {
      asynchronousEventFired();
      AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, source, EVENT_DATA_NOT_CONSTRUCTED));
    } else {
      ProxyletEvent event = new ProxyletEvent(source, this);
      for (ProxyletEventListener listener : listeners) {
        listener.proxyletEvent(event);
      }
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#fireProxyletEvent(com.nextenso.proxylet.event.ProxyletEvent,
   *      boolean)
   */
  public void fireProxyletEvent(ProxyletEvent event, boolean asynchronous) {
    if (listeners.length == 0) {
      return;
    }
    
    if (asynchronous) {
      asynchronousEventFired();
      AsynchronousEventScheduler.schedule(new AsynchronousEvent(this, event, EVENT_DATA_CONSTRUCTED));
    } else {
      for (ProxyletEventListener listener : listeners) {
        listener.proxyletEvent(event);
      }
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#registerProxyletEventListener(com.nextenso.proxylet.event.ProxyletEventListener)
   */
  public void registerProxyletEventListener(ProxyletEventListener listener) {
    // Wraps the listener with our dynamic proxy, which sets proper class loader.
    ProxyletEventListener l = (ProxyletEventListener) ProxyletInvocationHandler.newInstance(listener
        .getClass().getClassLoader(), listener);
    
    synchronized (listenersLock) {
      int n = listeners.length;
      ProxyletEventListener[] clone = new ProxyletEventListener[n + 1];
      System.arraycopy(listeners, 0, clone, 0, n);
      clone[n] = l;
      listeners = clone;
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#deregisterProxyletEventListener(com.nextenso.proxylet.event.ProxyletEventListener)
   */
  public void deregisterProxyletEventListener(ProxyletEventListener listener) {
    synchronized (listenersLock) {
      int n = listeners.length;
      int index = -1;
      for (int k = 0; k < n; k++) {
        // Warning: listeners[k] is a Proxy to the listener and we must get the inner object ..
        if (ProxyletInvocationHandler.getProxylet(listeners[k]) == listener) {
          index = k;
          break;
        }
      }
      if (index == -1) {
        return;
      }
      ProxyletEventListener[] clone = new ProxyletEventListener[n - 1];
      if (index > 0) {
        System.arraycopy(listeners, 0, clone, 0, index);
      }
      if (index != (n - 1)) {
        System.arraycopy(listeners, index + 1, clone, index, n - 1 - index);
      }
      listeners = clone;
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#getAttribute(java.lang.Object)
   */
  public Object getAttribute(Object key) {
    if (_syncAttrs){
      synchronized (this){
	return _attributes.get(key);
      }
    } else {
      return _attributes.get(key);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#getAttributeNames()
   */
  public Enumeration getAttributeNames() {
    if (_syncAttrs){
      synchronized (this){
	return _attributes.keys();
      }
    } else {
      return _attributes.keys();
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#setAttribute(java.lang.Object,
   *      java.lang.Object)
   */
  public void setAttribute(Object key, Object o) {
    if (o == null) {
      removeAttribute(key);
    } else {
      if (_syncAttrs){
	synchronized (this){
	  _attributes.put(key, o);
	}
      } else {
	_attributes.put(key, o);
      }
    }
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#removeAttribute(java.lang.Object)
   */
  public Object removeAttribute(Object key) {
    if (_syncAttrs){
      synchronized (this){
	return _attributes.remove(key);
      }
    } else {
      return _attributes.remove(key);
    }
  }
 
  // note : the synchronization seems broken : used in sms/ShortMessage where the arg should be sync'ed as well (message cloning)
  protected void setAttributes(Hashtable attributes) {
    if (_syncAttrs){
      synchronized (this){
	_attributes.clear();
	if (attributes != null) {
	  Iterator it = attributes.keysIterator();
	  while (it.hasNext()) {
	    Object o = it.next();
	    _attributes.put(o, attributes.get(o));
	  }
	}
      }
    } else {
      _attributes.clear();
	if (attributes != null) {
	  Iterator it = attributes.keysIterator();
	  while (it.hasNext()) {
	    Object o = it.next();
	    _attributes.put(o, attributes.get(o));
	  }
	}
    }
  }
  
  // note : the synchronization seems broken : used in sms/ShortMessage
  protected Hashtable getAttributes() {
    if (_syncAttrs){
      synchronized (this){
	return _attributes;
      }
    } else {
      return _attributes;
    }
  }
  
  /**
   * @see com.nextenso.agent.event.AsynchronousEventListener#asynchronousEvent(java.lang.Object,
   *      int)
   */
  public void asynchronousEvent(Object data, int type) {
    ProxyletEventListener[] localListeners = listeners;
    int n = localListeners.length;
    if (n == 0) {
      return;
    }
    
    ProxyletEvent event = null;
    if (type == EVENT_DATA_CONSTRUCTED) {
      // the data is the event
      event = (ProxyletEvent) data;
    } else if (type == EVENT_DATA_NOT_CONSTRUCTED) {
      // the data is the event source
      event = new ProxyletEvent(data, this);
    }
    
    if (event != null) {
      fireProxyletEvent(event, false);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.engine.ProxyletChain.ProxyletStateTracker#getProxyletState()
   */
  public int getProxyletState() {
    return _proxyletState;
  }
  
  /**
   * @see com.nextenso.proxylet.engine.ProxyletChain.ProxyletStateTracker#setProxyletState(int)
   */
  public void setProxyletState(int i) {
    _proxyletState = i;
  }
  
  public void initProxyletState() {
    _proxyletState = 0;
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#resume(int)
   */
  @Override
  public void resume(int status) {
    throw new UnsupportedOperationException("resume() is not supported");
  }
  
  /**
   * @see com.nextenso.proxylet.ProxyletData#setSuspendListener(com.nextenso.proxylet.SuspendListener,
   *      long)
   */
  @Override
  public void setSuspendListener(SuspendListener listener, long delay) {
    cancelSuspendListener();
    if (listener != null && delay > 0) {
      _suspendListener = listener;
      PlatformExecutor executor = PlatformExecutors.getInstance().getCurrentThreadContext()
          .getCurrentExecutor();
      Runnable r = new DataRunnable(this);
      _future.set(executor.schedule(r, delay, TimeUnit.MILLISECONDS));
    }
  }
  
  private SuspendListener getSuspendListener() {
    return _suspendListener;
  }
  
  public void cancelSuspendListener() {
    Future future = _future.getAndSet(null);
    if (future != null && !future.isCancelled() && !future.isDone()) {
      future.cancel(false);
    }
    _suspendListener = null;
  }  
}
