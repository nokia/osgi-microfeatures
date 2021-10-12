// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.ha;

import static com.nextenso.http.agent.Utils.logger;

import java.io.Serializable;

import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.Transaction;
import com.alcatel.as.session.distributed.TransactionListener;
import com.nextenso.http.agent.Client;
import com.nextenso.http.agent.HttpChannel;

class RequestRecoverTransactionListener implements TransactionListener {
  
  private HttpChannel channel;
  
  public RequestRecoverTransactionListener(HttpChannel channel) {
    this.channel = channel;
  }
  
  public void transactionCompleted(Transaction tr, Serializable result) {
    if (logger.isDebugEnabled() && channel.getClient().isDsCreated()) {
      logger.debug("HTTP session recovered sid=" + Long.toHexString(channel.getClient().getId()));
    }
    handleRequest();
  }
  
  public void transactionFailed(Transaction tr, SessionException result) {
    handleRequest();
  }
  
  private void handleRequest() {
    Client client = channel.getClient();
    if (client.isDsCreated()) {
      // Session has been recovered successfully
      if (client.sessionRecovered()) {
        channel.handleRequest();
      }
    } else {
      // Unregistered clid
      channel.handleRequest();
    }
  }
  
}
