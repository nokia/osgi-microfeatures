// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;

/**
 * Utility class to create or update a session, with or without attributes
 *
 */
@SuppressWarnings("serial")
public class CreateGetTransaction extends Transaction {
  List<Session.Attribute> attributes;

  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_CREATE_GET}|{@link Transaction#TX_SERIALIZED}
   * @param type the session type
   * @param sessionId the session id
   */
  public CreateGetTransaction(SessionType type, String sessionId) {
    super(type, sessionId, Transaction.TX_CREATE_GET|Transaction.TX_SERIALIZED);
  }

  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_CREATE_GET}|{@link Transaction#TX_SERIALIZED} taking a list of attributes
   * @param type the session type
   * @param sessionId the session id
   * @param attributes a list of {@link Session.Attribute}
   */
  public CreateGetTransaction(SessionType type, String sessionId, List<Session.Attribute> attributes) {
    super(type, sessionId, Transaction.TX_CREATE_GET|Transaction.TX_SERIALIZED);
    this.attributes = attributes;
  }

  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_CREATE_GET}|{@link Transaction#TX_SERIALIZED} taking a map of attributes
   * @param type the session type
   * @param sessionId the session id
   * @param attributes a map of serializable attributes
   */
  public CreateGetTransaction(SessionType type, String sessionId, Map<String, Serializable> attributes) {
    super(type, sessionId, Transaction.TX_CREATE_GET|Transaction.TX_SERIALIZED);
    this.attributes = new ArrayList<Session.Attribute>();
    for (Iterator<String> i = attributes.keySet().iterator(); i.hasNext();) {
      String key = i.next();
      this.attributes.add(new Session.Attribute(key, (Serializable)attributes.get(key)));
    }
  }

  /**
   * Body of the transaction:
   * <ol>
   * <li>If attributes exist, set the attributes given to the constructor into the session,
   * <li>Commit the session 
   * </ol>
   */
  public void execute(Session session) throws SessionException {
    if (attributes != null) {
      for (int i = 0; i < attributes.size(); i++) {
        Session.Attribute a = (Session.Attribute) attributes.get(i);
        session.setAttribute(a.getName(), a.getValue());
      }
    }
    session.commit(null);
  }
}
