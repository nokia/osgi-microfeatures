package com.alcatel.as.session.distributed;

import java.util.List;

/**
 * @deprecated
 * @internal
 * Asynchronous callbacks used with asynchronous get and create operations. 
 */
public abstract class SessionTask {
  /**
   * Callback used to notify the task issuer once the task has completed.
   */
  public interface Callback {
    /**
     * A task has completed.
     * @param type The session type,
     * @param sessionId the session id,
     * @param task The completed task.
     */
    void taskCompleted(SessionType type, String sessionId, SessionTask task);

    /**
     * A task has failed.
     * @param type The session type,
     * @param sessionId the session id,
     * @param task The failed task.
     * @param error The error cause.
     */
    void taskFailed(SessionType type, String sessionId, SessionTask task, SessionException error);
  }

  /** The session has to be committed (returned by the execute() method. */
  public final static int COMMIT = 1;

  /** The session has to be rollbacked (returned by the execute() method). */
  public final static int ROLLBACK = 2;

  /** The session has to be destroyed (returned by the execute() method). */
  public final static int DESTROY = 3;

  /** Use this if the session was not found or could not be created. */
  public final static int IGNORE  = 4;

  /**
   * Executes the transaction. This method MUST return one of the defined code. 
   *
   * @param session the session this task is operating on. 
   *  
   * @return COMMIT if the session has to be committed, ROLLBACK if the session 
   *	     has to be rollbacked, or DESTROY if the session has to be destroyed.
   * @throws SessionException on any exception.
   */
  public abstract int execute(SessionData session) throws SessionException;

  /**
   * @deprecated
   * @internal
   * Use this method to give a hint to the Session Manager of which attributes will be
   * accessed by your transaction. 
   */
  @SuppressWarnings("rawtypes")
  public List getAttributeWorkingSet() {
    return null;
  }

}
