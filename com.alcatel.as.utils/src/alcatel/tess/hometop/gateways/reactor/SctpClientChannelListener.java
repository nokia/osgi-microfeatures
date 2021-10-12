// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

public interface SctpClientChannelListener extends SctpChannelListener {
  void connectionEstablished(SctpChannel cnx);
  
  void connectionFailed(SctpChannel cnx, Throwable error);
}
