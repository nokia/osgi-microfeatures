// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

public interface SctpServerChannelListener extends SctpChannelListener {
  void connectionAccepted(SctpServerChannel ssc, SctpChannel client);
  
  void serverConnectionClosed(SctpServerChannel ssc, Throwable err);
}
