// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine;

// Jdk
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.impl.ProxyletDataImpl;

/**
 * This class manages async proxylets.
 */
public class AsyncProxyletManager {
  private final static Logger _logger = Logger.getLogger("callout.engine.AsyncProxyletManager");
  
  public interface ProxyletResumer {
    void resumeProxylet(ProxyletData msg, int status);
  }
  
  /**
   * Called when a proxylet returns SUSPEND.
   */
  public static void suspend(final ProxyletData msg, final ProxyletResumer resumer) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("AsyncProxyletManager.suspend: suspending msg: " + System.identityHashCode(msg));
    }
    
    Integer alreadyResumedStatus = null;
    Executor callerExecutor = PlatformExecutors.getInstance().getCurrentThreadContext().getCurrentExecutor();
    ClassLoader callerCL = Thread.currentThread().getContextClassLoader();
    
    synchronized (LOCK) {
      if ((alreadyResumedStatus = (Integer) msg.removeAttribute(ATTR_RESUMED_STATUS)) == null) {
        msg.setAttribute(ATTR_RESUMER, resumer);
        msg.setAttribute(ATTR_CALLER_EXECUTOR, callerExecutor);
        msg.setAttribute(ATTR_CALLER_CL, callerCL);
      }
    }
    
    if (alreadyResumedStatus != null) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("AsyncProxyletManager.suspend: proxylet already resumed msg "
            + System.identityHashCode(msg) + ": scheduling resume !");
      }
      scheduleResume(msg, resumer, alreadyResumedStatus.intValue(), callerExecutor, callerCL);
    }
  }
  
  /**
   * Resumes a suspended proxylet.
   */
  public static void resume(final ProxyletData msg, final int nextStatus) {
    ProxyletResumer resumer = null;
    Executor callerExecutor = null;
    ClassLoader callerCL = null;
    
    synchronized (LOCK) {
      resumer = (ProxyletResumer) msg.removeAttribute(ATTR_RESUMER);
      callerExecutor = (Executor) msg.removeAttribute(ATTR_CALLER_EXECUTOR);
      callerCL = (ClassLoader) msg.removeAttribute(ATTR_CALLER_CL);
      
      if (resumer == null) {
        if (_logger.isDebugEnabled()) {
          _logger.debug("AsyncProxyletManager.resume: message not currently suspended: "
              + System.identityHashCode(msg));
        }
        msg.setAttribute(ATTR_RESUMED_STATUS, new Integer(nextStatus));
      }
    }
    
    if (resumer != null) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("AsyncProxyletManager.resume: scheduling resume for msg "
            + System.identityHashCode(msg));
      }
      if (msg instanceof ProxyletDataImpl) {
        ((ProxyletDataImpl) msg).cancelSuspendListener();
      }
      
      scheduleResume(msg, resumer, nextStatus, callerExecutor, callerCL);
    }
  }
  
  private static void scheduleResume(final ProxyletData msg, final ProxyletResumer r, final int nextStatus,
                                     final Executor callerExecutor, final ClassLoader callerCL) {
    callerExecutor.execute(new Runnable() {
      public void run() {
        Thread.currentThread().setContextClassLoader(callerCL);
        r.resumeProxylet(msg, nextStatus);
      }
    });
  }
  
  private final static Object ATTR_RESUMER = new Object();
  private final static Object ATTR_CALLER_EXECUTOR = new Object();
  private final static Object ATTR_CALLER_CL = new Object();
  private final static Object ATTR_RESUMED_STATUS = new Object();
  private final static Object LOCK = new Object();
}
