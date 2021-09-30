package com.alcatel.as.session.distributed;

import java.util.List;

/**
 * @deprecated
 * @internal
 * <p>Interface for all asynchronous tasks schedulable for one session.
 */
public abstract class AsyncSessionTask {
  /**
   * A task has completed.
   * @param type The session type,
   * @param sessionId The session id.
   */
  public abstract void taskCompleted(SessionType type, String sessionId);

  /**
   * A task has failed.
   * @param type The session type,
   * @param sessionId The session id,
   * @param error The error cause.
   */
  public abstract void taskFailed(SessionType type, String sessionId, SessionException error);

  /**
   * Executes the transaction. Ultimately, you MUST end up executing {@linkplain Session#destroy(java.io.Serializable) destroy}, 
   * {@linkplain  com.alcatel.as.session.distributed.Session#rollback rollback} or {@linkplain com.alcatel.as.session.distributed.Session#commit commit}
   * on the corresponding session.
   *
   * @param session the session this task is operating on. 
   *  
   * @throws SessionException, if you need to abort the transaction due to an unexpected error, you may throw an exception, 
   * in which case the transaction will be automatically rolled back, and you will be callbacked in 
   * {@linkplain com.alcatel.as.session.distributed.AsyncSessionTask#taskFailed taskFailed}
   * method. However, you might prefer terminating the transaction with the {@linkplain  com.alcatel.as.session.distributed.Session#rollback rollback} method, 
   * in which case you will be callbacked in {@linkplain com.alcatel.as.session.distributed.AsyncSessionTask#taskCompleted taskCompleted} instead.
   */
  public abstract void execute(Session session) throws SessionException;

  /**
   * Define the list of attributes required by a transaction. 
   * 
   * When you schedule an asynchronous get 
   * transaction on a remote session, a local copy of it, together with all its attributes is first brought 
   * back locally. This is expensive should your transaction access only some of the attributes. To avoid this 
   * unnecessary remote copy, overload this method so that it returns the list of attributes required by your 
   * transaction.
   */
  @SuppressWarnings("rawtypes")
  public List getAttributeWorkingSet() {
    return null;
  }

}
