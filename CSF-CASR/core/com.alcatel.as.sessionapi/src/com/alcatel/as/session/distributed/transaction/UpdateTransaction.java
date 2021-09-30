package com.alcatel.as.session.distributed.transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionNotAliveException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;

/**
 * Utility class to set attributes into an existing session
 *
 */
@SuppressWarnings("serial")
public class UpdateTransaction extends Transaction {
  List<Session.Attribute> attributes;
  
  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_GET}|{@link Transaction#TX_SERIALIZED} taking a list of attributes
   * @param type the session type
   * @param sessionId the session id
   * @param attributes a list of {@link Session.Attribute}
   */
  public UpdateTransaction(SessionType type, String sessionId, List<Session.Attribute> attributes) {
    super(type, sessionId, TX_SERIALIZED|TX_GET);
    this.attributes = attributes;
  }
 
  /**
   * Constructor:<br>
   * Transaction of type {@link Transaction#TX_GET}|{@link Transaction#TX_SERIALIZED} taking a map of attributes
   * @param type the session type
   * @param sessionId the session id
   * @param attributes a map of serializable attributes
   */
  public UpdateTransaction(SessionType type, String sessionId, Map<String, Serializable> attributes) {
    super(type, sessionId, TX_SERIALIZED|TX_GET);
    this.attributes = new ArrayList<Session.Attribute>();
    for (Iterator<String> i = attributes.keySet().iterator(); i.hasNext();) {
      String key = i.next();
      this.attributes.add(new Session.Attribute(key, (Serializable)attributes.get(key)));
    }
  }
  
  /**
   * Body of the transaction:
   * Set the attributes given to the constructor into the session
   */
  public void execute(Session session) throws SessionException {
    if (session != null)
    {
      for (int i = 0; i < attributes.size(); i++)
      {
        Session.Attribute a = (Session.Attribute) attributes.get(i);
        session.setAttribute(a.getName(), a.getValue());
      }
      session.commit(null);
    }
    else throw new SessionNotAliveException(this.getSessionId());
  }
}
