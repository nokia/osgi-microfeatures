// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed;

import java.util.List;


/**
   Defines a session type.
   
   <p>Not all applications need the same session semantics. Take sharing and high-availability: one application may need sharing (any session accessed by any 
  agent), but not session high-availability. In contrast, another application may deal with sessions always created and accessed by the same agent, yet 
  with the requirement that in case that agent crashes, that session be re-instantiated in another agent. 
  
  <P>You can define the session semantics you need by defining a so-called <b>session type</b>.  
 
  Together with its type, a session is uniquely identified by a string <b>session identifier</b>, given by the application at creation time.   
  <b>A session is thus uniquely identified by the tuple session type/session identifier</b>.

  A session type defines the following set of properties:
  <ul>
  <li> session type name: its name {@link #TYPE_NAME},
  <li> session sharing: allows you to enable/disable session sharing {@link #TYPE_SHARED}.  
  <li> high-availability: allows you to enable/disable session high-availability {@link #TYPE_HA},
  <li> replication strategy: allows you to define the secondary selection strategy {@link #TYPE_STRATEGY},
  <li> session timeout: allows you to set the semantics you want for session duration {@link #TYPE_TIMEOUT_TYPE}.
  </ul>
*/
public interface SessionType {
  
  /**
   * The session type name
   */
  public final String TYPE_NAME = "name";
  
  /**
   * Set this property to true if the session must survive an agent crash
   */
  public final String TYPE_HA = "ha";
  
  /**
   * Set this property to true if the session can be accessed on any agent. 
   * If true, a session registry is needed.
   */
  public final String TYPE_SHARED = "shared";

  /**
   * @deprecated No more used since ASR-5.0
   * 
   * <p>Set this property to change the secondary selection strategy. Upon creating 
   * a new session, a secondary agent is chosen among the set of available peer agents 
   * using a round robin strategy. You can make this election take into account
   * the network topology of the cluster:
   * <ul>
   * <li>round-robin-far : the secondary is chosen among peer agents that are far, i.e. running on other sub-network.
   * <li>round-robin-near : the secondary is chosen among peer agents that are near, i.e. running on the same sub-network or on the local machine.
   * <li>round-robin-plain : peer agents are chosen randomly.
   * </ul>
   */
  public final String TYPE_STRATEGY = "replicationStrategy";

  /**
   * Set this to "inactivity" or to "absolute" do define the semantics you want to session duration.
   */
  public final String TYPE_TIMEOUT_TYPE = "sessionTimeoutType";

  /**
   * If set to true, the addSessionType method will NOT throw an IllegalArgumentException 
   * when the session type already exists. The existing session type will simply be returned. 
   */
  public final String TYPE_CREATE_GET = "createGet";
  
  /**
   * @deprecated
   * Set this to true to use the DHT
   */
  public final String TYPE_DHT = "dht";
  
  public final String TYPE_KEY_CACHE = "keyCache";
    
    /**
     * Returns the session type name.
     * @return the type name
     */
    public String getType();
  
    /**
     * @deprecated
     * @internal
     * Returns a session key. If you can let the container choose a session key for you, 
     * you will benefit from stateless session key handling. That is, the corresponding session will
     * not require an external session registry service. The session localization in the cluster
     * will be deduced directly from the session key. We strongly encourage you to use this features
     * both for performance and scalability.  
     * @see SessionType#createSmartKey(String)
     * @param userPart a userPart if you need to include in the key a suffix defined by your application
     * @return a session key. 
     */
    public String getSessionKey(String userPart);

    /**
     * @deprecated
     * @internal
     * Returns a session key. If you can let the container choose a session key for you, 
     * you will benefit from stateless session key handling. That is, the corresponding session will
     * not require an external session registry service. The session localization in the cluster
     * will be deduced directly from the session key. We strongly encourage you to use this features
     * both for performance and scalability.
     * @see SessionType#createSmartKey(String) 
     * @return a session key. 
     */
    public String getSessionKey();
    
    /**
     * Returns a smart session key. If you can let the container choose a session key for you, 
     * you will benefit from stateless session key handling. That is, the corresponding session will
     * not require an external session registry service. The session localization in the cluster
     * will be deduced directly from the session key. We strongly encourage you to use this features
     * both for performance and scalability.  
     * @param userPart a userPart if you need to include in the key a suffix defined by your application
     * @return a smart session key. 
     */
    public String createSmartKey(String userPart);

    /**
     * Returns a smart session key. If you can let the container choose a session key for you, 
     * you will benefit from stateless session key handling. That is, the corresponding session will
     * not require an external session registry service. The session localization in the cluster
     * will be deduced directly from the session key. We strongly encourage you to use this features
     * both for performance and scalability.  
     * @return a smart session key. 
     */
    public String createSmartKey();
    
    /**
     * Returns the user part of a stateless session key. 
     * @param key the key returned to you by getSessionKey
     * @return userPart the userPart you provided when calling createSmartKey, null if there is no userPart.
     * @throws SessionException if the key was not obtained by using createSmartKey.
     */
    public String getUserPart(String key) throws SessionException;
    
    /**
     * Tests whether the key is a stateless session key 
     * @param key the key of the session
     * @return true if the key is a stateless session key
     */
    public boolean isSmartKey(String key);
    
    /**
     * Get the agent identifiers stored in a stateless session key
     * @param key the stateless session key
     * @return an array of agent identifiers
     * @throws IllegalArgumentException if the key is not a stateless session key
     */
    public long[] getAgentIdsFromSmartKey(String key) throws IllegalArgumentException;
    
    /**
     * Get the session manager
     * @return the session manager
     */
    public SessionManager getSessionManager();
    
    /**
     * Get the number of sessions instantiated as master on the agent 
     * @return the number of sessions
     * @since ASR 5.0
     */
    public int getInstanceSessions();
    
    /**
     * Get the number of aliases to sessions instantiated as master on the agent
     * @return the number of aliases
     * @since ASR 5.0
     */
    public int getInstanceAliases();
    
    /**
     * Get the ids of sessions instantiated as master on the agent.
     * @return a list of session ids (as a snapshot, since sessions can be created/destroyed concurrently)
     * @since ASR 5.0
     */
    public List<String> getSessionIds();
   
}
