// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 *
 */
package javax.jmdns;

import java.util.EventListener;

/**
 * Listener for network topology updates.
 * 
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public interface NetworkTopologyListener extends EventListener {
    /**
     * A network address has been added.<br/>
     * 
     * @param event
     *            The NetworkTopologyEvent providing the name and fully qualified type of the service.
     */
    void inetAddressAdded(NetworkTopologyEvent event);

    /**
     * A network address has been removed.
     * 
     * @param event
     *            The NetworkTopologyEvent providing the name and fully qualified type of the service.
     */
    void inetAddressRemoved(NetworkTopologyEvent event);

}
