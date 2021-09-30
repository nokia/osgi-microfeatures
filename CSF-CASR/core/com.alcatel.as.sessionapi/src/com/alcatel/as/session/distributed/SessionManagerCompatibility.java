package com.alcatel.as.session.distributed;

import java.util.List;

/**
 * @deprecated
 * @internal
 * Support for the old style session API. These APIs are now deprecated, refer to the new transaction based API.
 */

public abstract class SessionManagerCompatibility {
  /**
   * This method is supported only for backward compatibility.
   * @deprecated
   * @internal
   * Creates or retrieves a Session, as part of a new transaction. <B><EM>This method is blocking, use with care.</EM> </B>
   * <p/>The returned Session depends on the value of the get parameter:
   * <ul>
   * <li>if get is set to true : the Session is created or retrieved. The two cases can be identified by calling created() on the returned Session. 
   * A null value is never returned.
   * <li>if get is set to false : the Session can only be created. Null is returned if the Session already exists.
   * </ul>
   * <p>Note that when the Session is retrieved (and not created), the provided 'duration' parameter is ignored, but the 'isolationLevel' 
   * parameter is used.
   * Once you get a session, it is part of a transaction with the requested isolation level 
   * (see {@link com.alcatel.as.session.distributed.Session#TRANSACTION_SERIALIZED}, 
   * {@link com.alcatel.as.session.distributed.Session#TRANSACTION_READ_COMMITTED}).
   *
   * <p>Once done creating a session, you must terminate the transaction using one of {@link com.alcatel.as.session.distributed.Session#commit}, {@link com.alcatel.as.session.distributed.Session#rollback} or {@link com.alcatel.as.session.distributed.Session#destroy}).
   * If you terminate the transaction using {@link com.alcatel.as.session.distributed.Session#rollback} or 
   * {@link com.alcatel.as.session.distributed.Session#destroy}, the session is not created at all, i.e. no listener will be notified.
   * If you terminate the transaction using {@link com.alcatel.as.session.distributed.Session#commit}, the session will atomically be 
   * created with all the attributes set in the transaction.  
   *
   * @param isolationLevel the transaction isolation level.  
   * @param type the session type.  
   * @param sessionId the session identifier.  
   * @param duration  the session duration in seconds, -1 for infinite.   
   * @param get true to retrieve the session if it already exists.   
   * @return the Session (may be null, see above) 
   * @throws InvalidArgumentException if one of the argument is incorrect 
   */
  public abstract Session createSession(int isolationLevel, SessionType type, String sessionId, int duration, boolean get) 
    throws SessionException;
  /**
   * This method is supported only for backward compatibility.
   * @deprecated
   *
   * @param isolationLevel the transaction isolation level.  
   * @param type the session type.  
   * @param sessionId the session identifier.  
   * @param duration  the session duration in seconds, -1 for infinite.   
   * @param get true to retrieve the session if it already exists.   
   * @param task a task scheduled once the session has been created.
   * @param callback The task issued called once the task has completed.
   */
  public abstract void createSession(int isolationLevel, SessionType type, String sessionId, int duration, boolean get,
				     SessionTask task, SessionTask.Callback callback);

 
  /**
   * This method is supported only for backward compatibility.
   * @deprecated
   *
   * Get a session, as part of a new transaction. <B><EM>This method is blocking, use with care.</EM> </B>
   *
   * <p>Once you get a session, it is part of a transaction with the requested isolation level 
   * (see {@link com.alcatel.as.session.distributed.Session#TRANSACTION_SERIALIZED}, 
createSession(SessionType type, String sessionId, boolean getIfExist, int duration, Transaction tx);   * {@link com.alcatel.as.session.distributed.Session#TRANSACTION_READ_COMMITTED}). 
   *
   * Once done working on the session, you must terminate your transaction 
   * (see {@link com.alcatel.as.session.distributed.Session#commit}, {@link com.alcatel.as.session.distributed.Session#rollback},
   * {@link com.alcatel.as.session.distributed.Session#destroy}), even though your transaction only get attributes.   
   *
   * @param isolationLevel the transaction isolation level * 
   * @param type the session type.  
   * @param sessionId the session identifier.  
   * @return the Session or null if it does not exists. 
   * @throws InvalidArgumentException if one of the argument is incorrect 
   * @throws OperationTimeoutException if the operation blocks for more than the timeout associated to this session type  (see {@link SessionType}). 
   */
  public abstract Session getSession(int isolationLevel, SessionType type, String sessionId) 
    throws SessionException;

  /**
   * This method is supported only for backward compatibility.
   * @deprecated
   * @param isolationLevel the transaction isolation level.  
   * @param type the session type.  
   * @param sessionId the session identifier.  
   * @param task a task scheduled once the session has been retrieved.
   * @param callback The task issued called once the task has completed.
   */
  public abstract void getSession(int isolationLevel, SessionType type, String sessionId,
				  SessionTask task, SessionTask.Callback callback);

 
  /**
   * Retrieves all the Sessions of a given type.
   * @param type the Session type
   * @return a list of Session Objects
   * @throws SessionException if a problem occurs
   */
  public abstract List<Session> getSessions(SessionType type) 
    throws SessionException;
  
  /**
   * Returns the number of Sessions of a given type.
   * @param type the Session type
   * @return the number of Sessions
   * @throws SessionException if a problem occurs
   */
  public abstract int getSessionsSize(SessionType type) 
    throws SessionException;

  /**
   * Return the duration left for a session. This operation is not transactional and returns
   * only an approximate value. 
   * @param type the session type
   * @param sessionId the session identifier
   * @return the duration left for the corresponding session or 0 if the session does not exists or has already expired. 
   */
  public abstract int getDurationLeft(SessionType type, String sessionId);
}
