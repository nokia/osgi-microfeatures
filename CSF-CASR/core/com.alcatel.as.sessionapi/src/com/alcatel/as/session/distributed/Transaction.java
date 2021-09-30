package com.alcatel.as.session.distributed;
import java.io.Serializable;
import java.util.List;
/**
   <h3>Understanding Transactions</h3>

   <p/>Because sessions are shared objects potentially accessed concurrently by several JVMs, care must be taken with respect to synchronization, 
   and failure handling. This is where transactions will help you. 

   The session API makes you create, update and destroy sessions inside so-called ACID transaction. The term ACID historically refers to the basic properties 
  of a database transaction: <EM><B>Atomicity</EM></B>, <EM><B>Consistency</EM></B>, 
  <EM><B>Isolation</EM></B>, and <EM><B>Durability</EM></B>. 

  <p/><b>Atomicity</b> means that the entire sequence of actions in the transaction must be either completed or aborted. The transaction 
  cannot be partially successful. Combined with Isolation, Atomicity specifies that any transaction will view all actions taken by 
  any other transaction as having occurred all together, or atomically. In particular, should a JVM crashes while executing a transaction, 
  none of its partial effect will take place.

  <p/><b>Consistency</b> specifies that the transaction either creates a new and valid state of data in which all changes have been applied, or, 
  in case of failure, returns all data to the state existing before the transaction started.

  <p/><b>Isolation</b> specifies that actions taken within a transaction are not visible to any other transactions until the transaction 
  is committed.

  <p/><b>Durability</b> specifies that all changes made and committed successfully by transactions are permanent and must survive system failure. 
  For example, in the event of a failure and system restart, the data is available in the state existing after the last committed 
  transaction.

  <h3>Transaction API usage</h3>

 <P>If you are familiar with java transaction API (like JTA), the session API will look familiar to you. You'll find 
 the <CODE>commit()</CODE> and <CODE>rollback()</CODE> usual methods. There is a big difference though. Using a traditional transaction API you write something like:  
     <PRE><CODE>

    begin transaction
    ...
    update table-a
    ...
    if (condition-x)
        commit transaction
    else if (condition-y)
        update table-b
        commit transaction
    else
        rollback transaction
 <p>
.     </PRE></CODE>
 <P>That is, a transaction is a mere context of execution associated to the current thread, which implicitly glues several updates  
into one big atomic operation. In this example, <EM>table-a</EM> and <EM>table-b</EM> will be atomically updated. 
 Should <EM>table-a</EM> and <EM>table-b</EM> be managed by different underlying database systems, a two-phase commit message exchange 
would be required to make sure both updates are indeed performed.
 <P>Using the session API, you write something different. It looks like this:
     <PRE><CODE>

    begin transaction for session 'bob'
    ...
    update 'bob' attribute 'a'
    ...
    if (condition-x)
        commit 
    else if (condition-y)
        update 'bob' attribute 'b'
        commit 
    else
        rollback 
      </PRE></CODE>
 <P>As you can see, the difference is that a transaction is <B><EM>always associated to one and only one session object</B></EM>. 
A transaction never glues updates on more than one session object. It only glues update on several attributes on a given session.  
Because transaction never imply several sessions, no two-phase commit algorithm is required, ever.

<P>Why is this important ? Remember the session API targets IO-driven proxy applications, not traditional database applications 
such as banking applications. The session engine must provide a scalable and highly-available main memory distributed (session) database, 
exhibiting a very low access and update time. Distributed transactions with two phase commit do not fit such requirements. 

<P>Why transaction then ? Because it makes it a lot easier for you to write a robust application. Without transaction, you would need to
consider every potential failure, leaving your shared sessions in a partial inconsistent state. You should also deal with locking to 
prevent concurrent accesses. All this is possible, but difficult and error prone.  

<P>What is needed are light-weight transactions providing fast completion and state replication. This is exactly what the SessionAPI implements.  
Let's now have a look at a real piece of code. First, here is how you create a new session. In this example, it
is created with two attributes.
<CODE><PRE>
    // First you need to implement the Transaction interface. 
    public class MyTransaction extends Transaction {

        public MyTransaction(SessionType type, String sessionId, int flags) {
            super(sessionType, sessionId, flags);
        }

        // this is the transaction body. It is executed once the session has been created.
        public void execute(Session session) throws SessionException {
            // check if we are the creator or if the session existed already.
            if (session.created())
       	        session.setAttribute("a1", "initial value");
                session.setAttribute("a2", "initial value");
            }
    	    session.commit("done");
        }
    }

    ...
    // here is how to use this transaction using the SessionManager.execute method.
    // This method is synchronous. It blocks until the transaction has terminated.
    // The returned value is the value passed to the commit method.

    String result = (String) mgr.execute(new MyTransaction(type, "bob", Transaction.TX_CREATE_GET));
.     </PRE></CODE>
<P>As shown by this example, you always submit a Transaction for execution. The following example show the same transaction
executed asynchronously.
<CODE><PRE>
    // This is the listener upcalled once the transaction terminated.

    public class MyTransactionListener extends TransactionListener {

        // the transaction completed. The result parameters holds the value passed
        // to the commit method.
        public void transactionCompleted(Transaction tx, Serializable result) {
            ...
        }

        // the transaction failed. Typically your execute method has thrown an exception. 
        public void transactionFailed(Transaction tx, SessionException exc);
            ...
        }
    }
    ...
    // This method is asynchronous. It returns immediately. When the transaction will terminate, 
    // you'll be upcalled in your listener transactionCompleted method.
    mgr.execute(new MyTransaction(type, "bob", Transaction.TX_CREATE_GET), new MyTransactionListener());
    ...
</PRE></CODE>

   <h3>Advanced use</h3>

  <P>ACID properties are strong properties that come at some cost. Should several transactions access the same session 
  at the same time, only one at a time will run, while others are temporarily blocked. In practice, you can choose to execute transactions 
  under weaker guarantees (see {@linkplain com.alcatel.as.session.distributed.Transaction#TX_SERIALIZED TX_SERIALIZED} 
  versus {@linkplain com.alcatel.as.session.distributed.Transaction#TX_READ_COMMITTED TX_READ_COMMITTED} isolation levels).


 */
@SuppressWarnings("serial")
public abstract class Transaction implements Serializable {
  /**
   * 
   */
  private transient String      sessionId;
  private transient SessionType sessionType;
  private int                   flags;
  private String                conditionalEtag;
  private volatile transient Object assignedEtag;

  /**
   * Get the flags
   */
  public final int getFlags() {
    return flags;
  }
  /**
   * Get the session Id
   */
  public String getSessionId() {
    return sessionId;
  }
  /**
   * Get the session type
   */
  public SessionType getSessionType() {
    return sessionType;
  }

  /**
   * Execute the transaction on an new session. If the session already exists, the transaction is executed with a null 
   * session object.
   */
  public final static int TX_CREATE         =   1;

  /**
   * Execute the transaction on an existing session or a new session. If the session does not exist yet, a new one is created before executing the transaction.
   */
  public final static int TX_CREATE_GET     =   2;

  /**
   * Execute the transaction on an existing session. If the session does not exists, the transaction is executed with a 
   * null session object.
   */
  public final static int TX_GET            =   4;
  
  /**
   * Strongest transaction isolation level. This isolation level is fully ACID-compliant. The word "serializable" refers to ACID compliance, in which your transaction is 
   * deemed to have taken place in its entirety as if all other committed transactions have taken place in their entirety either 
   * before or after the transaction. In other words, the transactions are serialized.
   */
  public final static int TX_SERIALIZED     =   8;

  /**
   * @internal
   * Weaker isolation level. Your transaction can read changes made by other concurrent transactions that have been committed. 
   * Dirty reads are prevented with this transaction level, but non-repeatable reads and phantom reads can both occur. 
   * A transaction executed under this level may not destroy a session. 
   * (see {@link com.alcatel.as.session.distributed.Session#destroy}). 
   */
  public final static int TX_READ_COMMITTED =  16;

  /**
   * @internal
   * Weakest isolation level. Non-repeatable reads and phantom reads can both occur. 
   * A transaction executed under this level may not destroy a session. 
   * (see {@link com.alcatel.as.session.distributed.Session#destroy}). 
   */
  public final static int TX_DIRTY_READ     =  32;
  
  /**
   * This weaker isolation level specifies that a transaction can read only committed changes.
   * It cannot read data that has been modified but not committed by another transaction.
   * It cannot read data that has been modified and committed after the beginning of your transaction.
   * In other words, it means that you obtain something like a "snapshot" of a committed session. 
   * Other transaction can destroy the session, which results in phantom reads.
   */
  public final static int TX_READ_ONLY      =  64;

  /**
   * Request a remote transaction execution.
   */
  public final static int TX_REMOTE         =  128;

  /**
   * Request the transaction to execute locally. This is the default.
   */
  public final static int TX_LOCAL          =  256;

  /**
   * Indicates that the session etag must match the etag provided by the user of the transaction.
   * <br>This is the default behavior when a user etag is provided and {@link Transaction#TX_IF_NONE_MATCH} is not set.
   * <br>It takes precedence over {@link Transaction#TX_IF_NONE_MATCH} when both are set.
   */
  public final static int TX_IF_MATCH       =  512;
  
  /**
   * Indicates that the session etag must not match the etag provided by the user of the transaction.
   */
  public final static int TX_IF_NONE_MATCH  = 1024;
  
  /**
   * Indicates that the transaction will be used to in a iterator over all sessions. 
   * This flag used alone will iterate over all sessions master on the current node.
   * The session ID in this case must be a regular expression filter on session IDs (use ".*" for "all")
   * In addition to TX_REMOTE, iteration will span across the whole group.
   * If a destination group is specified by getDestinationGroup, iteration will occur over sessions of that group.
   */
  public final static int TX_ITERATOR  = 2048;
  
  /**
   * Indicates that the transaction if only interested by the metadata of the session, 
   * not by getting the data (attributes) of the session.
   * <p>This is useful for sending or setting the session attributes, 
   * when the transaction is running on a node where there is no local copy (neither master nor secondary) of the session:
   * <br>- only the metadata of the session are remotely copied.
   */
  public final static int TX_METADATA  = 4096;
  
  /**
   * @internal
   * The transaction will be executed only if the session has been locally marked by {@link SessionData#mark()}.
   */
  public final static int TX_IF_MARKED = 0x2000;

  /**
   * @internal
   * The transaction will be executed only if the session has not been locally marked. 
   * <p>See: {@link SessionData#mark()} and {@link SessionData#unmark()}
   */
  public final static int TX_IF_NOT_MARKED = 0x4000;

  /**
   * @internal
   * TODO expose this mode
   */
  public final static int TX_SEND_ONLY = 0x8000 | TX_METADATA | TX_READ_ONLY;
  
  /**
   * Usual constructor.
   * @param sessionType the session type
   * @param sessionId the session id
   * @param flags a combination of flags defining the transaction
   * <ul>
   * <li>One of {@link Transaction#TX_CREATE} or {@link Transaction#TX_GET} or {@link Transaction#TX_CREATE_GET} must be set,
   * <li>The default isolation level is {@link Transaction#TX_SERIALIZED},
   * <li>The default execution mode is {@link Transaction#TX_LOCAL}.
   * </ul>
   * <br>Example. Create a new session "bob":
   * <CODE><PRE>
   *   new Transaction(type, "bob", {@link Transaction#TX_CREATE} | {@link Transaction#TX_SERIALIZED} | {@link Transaction#TX_LOCAL});
   * is equivalent to:
   *   new Transaction(type, "bob", {@link Transaction#TX_CREATE});
   * <PRE><CODE>
   * @throws IllegalArgumentException when:
   * <ul>
   * <li>There is none of {@link Transaction#TX_CREATE} or {@link Transaction#TX_GET} or {@link Transaction#TX_CREATE_GET} flags,
   * <li>The combination {@link Transaction#TX_CREATE} and {@link Transaction#TX_REMOTE} is set.
   * </ul>
   */
  public Transaction(SessionType sessionType, String sessionId, int flags) {
    this.sessionId   = sessionId;
    this.sessionType = sessionType;
    this.flags       = flags;
    if ((flags&(TX_CREATE_GET|TX_GET|TX_CREATE|TX_ITERATOR)) == 0) {
      throw new IllegalArgumentException("flags need specify one of TX_CREATE_GET, TX_GET, TX_CREATE or TX_ITERATOR");
    }
    if ((flags&(TX_REMOTE | TX_CREATE)) == (TX_REMOTE | TX_CREATE)) {
      throw new IllegalArgumentException("flags: unauthorized combination");
    }
    if (sessionType == null) {
      throw new IllegalArgumentException("session type cannot be null when submitting transaction");
    }
    if ((flags&(TX_SERIALIZED|TX_READ_ONLY|TX_DIRTY_READ)) == 0) {
      this.flags |= TX_SERIALIZED;
    }
    if ((flags&(TX_REMOTE|TX_LOCAL)) == 0) {
      this.flags |= TX_LOCAL;
    }

  }
  
  /**
   * This constructor takes a conditional etag which must match or not match the session etag 
   * (according to the flags {@link Transaction#TX_IF_MATCH} and {@link Transaction#TX_IF_NONE_MATCH})
   * when the transaction will be executed by the {@link SessionManager}.
   * If the matching condition is not satisfied, the caller will get a {@link EtagException}.
   * @param sessionType the session type
   * @param sessionId the session id
   * @param flags a combination of flags defining the transaction
   * <ul>
   * <li>One of {@link Transaction#TX_CREATE} or {@link Transaction#TX_GET} or {@link Transaction#TX_CREATE_GET} must be set,
   * <li>The default isolation level is {@link Transaction#TX_SERIALIZED},
   * <li>The default execution mode is {@link Transaction#TX_LOCAL}.
   * </ul>
   * @param conditionalEtag the conditional etag known by the user of the transaction
   * @throws NullPointerException when the conditional etag is null
   * @throws IllegalArgumentException when:
   * <ul>
   * <li>There is none of {@link Transaction#TX_CREATE} or {@link Transaction#TX_GET} or {@link Transaction#TX_CREATE_GET} flags,
   * <li>The combination {@link Transaction#TX_CREATE} and {@link Transaction#TX_REMOTE} is set,
   * <li>The conditional etag is an empty string.
   * <ul>
   */
  public Transaction(SessionType sessionType, String sessionId, int flags, String conditionalEtag) {
    this(sessionType, sessionId, flags);
    if (conditionalEtag == null)
      throw new NullPointerException ("null etag");
    if (conditionalEtag.length() == 0)
      throw new IllegalArgumentException("empty etag");
    this.conditionalEtag = conditionalEtag;
  }  

  /**
   * Overload this method to give a hint to the session engine of which attributes will be
   * accessed by your transaction. 
   */
  @SuppressWarnings("rawtypes")
  public List getAttributeWorkingSet() {
    return null;
  }

  /**
   * Overload this method to specify in which group the session engine must look for session
   * <p>Null value or empty string means "local group"
   * @return the group name (default is null)
   */
  public String getDestinationGroup() {
    return null;
  }

  /**
   * Executes the transaction. Ultimately, you MUST end up executing {@linkplain com.alcatel.as.session.distributed.Session#destroy destroy}, 
   * {@linkplain  com.alcatel.as.session.distributed.Session#rollback rollback} or {@linkplain com.alcatel.as.session.distributed.Session#commit commit}
   * on the corresponding session.
   *
   * @param session the session this task is operating on.
   * <br>The session can be null when:
   * <ul>
   * <li>Using a Transaction of type {@link Transaction#TX_GET} and the session does not exist,
   * <li>Using a Transaction of type {@link Transaction#TX_CREATE} and the session already exists.
   * </ul>
   *  
   * @throws SessionException, if you need to abort the transaction due to an unexpected error, you may throw an exception, 
   * in which case the transaction will be automatically rolled back. Depending on the synchronous (resp asynchronous) API, 
   * you application will return an exception, (resp will be callbacked in 
   * {@linkplain com.alcatel.as.session.distributed.TransactionListener#transactionFailed transactionFailed}).
   * However, you might prefer terminating the transaction with the {@linkplain  com.alcatel.as.session.distributed.Session#rollback rollback} method, 
   * in which case you will return without error, (resp be callbacked in {@linkplain com.alcatel.as.session.distributed.TransactionListener#transactionCompleted transactionCompleted}).
   */
  public abstract void execute(Session session) throws SessionException;
  
  /**
   * @internal
   * Set the etag assigned to the session associated to this transaction.
   * This method is used by the {@link SessionManager}.
   * @param etag the etag of the session
   */
  public final void setAssignedEtag(Object etag)
  {
    this.assignedEtag = etag;
  }
  
  /**
   * Get the etag assigned to the session associated to this transaction.
   * @return the etag assigned to the session
   * <ul>
   * <li>returns null until the "execute" method is run,
   * <li>returns null if the session parameter of the "execute" method is null (no session is associated to this transaction).
   * </ul>
   */
  public String getAssignedEtag()
  {
    if (assignedEtag != null)
    {
      return assignedEtag.toString();
    }
    return null;
  }
  
  /**
   * Get the conditional etag given by the user of the transaction
   * @return the conditional etag given in the constructor
   */
  public final String getConditionalEtag()
  {
    return conditionalEtag;
  }
  
  @Override
  public String toString()
  {
    return "Transaction(" + sessionType + ",id=" + sessionId + ",flags=" + flags + ")";
  }
 
}
