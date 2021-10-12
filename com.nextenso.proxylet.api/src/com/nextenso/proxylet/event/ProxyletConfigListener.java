// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.event;

import java.util.EventListener;
import com.nextenso.proxylet.ProxyletException;

/**
 * This is the interface to implement in order to be notified when dynamic configuration changes occur.
 * <p/>In order to activate a ProxyletConfigListener, it should be passed to the ProxyletConfig Object in <code>registerProxyletConfigListener(ProxyletConfigListener listener)</code>.
 */
public interface ProxyletConfigListener extends EventListener {
        
    /**
     * Called when a ProxyletConfigEvent occurs.
     * @param event the ProxyletConfigEvent that wraps the new configuration and the changes.
     * @throws ProxyletException if the new ProxyletConfig is inadequate.
     */
    public void configEvent(ProxyletConfigEvent event)
        throws ProxyletException;
}
