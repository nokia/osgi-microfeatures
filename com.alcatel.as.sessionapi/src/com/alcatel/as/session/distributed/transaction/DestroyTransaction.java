// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.transaction;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionNotAliveException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;

/**
 * Utility class to destroy a session
 *
 */
@SuppressWarnings("serial")
public class DestroyTransaction extends Transaction {
  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_GET}|{@link Transaction#TX_READ_ONLY}
   * @param type the session type
   * @param sessionId the session id
   */
  public DestroyTransaction(SessionType type, String sessionId) {
    super(type, sessionId, TX_GET|TX_SERIALIZED);
  }
  
  /**
   * Body of the transaction:
   * Destroy the session
   */
  public void execute(Session session) throws SessionException {
    if (session == null) {
      throw new SessionNotAliveException();
    }
    session.destroy(null);
  }
}
