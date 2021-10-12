// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.transaction;

import com.alcatel.as.session.distributed.SessionManager;
import  com.alcatel.as.session.distributed.Transaction;
import  com.alcatel.as.session.distributed.Session;
import  com.alcatel.as.session.distributed.SessionType;
import  com.alcatel.as.session.distributed.SessionException;
import  com.alcatel.as.session.distributed.SessionNotAliveException;
import com.alcatel.as.session.distributed.TransactionListener;
import com.alcatel.as.session.distributed.Session.Attribute;

import java.util.ArrayList;

/**
 * Utility class to get all the attributes of a session
 *
 */
@SuppressWarnings("serial")
public class FetchTransaction extends Transaction {
  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_GET}|{@link Transaction#TX_SERIALIZED}
   * @param type the session type
   * @param sessionId the session id
   */
  public FetchTransaction(SessionType type, String sessionId) {
    super(type, sessionId, TX_GET|TX_SERIALIZED);
  }
  
  /**
   * Body of the transaction:
   * A List of {@link Session.Attribute} is:
   * <ul>
   * <li>returned by {@link SessionManager#execute(Transaction)} for synchronous transaction
   * <li>the result parameter of {@link TransactionListener#transactionCompleted(Transaction, java.io.Serializable)} 
   * when using {@link SessionManager#execute(Transaction, TransactionListener)}
   * for asynchronous transaction
   */
  public void execute(Session session) throws SessionException {
    if (session == null) {
      throw new SessionNotAliveException();
    }
    ArrayList<Attribute> list = new ArrayList<Attribute>(session.getAttributes());
    session.commit(list);
  }
}
