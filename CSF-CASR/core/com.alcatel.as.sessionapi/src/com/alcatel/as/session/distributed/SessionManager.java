package com.alcatel.as.session.distributed;

import java.io.Serializable;
import java.util.Dictionary;

import com.alcatel.as.session.distributed.event.GroupListener;
import com.alcatel.as.session.distributed.event.SessionActivationListener;
import com.alcatel.as.session.distributed.event.SessionEventFilter;
import com.alcatel.as.session.distributed.event.SessionListener;
import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
  The Manager to access the Sessions. This class allows you to create and get sessions, and to add listeners.
  
  The manager can be retrieved by 2 ways:
  <ul>
  <li>The best practice: Injected by the OSGi framework when starting your application.
  <p>Example using the declarative service with annotations (OSGi 4.2):</p>
  <code><pre>
    &#64;Reference
    protected void setSessionManager(SessionManager manager)
    {
      this.manager = manager;
    }
  </pre></code>
  <li>Legacy: getting a instance is retrieved via {@linkplain SessionManager#getSessionManager() getSessionManager}.
  </ul>
  
  <P>Session are shared data object with a cluster-wide visibility. Session scaling is supported by adding new JVMs to the cluster.
     The session API completely hides session distribution. You write your application just as if all sessions were local to your JVM. The session
     objects will be automatically fetched or remotely updated whenever needed. 
   
   A session is created, updated or accessed through a set of operations grouped inside a <em>transaction</em>. When you submit a transaction for execution, the session 
   API lets you choose among two different strategies:  
  <ul>
  <li><b>local transactions</b>: the transaction code is executed in the JVM where you submit it. If the session is remote, then a copy will be fetched locally first. 
  <li><b>remote transactions</b>: the transaction code is executed on the JVM where the session is hosted. 
  </ul>
   <P>We recommend the remote transaction mode, because it does not incur remote session locking. However, it puts some additional
      constraint to your transaction which must be executable from any JVM from the cluster. For example, you must be careful when using static objects, avoid
      using per-thread context data, and so on.
       
   <P>In most cases your application must not and need not differentiate <EM>local sessions</EM> 
   (the ones homed by the agent executing you code now) from <EM>remote sessions</EM> (the ones homed by other agents). 
   
   <P>There are however two functionalities that need to access only local sessions. The first is dealing with the set of all sessions, 
   and the second is to be notified for all session creations. 
 
 <h3>Accessing all sessions</h3>
 
  Suppose your application needs to access the set of all current sessions. The session API provides 
  a <CODE>getSessions</CODE> method. This method could naively return to you a list of all sessions.
  
  <P>
  However, this is likely to make the calling agent crashes because 
  <EM><B>the number of sessions in the cluster is too large to be contained in a single agent</B></EM>.
  Even worst, since all agents run the same code, it is likely that <EM><B>all agents</B></EM> execute
  <CODE>getSessions</CODE> at the same time. Hence, the whole cluster would explode !  
 
  <P>That's why <CODE>getSessions</CODE> returns only the set of local sessions. This allows you, for instance, to periodically 
  access all sessions in the system, as far as your code is executed in all agents. 
 
  <P>
  Here is a concrete example. Suppose sessions are used to represent currently connected http users. You can use <CODE>getSessions</CODE>
  to periodically check the profile of all connected users. Each agent will access its local sessions, and (say) will deduce some 
  aggregate information ("1200 users whose age is lower than 18 year are connected right now"). If you must then aggregate further the result
  for the whole cluster, it will be up to your application to use a dedicated shared data structure to hold it. 
  <P>An easy solution would be to use a dedicated session (not of the same type that the ones used to represent http users). 
  That a session would be accessed  and updated by all agents, using the well known transaction guarantees. 
 
 <h3>Be notified on for all session event</h3>

 <P>
  Suppose your application need be notified each time a session of a given type is created. The   
  {@linkplain SessionManager#addSessionListener(SessionType, String, SessionListener, SessionEventFilter, AddSessionListenerCallback)} lets you do that.

  <P>A naive implementation would invoke your listener every time a session of the corresponding type is created in the entire cluster.
  Here again, it would clearly overwhelm the listener with a storm of notifications, making the listener home agent crash. 
  Even worst, since all agents run the same code, it is likely that <EM><B>all agents</B></EM> execute
  <CODE>addSessionListener</CODE>. Again, the whole cluster would explode ! This makes no sense.

 <P>However, it makes perfect sense to notify such a listener every time a session is created in the same agent. 
   If your application ensures a listener is created in each agent, at the end you will be notified every time a session is created, wherever it is created.

   <P>This kind of notification makes it easy to react to an event coming from one protocol ("I received a SMPP message") and 
   take some action in another protocol ("I must then initiate some SIP exchange").

 */
@SuppressWarnings("deprecation")
public abstract class SessionManager extends SessionManagerCompatibility {

  @Deprecated
  public final int VERSION_MAJ = 4;
  @Deprecated
  public final int VERSION_MIN = 0;
  
  /**
   * @deprecated versioning is achieved by OSGi versions of exported packages
   * @internal
   * @return
   */
  public String version() {
    return VERSION_MAJ + "." + VERSION_MIN;
  }

  /**
   * Submit a transaction for asynchronous execution.
   * @param tx the transaction
   * @param listener the transaction termination callback.
   */
  public abstract void execute(Transaction tx, TransactionListener listener);
  
  /**
   * Submit a transaction for synchronous execution.
   * @param cmd the transaction
   * @return the result of your transaction as returned using 
   * {@linkplain com.alcatel.as.session.distributed.Session#commit commit}, 
   * {@linkplain com.alcatel.as.session.distributed.Session#destroy destroy}, 
   * {@linkplain com.alcatel.as.session.distributed.Session#rollback rollback}, 
   * @throws SessionException if a problem occurs. 
   */
  public abstract <T extends Serializable>T execute(Transaction cmd) throws SessionException;
 
  /**
   * Returns the static SessionManager.
   * <p>Note that method is no more recommended since the SesionManager is an OSGi service. 
   * A good practice is to be injected with the OSGi service.
   * @return the SessionManager
   */
  public static SessionManager getSessionManager(){
      return ServiceLoader.getService(SessionManager.class);
  } 

  /**
   * Adds a session listener to a session type.
   * <P>The listener will be notified of any event involving a session of the given type. 
   * <p>Depending of the {@link SessionEventFilter}, this only applies for <EM>local</EM> sessions or for all the sessions of a group. 
   * If it applies only for <EM>local</EM> sessions, this method should be executed in all cluster nodes to ensure all events are notified.
   * 
   * @param type the Session type
   * @param listener the listener to add
   * @param filter the filter to use to discriminate events, possibly null (no discrimination)
   * @return the id assigned to the listener (this id is used to remove the listener)
   * @throws SessionException if a problem occurs
   */
  public abstract int addSessionTypeListener(SessionType type, SessionListener listener, SessionEventFilter filter)
    throws SessionException;

  /**
   * Removes a session listener from a session type.
   * @param listener the listener to remove
   */
  public abstract void removeSessionTypeListener(SessionType type, SessionListener listener);


  /**
   * Removes a global session listener from this agent.
   * @param id the id returned when the global listener was added
   */
  public abstract void removeSessionListener(int id);
  
  /**
   * Returns a SessionType.
   * @param type the type
   * @return the SessionType Object that wraps the specified type
   */
  public abstract SessionType getSessionType(String type);

  /**
   * Adds an activation listener to a an agent.
   * <br/>The listener will be notified of any activation or passivation for sessions of a given type.
   * @deprecated replaced by {@link #addSessionTypeListener} with a {@link SessionEventFilter#EVENT_SESSION_ACTIVATED}
   * @param type the session type
   * @param listener the listener to add
   * @return the id assigned to the listener (this id is used to remove the listener)
   * @throws SessionException if a problem occurs
   */
  public abstract int addActivationListener(SessionType type, SessionActivationListener listener) throws SessionException;
    
  /**
   * Removes an activation listener from this agent.
   * @param id the id returned when the activation listener was added
   */
  public abstract void removeActivationListener(int id);

  /**
  * Interface used to notify a completed addSessionListener operation.
  */
  public interface AddSessionListenerCallback {
      void addSessionListenerCompleted(SessionType type, String sessionId, SessionListener listener, int listenerId);
      void addSessionListenerFailed(SessionType type, String sessionId, SessionListener listener, SessionException error);
  }
  
  /**
   * Adds a listener to a given session asynchronously.
     <P>The listener will be notified of any event involving the specified session. This method does not require
     the session to exist already. 
     If the filter argument includes the session creation event, and if the session already exists, 
     the listener will be directly invoked. Once the listener is registered in the cluster, your are notified through your callback. 
    
    
     This kind of listener is powerful but costly. Whenever possible, prefer per session listeners 
     ({@linkplain com.alcatel.as.session.distributed.Session#addSessionListener Session.addSessionListener}),
     or session type listeners ({@linkplain com.alcatel.as.session.distributed.SessionManager#addSessionTypeListener addSessionTypeListener}).

     @param type the Session type
     @param sessionId the Session Id.
     @param listener the listener to add
     @param filter the filter to use to discriminate events, possibly null (no discrimination)
     @param callback The client callback called once the operation has completed.
   */
  public abstract void addSessionListener(SessionType type, String sessionId, SessionListener listener, SessionEventFilter filter,
					  AddSessionListenerCallback callback);
  
  /**
     Adds a listener to a given session identifier. <B><EM>This method is blocking, use with care.</EM></B>
     <P>The listener will be notified of any event involving the specified session. This method does not require
     the session to exist already. 
     If the filter argument includes the session creation event, and if the session already exists, 
     the listener will be directly invoked. 
    
     This kind of listener is powerful but costly. Whenever possible, prefer per session listeners 
     ({@linkplain com.alcatel.as.session.distributed.Session#addSessionListener Session.addSessionListener}),
     or session type listeners ({@linkplain com.alcatel.as.session.distributed.SessionManager#addSessionTypeListener addSessionTypeListener}).
     
     @param type the Session type
     @param sessionId the Session Id.
     @param listener the listener to add
     @param filter the filter to use to discriminate events, possibly null (no discrimination)
     @return the id assigned to the listener (this id is used to remove the listener)
     @throws SessionException if a problem occurs
  */
  public abstract int addSessionListener(SessionType type, String sessionId, SessionListener listener, SessionEventFilter filter) 
    throws SessionException;
  
  /**
   * Add a new session type defined by properties:
   * <ul>
   * <li>name [M] : The session type name {@link SessionType#TYPE_NAME} 
   * <li>ha [O default=true] : Set this property to true if the session must survive 
   * an agent crash {@link SessionType#TYPE_HA}
   * <li>shared [O default=false|ha] : Set this property to true if the session 
   * can be accessed on any agent. If true, a session registry is needed {@link SessionType#TYPE_SHARED}
   * <li>replicationStrategy [O default="round-robin-far"]: {@link SessionType#TYPE_STRATEGY}
   * <p>Set this property to change the secondary selection strategy. Upon creating 
   * a new session, a secondary agent is chosen among the set of available peer agents 
   * using a round robin strategy. You can make this election take into account
   * the network topology of the cluster:
   * <ul>
   * <li>round-robin-far : the secondary is chosen among peer agents that are far, i.e. running on other sub-network.
   * <li>round-robin-near : the secondary is chosen among peer agents that are near, i.e. running on the same sub-network or on the local machine.
   * <li>round-robin-plain : peer agents are chosen randomly.
   * </ul>
   * <li>sessionTimeoutType [O default="inactivity"] : Set this to "inactivity" or to "absolute"
   * do define the semantics you want to session duration {@link SessionType#TYPE_TIMEOUT_TYPE}
   * <li>dht [O default=true] : Set this to true to use the DHT {@link SessionType#TYPE_DHT} 
   * </ul>
   * @param properties description of the session type 
   * @return the session type
   * @throws IllegalArgumentException if properties are null or does not contain a name, or if the session type already exists (unless the TYPE_CREATE_GET option is set)
   */
  public abstract SessionType addSessionType(Dictionary<String, String> properties) throws IllegalArgumentException;

  /**
   * Add a session type and register an activation listener atomically
   * @see #addSessionType
   * @param properties description of the session type 
   * @param listener the session listener
   * @param filter the event filter
   * @return the session type
   * @throws IllegalArgumentException if properties are null or does not contain a name, or if the session type already exists (unless the TYPE_CREATE_GET option is set)
   */
  public abstract SessionType addSessionType(Dictionary<String, String> properties,
                                             SessionListener listener,
                                             SessionEventFilter filter) throws IllegalArgumentException;

  public abstract SessionType addSessionType(Dictionary<String, String> properties,
                                             SessionListener listener,
                                             SessionEventFilter filter,
                                             GroupListener groupListener) throws IllegalArgumentException;

  /**
   * Get the name of the local group where this manager is deployed
   * @return the name of the local group
   */
  public abstract String getLocalGroupName();
  
  /**
   * Get the identities of a smartkey contained in a string
   * @param value the string containing the smartkey
   * @return 
   * <p>null if the string does not contain a smartkey.</p>
   * <p>an array of the identities:</p>
   * <ul>
   *   <li>The first one is the platform identity</li>
   *   <li>The second one is the group identity</li>
   *   <li>The third one is the container identity</li>
   *   <li>Then are the agent identities</li>
   * </ul>
   * @since API 4.8.1 (previously, this method was returning an array of longs)
   */
  public abstract String[] getIdsFromSmartKey(String value);
}
