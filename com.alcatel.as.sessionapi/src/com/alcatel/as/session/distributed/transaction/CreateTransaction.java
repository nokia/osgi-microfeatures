package com.alcatel.as.session.distributed.transaction;

import  com.alcatel.as.session.distributed.Transaction;
import  com.alcatel.as.session.distributed.Session;
import  com.alcatel.as.session.distributed.SessionType;
import  com.alcatel.as.session.distributed.SessionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.io.Serializable;

/**
 * Utility class to create a session with or without attributes
 *
 */
@SuppressWarnings("serial")
public class CreateTransaction extends Transaction {
  ArrayList<Session.Attribute> attributes;

  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_CREATE}|{@link Transaction#TX_SERIALIZED}
   * @param type the session type
   * @param sessionId the session id
   */
  public CreateTransaction(SessionType type, String sessionId) {
    super(type, sessionId, TX_CREATE|TX_SERIALIZED);
  }

  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_CREATE}|{@link Transaction#TX_SERIALIZED} taking a list of attributes
   * @param type the session type
   * @param sessionId the session id
   * @param attributes a list of {@link Session.Attribute}
   */
  public CreateTransaction(SessionType type, String sessionId, ArrayList<Session.Attribute> attributes) {
    super(type, sessionId, TX_CREATE|TX_SERIALIZED);
    this.attributes = attributes;
  }

  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_CREATE}|{@link Transaction#TX_SERIALIZED} taking a map of attributes
   * @param type the session type
   * @param sessionId the session id
   * @param attributes a map of serializable attributes
   */
  public CreateTransaction(SessionType type, String sessionId, Map<String, Serializable> attributes) {
    super(type, sessionId, Transaction.TX_CREATE|Transaction.TX_SERIALIZED);
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
    if (session == null) {
      return;
    }
    if (attributes != null) {
      for (int i = 0; i < attributes.size(); i++) {
        Session.Attribute a = (Session.Attribute) attributes.get(i);
        session.setAttribute(a.getName(), a.getValue());
      }
    }
    session.commit(null);
  }
  
}
