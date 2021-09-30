
package com.alcatel.as.session.distributed;
import java.io.Serializable;

/**

  <h1>Quick Tour</h1>
  Roughly speaking, a session is a simple hash table like data structure, which you can use in JVMs to set and get attributes.
  
   <p/>
   Sessions have been specifically designed to let you write scalable and high-available clustered applications. 
   Such applications consist of a cluster of java JVMs running on several physical hosts.
   The session API semantics is closely tighted to this notion of cluster. In a nutshell, a session is an shared object, that can be created, 
   updated an destroyed by any cluster member. This is transparent to the application.   
   
  <p/>
  More specifically, sessions provide your application with the following fundamental properties:
    
  <ul>
  <li><b>high-availability</b>: sessions are replicated on two distinct JVMs. Should a JVM crash, its sessions will be re-instantiated on 
   a secondary JVM.
  <li><b>scaling</b>: sessions are evenly spread among JVMs. The scaling of sessions is obtained by adding new JVMs.
  <li><b>sharing</b>: a session can be transparently accessed from any JVM, i.e. a session created on one JVM can be accessed and updated from another jvm in the cluster.
  <li><b>ACID transactions</b>: session are manipulated using the well-known ACID transactional paradigms. This dramatically simplifies your application with respect to synchronization and failures. Please refer to  {@linkplain com.alcatel.as.session.distributed.Transaction Transaction}.  
  <li><b>notifications</b>: using the session listener API, the application can be designed using a event-driven pattern. Listeners are notified upon various session events (creation, destruction, updates).  
  <li><b>asynchronous or synchronous programming model</b>: the session API includes a synchronous API (one thread per transaction) and a asynchronous API (one thread for all transactions). Both fits differents applications needs.   
  </ul>
  
  <P>In the following we explain in further detail the transaction and high-availability concepts. Please refer to {@linkplain com.alcatel.as.session.distributed.event.SessionListener SessionListener} for a description of session listeners. 

  <h2>Writing a highly available application</h2>
  
  <P>Let us now consider the issue of JVM failure. Remember your application is executed in a cluster of several 
   jvms. What then happens when a JVM crashes ?

  <P>For simplicity, let us assume that the JVM created just one session 'bob'. Just like all sessions, 'bob' is backed up on a <EM>secondary</EM> JVM. 
   Upon detecting the crash, the secondary JVM will simply reactivate 'bob'.  Your application will not notice the crash.
        
   <P>What happens if another JVM had a listener on 'bob' ? Nothing. The listener will keep be notified by the secondary JVM.

   <P>What happens if another JVM was in the middle of executing a transaction on 'bob' ? Nothing. It will keep executing that transaction on
   the secondary. 

    <P>This makes life a lot easier for you, because in most cases, your application does not want to deal explicitly 
    with jvm failures. 
    <P>In the case you need to know when a session is reactivated on a secondary jvm, and take 
    some action on your own, the session API lets you do that with {@linkplain com.alcatel.as.session.distributed.event.SessionActivationListener SessionActivationListener}.  
    
  
*/
public interface Session extends SessionData {
    
  /**
   * @deprecated
   * Please refer to the {@linkplain com.alcatel.as.session.distributed.Transaction Transaction} class.
   */
  public final static int TRANSACTION_NONE            =  0;

  /**
   * @deprecated
   * Please refer to the {@linkplain com.alcatel.as.session.distributed.Transaction Transaction} class.
   */
  public final static int TRANSACTION_SERIALIZED      =  1;

  /**
   * @deprecated
   * Please refer to the {@linkplain com.alcatel.as.session.distributed.Transaction Transaction} class.
   */
  public final static int TRANSACTION_READ_COMMITTED  =  2;

  /**
   * @internal
   * @deprecated
   */
  public void setTransactionIsolation (int isolationMode) throws SessionException;

  /**
   * @internal
   *@deprecated
   */
  public void setAutoCommit ();

  /**
   * This method is deprecated, and supported only for backward compatibility.
   * Commit all the attribute changes performed since the last begin. Listeners, if any, will be notified upon the corresponding changes.  
   * @deprecated
   * @throws SessionException if the transaction has been rollbacked.
   */
  public void commit () throws SessionException;

  /**
   * Terminate a transaction by committing all the attribute changes, if any. You must execute this method even though your transaction
   * only read session attributes. Listeners, if any, will be notified upon the potential changes.  
   * @param result the return value as returned by the {@linkplain com.alcatel.as.session.distributed.SessionManager#execute execute} methods.
   */
  public void commit (Serializable result);

  /**
   * Destroy the session. This operation must be executed from a transaction.  Listeners, if any, will be notified upon the destruction.  
   * Only transaction executed under the {@linkplain com.alcatel.as.session.distributed.Transaction#TX_SERIALIZED  TX_SERIALIZED} isolation are allowed to do this.
   * @param result a return value, possibly null,  that will be returned to your application (see the {@linkplain com.alcatel.as.session.distributed.SessionManager#execute execute} methods).
   */
  public void destroy (Serializable result);

  /**
   * Destroy the session. This operation must be executed from a transaction.  Listeners, if any, will be notified upon the destruction.  
   * Only transaction executed under the {@linkplain com.alcatel.as.session.distributed.Transaction#TX_SERIALIZED  TX_SERIALIZED} isolation are allowed to do this.
   * @param result a return value, possibly null,  that will be returned to your application (see the {@linkplain com.alcatel.as.session.distributed.SessionManager#execute execute} methods).
   * @param send when true: send the attributes set by {@linkplain SessionData#sendAttribute(String, Serializable)}. Listeners, if any, will be notified in the same notification than the destruction.
   */
  public void destroy (Serializable result, boolean send);

  /**
   * Rollback all the changes performed in a transaction.   
   * @param result a return value, possibly null, that will be returned to your application (see the {@linkplain com.alcatel.as.session.distributed.SessionManager#execute execute} methods).
   */
  public void rollback (Serializable result);

  /**
   *@deprecated
   * This method is supported only for backward compatibility.
   * Rollback all the attribute changes performed since the last begin. Note that listeners, if any, will not be notified.  
   * @throws SessionException if a problem occurs.
   */
  public void rollback () throws SessionException;

  /**
   *@deprecated
   * This method is supported only for backward compatibility.
   * Destroys the session. 
   */
  public void destroy () throws SessionException;

  /**
   * This class wraps a Session Attribute (key/value).
   */
  public static class Attribute implements Serializable {

    /**
     * A session attribute defined by:
     * <ul>
     * <li>Its name,</li>
     * <li>Its value,</li>
     * <li>and optionally by a timeout value</li>
     * </ul>
     */
    
    private static final long serialVersionUID = 1L;
    
    private String name;
    private Serializable value;
    private int timeout;
	
    /**
     * Constructor.
     * @param name the attribute name
     * @param value the attribute value
     */
    public Attribute (String name, Serializable value){
      this.name = name;
      this.value = value;
      this.timeout = -1;
    }
	
    /**
     * Constructor.
     * @param name the attribute name
     * @param value the attribute value
     * @param timeout the inactivity delay before timeout
     */
   public Attribute(String name, Serializable value, int timeout)
    {
      super();
      this.name = name;
      this.value = value;
      this.timeout = timeout;
    }

    /**
     * Returns the Attribute name.
     * @return the name
     */
    public String getName (){
      return name;
    }

    /**
     * Returns the Attribute value.
     * @return the value
     */   
    @SuppressWarnings("unchecked")
    public <T extends Serializable>T getValue (){
      return (T) value;
    }

    /**
     * Returns the inactivity delay before timeout.
     * @return the timeout value
     */
    public int getTimeout()
    {
      return timeout;
    }

    public String toString (){
      if (timeout > 0)
        return "Session.Attribute[name="+name+", value="+value+", timeout="+timeout+"]";
      return "Session.Attribute[name="+name+", value="+value+"]";
    }
  }
}
			 
