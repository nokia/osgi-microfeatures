// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.nio.channels.SelectionKey;

/**
 * Notify a socket listener about IO events.
 */
public interface SelectHandler {
  public void selected(SelectionKey key);
  
  public int getPriority();
}
