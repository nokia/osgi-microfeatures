// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.event;

import java.util.EventListener;

/**
 * This is the interface to implement in order to be notified when ProxyletEvents are fired.
 * <p/>In order to activate a ProxyletEventListener, it should be passed to the ProxyletData Object in <code>registerProxyletEventListener(ProxyletEventListener listener)</code>.
 */
public interface ProxyletEventListener extends EventListener {
        
    /**
     * Called when a ProxyletEvent is fired.
     * @param event the ProxyletEvent that was fired.
     */
    public void proxyletEvent(ProxyletEvent event);
        
}
