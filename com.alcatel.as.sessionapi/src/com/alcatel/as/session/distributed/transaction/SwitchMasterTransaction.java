package com.alcatel.as.session.distributed.transaction;

import java.util.List;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;

/**
 * Utility class to switch the session master 
 *
 */
@SuppressWarnings("serial")
public class SwitchMasterTransaction extends Transaction {
  List<Session.Attribute> attributes;

  /**
   * Constructor:<br>
   * @param type the session type
   * @param sessionId the session id
   */
  public SwitchMasterTransaction(SessionType type, String sessionId) {
    super(type, sessionId, Transaction.TX_GET|Transaction.TX_METADATA|Transaction.TX_SERIALIZED|(1 << 25));
    if (type.isSmartKey(sessionId)) throw new IllegalArgumentException("SwitchMasterTransaction is not supported for smart key");
  }

  /**
   * Switch the master of the session
   * @throws SessionException
   * <ul>
   * <li>If the session does not exist,
   * <li>If the session is not replicated,
   * <li>If the session is not running on the current master
   * </ul>
   */
  @Override
  final public void execute(Session session) throws SessionException {
    if (session == null) throw new SessionException("null session");
    if (!session.isMaster()) throw new SessionException("I am not the master of " + this);
    session.commit(null);
  }

  /**
   * This transaction is only allowed for the local group
   */
  @Override
  final public String getDestinationGroup() {
    return null;
  }
  
}
