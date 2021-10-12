// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.event;

import java.util.EventListener;

/**
 * This is the interface to implement in order to be notified when ProxyletContextEvents are fired.
 * <p/>In order to activate a ProxyletContextListener, it should be passed to the ProxyletContext Object in <code>registerProxyletContextListener(ProxyletContextListener listener)</code>.
 */
public interface ProxyletContextListener extends EventListener {
        
    /**
     * Called when the ProxyletContext is about to be destroyed by the Engine.
     * <br/>This event is fired by the Engine.
     * @param event the ProxyletContextEvent fired by the Engine.
     */
    public void contextDestroyed(ProxyletContextEvent event);
        
    /**
     * Called when a ProxyletContextEvent is fired.
     * @param event the ProxyletContextEvent that was fired.
     */
    public void contextEvent(ProxyletContextEvent event);
        
}
