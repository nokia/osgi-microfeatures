// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.io.Serializable;

import com.alcatel.as.session.distributed.Session.Attribute;
import com.alcatel.as.session.distributed.event.SessionEventFilter;
import com.alcatel.as.session.distributed.event.SessionListener;

/**
 * SessionData interface gives you access to the session attributes.
 * Some attributes are predefined and have a special semantics. 
 * <dl>
 * <dt>{@link #ATTR_DURATION}</dd>
 * <dd>Holds the session duration in seconds. You can change the session duration dynamically by setting this attribute to a int value.</dd>
 * <dt>{@link #ATTR_CREATION_TIME}</dd>
 * <dd>Holds the session creation time. You cannot change it.</dd>
 * </dl>
 */
public interface SessionData {

  /**
   * @deprecated
   * Name  of the attribute holding the session duration. 
   * @see SessionData#getDuration()
   * @see SessionData#setDuration(int)
   */
  public final static String ATTR_DURATION = "_duration";

  /**
   * @deprecated
   * Name  of the attribute holding the session creation time.
   * @see SessionData#getCreationTime()
   */
  public final static String ATTR_CREATION_TIME = "_creationTime";

  /**
   * Returns the session type.
   * @return the type
   */
  public SessionType getSessionType ();

  /**
   * Returns the sessionId. This operation can be performed inside or outside transaction boundaries. 
   * That is, it succeeds event though the session has been destroyed or has expired. 
   * @return the sessionId
   */
  public String getSessionId ();

  /**
   * Make a string representing the sessionId as a smart key. 
   * <ul>
   * <li>The returned value has a syntax easily readable by the load-balancers.
   * <li>The returned value is valid until the master or the secondary does not change.
   * </ul>
   * @return a string representing the sessionId as a smart key
   * <ul>
   * <li>If the sessionId is a smart key, the sessionId is simply returned.
   * </ul>
   */
  public String makeSmartKey();

  /**
   * Make a string representing the sessionId as a smart key appended with a unique identifier
   * across the cluster.
   * <ul>
   * <li>The returned value has a syntax easily readable by the load-balancers.
   * <li>The returned value is valid until the master or the secondary does not change.
   * </ul>
   * @return a string representing the sessionId as a smart key,
   * even if the sessionId is already a smart key.
   * </ul>
   */
  public String makeUniqueSmartKey();

  /**
   * Return the current list of aliases. 
   * @return a possibly empty list of session keys.
   */
  public String[] getAliases ();

  /**
   * Return the session size. This returns an estimate, although precise, of 
   * the size in bytes of the session content.  
   * Basically it it the result of taking the sum of all session attributes size (together with
   * their name). Internal attributes used internally by the session implementation are also 
   * taken into account.
   *
   * Note that only committed attributes are taken into account. You will not see the size
   * effect of the current transaction changes.  
   * @return the size in bytes of the session content. 
   */
  public int getSize ();

  /**
   * Add aliases to a session. This works only with regular session keys, you cannot 
   * add an alias to a session named with a smart key.
   * @throws exception for any refused value, or if the session use a smart key as primary key.
   */
  public void setAliases (String... aliases) throws SessionException;

  /**
   * Remove aliases to a session. This will only work if the aliases you give were already set
   * by a previous transaction.
   * @throws exception for any refused value, or if the session use a smart key as primary key.
   */
  public void removeAliases (String... aliases) throws SessionException;

  /**
   * Returns the creation time.
   * @return the creation time
   * @throws SessionException if a problem occurs
   */    
  public long getCreationTime () throws SessionException;

  /**
   * Returns the session duration as specified when created.<br>
   * Executing getDuration is equivalent to execute getAttribute({@link #ATTR_DURATION}).
   * @return the duration in seconds, -1 for infinite, 0 if the session is expired
   * @throws SessionException if a problem occurs
   */
  public int getDuration () throws SessionException;

  /**
   * Set the session duration as specified when created, in seconds.<br>
   * Executing setDuration is equivalent to execute setAttribute({@link #ATTR_DURATION}, duration).
   * @param duration
   * @throws SessionException if a problem occurs
   */
  public void setDuration(int duration) throws SessionException;

  /**
   * Get the value of the etag of the session.
   * <br>This returns the value when entering into the transaction, whatever happens on the session.
   * <br>In other words, if the session is modified and then committed, 
   * the new value of the etag can be obtained by using {@linkplain Transaction#getAssignedEtag()}.
   * @return the etag value 
   * @throws SessionException if a problem occurs
   */
  public String getEtag() throws SessionException;

  /**
   * Touch the session. This will force the modification of the etag even if no attribute is set.
   * @throws SessionException if a problem occurs
   */
  public void touch() throws SessionException;

  /**
   * @internal
   * Mark the local copy of the session whatever is the termination of the transaction.
   * <p>It has no effect if it is not the master or the secondary copy.
   * <p>See {@link Transaction#TX_IF_MARKED} and {@link SessionData#unmark()} for the usage of marked sessions.
   */
  public void mark() throws SessionException;

  /**
   * @internal
   * Unmark the local copy of the session whatever is the termination of the transaction.
   * <p>It has no effect if it is not the master or the secondary copy.
   * <p>See {@link Transaction#TX_IF_MARKED} and {@link SessionData#unmark()} for the usage of marked sessions.
   */
  public void unmark() throws SessionException;

  /**
   * @deprecated
   * @internal
   * <p>This method does nothing. If you want to change the session duration, use setAttribute on the
   * "_duration" attribute.
   */
  public void keepAlive (int value) throws SessionException;

  /**
   * Tells if this session has been created by the current transaction.
   */
  public boolean created();

  /**
   * Send an attribute.
   * <br>The listeners interested by the event {@link SessionEventFilter#EVENT_ATTRIBUTE_SENT} will be notified.
   * <br>The attribute is not stored into the session.
   * <br>If the attribute was previously set by {@link SessionData#setSessionAttribute(Attribute)}, the timer is canceled
   * @param name the attribute name
   * @param value the attribute value
   * @throws SessionException if a problem occurs. 
   */
  public void sendAttribute (String name, Serializable value) throws SessionException;

  /**
   * Sets an attribute.
   * @param name the attribute name
   * @param value the attribute value
   * @return always true. This return code is obsolete, you may safely ignore it.
   * @throws InvalidArgumentException name or value is null, or the value is not a valid Serializable.
   * @throws SessionException if a problem occurs. 
   */
  public boolean setAttribute (String name, Serializable value) throws SessionException;

  /**
   * Retrieves an attribute.
   * @param name the attribute name
   * @return null if the attribute is not set in the session
   * <br/>the attribute if the session is alive and the attribute is set.
   * @throws SessionException if a problem occurs
   */
  public <T extends Serializable>T getAttribute (String name) throws SessionException;

  /**
   * Retrieves an attribute using a particular class loader.
   * @param name the attribute name
   * @param classLoader the class loader for deserializing the attribute
   * @return null if the attribute is not set in the session
   * <br/>the attribute if the session is alive and the attribute is set.
   * @throws SessionException if a problem occurs
   */
  public <T extends Serializable>T getAttribute (String name, ClassLoader classLoader) throws SessionException;

  /**
   * Sets an attribute. 
   * <p>This attribute may have an expiration delay. 
   * <br>In this case, an event {@link SessionEventFilter#EVENT_ATTRIBUTE_EXPIRED} is raised when the timeout occurs.
   * <br>The timer can be reset by using {@link SessionData#touchSessionAttribute(String)}.
   * <br>The timer can be cancelled by setting a timeout value lower or equal to zero.
   * @param attribute the attribute to set
   * @throws SessionException if the attribute value is null or if a problem occurs. 
   */
  public void setSessionAttribute(Attribute attribute) throws SessionException;

  /**
   * Gets an attribute.
   * @param name the attribute name
   * @return the attribute or null if the attribute does not exist).
   * @throws SessionException if a problem occurs
   */
  public Attribute getSessionAttribute(String name) throws SessionException;

  /**
   * Gets an attribute.
   * @param name the attribute name
   * @param classLoader the class loader for deserializing the attribute
   * @return the attribute or null if the attribute does not exist).
   * @throws SessionException if a problem occurs
   */
  public Attribute getSessionAttribute(String name, ClassLoader classLoader) throws SessionException;

  /**
   * Removes an attribute.
   * @param name the attribute name
   * @return the attribute or null if the attribute does not exist).
   * @throws SessionException if a problem occurs
   */
  public Attribute removeSessionAttribute(String name) throws SessionException;

  /**
   * Removes an attribute.
   * @param name the attribute name
   * @param classLoader the class loader for deserializing the attribute
   * @return the attribute or null if the attribute does not exist).
   * @throws SessionException if a problem occurs
   */
  public Attribute removeSessionAttribute(String name, ClassLoader classLoader) throws SessionException;

  /**
   * Touches an attribute.
   * <br/>Re-arm the timer of the attribute for the timeout value (if the timeout value is greater than 0)
   * @param name the attribute name
   * @throws SessionException if a problem occurs
   */
  public void touchSessionAttribute(String name) throws SessionException;

  /**
   * Updates an attribute.
   * <br/>Update the attribute without re-arming the timer
   * @param name the attribute name   
   * @param value the attribute value
   * @throws SessionException if the attribute value is null or if a problem occurs. 
   */
  public void updateSessionAttribute(String name, Serializable value) throws SessionException;

  /**
   * Retrieves the attributes keys.
   * @return a set of keys, possibly empty
   * @throws SessionException if executed outside a transaction
   */
  public Set<String> keySet() throws SessionException;  

  /**
   * Retrieves all the attributes.
   * <br/>The returned Enumeration contains all the attributes as Attribute Objects at the time of its call.
   * @return an Enumeration (possibly empty)
   * @throws SessionException if a problem occurs
   */
  public List<Attribute> getAttributes () throws SessionException;

  /**
   * Retrieves all the attributes.
   * <br/>The returned Enumeration contains all the attributes as Attribute Objects at the time of its call.
   * @param loaders the classLoaders for unmarshaling the attribute
   * @return an Enumeration (possibly empty)
   * @throws SessionException if a problem occurs
   */
  public List<Attribute> getAttributes (ClassLoader ... loaders) throws SessionException;

  /**
   * @deprecated
   * @internal
   * Retrieves all the attributes.
   * <br/>The returned Map contains all the attributes. The attributes names are the keys.
   * <p/>This method loads all the attributes in memory and facilitates attributes reading. 
   * Note that writing to the map has no effect on the Session.
   * @return a Map (possibly empty) or null if the session is destroyed
   * @throws SessionException if a problem occurs
   */

  @SuppressWarnings("rawtypes")
  public Map getAttributesMap () throws SessionException;

  /**
   * Retrieves the number of committed attributes.
   * @return the number of committed attributes.
   * @throws SessionException if you execute this method outside transaction boundaries
   */
  public int getAttributesSize () throws SessionException;

  /**
   * Retrieves the current number of attributes.
   * @return the current number of attributes.
   * @throws SessionException if you execute this method outside transaction boundaries
   */
  public int getCurrentAttributesSize () throws SessionException;

  /**
   * Retrieves the number of listeners attached to this session. Only the  number of per-session listeners are returned.
   * Session type listeners and activation listeners are not considered.
   * @return the number of listeners attached to this session.
   * @throws SessionException if you execute this method outside transaction boundaries
   */
  public int getListenersSize () throws SessionException;;

  /**
   * Removes an attribute.
   * @param name the attribute name
   * @param get indicate if the attribute value must be returned
   * @return the attribute or null the attribute does not exist or if the 'get' parameter is false.
   * @throws SessionException if you execute this method outside transaction boundaries
   */
  public <T extends Serializable>T removeAttribute (String name, boolean get) throws SessionException;

  /**
   * Removes an attribute.
   * @param name the attribute name
   * @param classLoader the class loader for deserializing the attribute
   * @return the attribute or null the attribute does not exist.
   * @throws SessionException if you execute this method outside transaction boundaries
   */
  public <T extends Serializable>T removeAttribute (String name, ClassLoader classLoader) throws SessionException;

  /**
   * Removes all the attributes.
   * @return returns true if at last one attribute is removed
   * @throws SessionException if you execute this method outside transaction boundaries
   */
  public boolean removeAttributes () throws SessionException;
  
  /**
       Add a per-session listener to a session. The listener may also implement 
       {@linkplain com.alcatel.as.session.distributed.event#SessionAttributeListener SessionAttributeListener}.
       The listener is automatically removed when the session is destroyed, but may be removed before using 
       {@linkplain #removeSessionListener removeSessionListener}.


       <P>The following code sample shows how create a session and adds a listener to it session. 
       This is interesting because the listener is de facto located in the same agent than the session (see {@link com.alcatel.as.session.distributed.event.SessionListener}).
       <CODE><PRE>
       public class MyTransaction extends Transaction {
       public MyTransaction(SessionType type, String sessionId) {
       super(sessionType, sessionId, TX_SERIALIZED|TX_CREATE_GET);
       }
       }
       public void execute(Session session) throws SessionException {
       if (session.created()) {
       // I want to be notified only for attributes updates. This is the corresponding filter I need.
       SessionEventFilter filter = new SessionEventFilter(null, SessionEventFilter.EVENT_ATTRIBUTE_UPDATED);
       session.addSessionListener(null, new MyListenerClass(), filter);
       }
       session.commit(null);
       }
       }
       ...
       mgr.execute(new MyTransaction(type, "bob"));
       </PRE></CODE>

       @param listener the listener to add
       @param filter the filter to use to discriminate events, possibly null (no discrimination)
       @return the id assigned to the listener (this id is used to remove the listener).
       @throws SessionNotAliveException if the session has expired or has been deleted. 
       @throws SessionException if another problem occurs
   */
  public int addSessionListener (SessionListener listener, SessionEventFilter filter) throws SessionException;

  /**
   * Removes a SessionListener.
   * @throws SessionNotAliveException if the session has expired or has been deleted. 
   * @param id the id returned when the SessionListener was added
   */
  public abstract void removeSessionListener (int id) throws SessionException;

  /**
   * Get a SessionListener
   * @param id
   * @return the SessionListener or null if there is no local listener on this session for the given id
   * @throws SessionException if called outside of a transaction
   */
  public SessionListener getListener(int id) throws SessionException;
  
  /**
   * Is the the master copy of the session local to this JVM?
   * @return true if the master copy of the session is local to this JVM.
   * @throws SessionException if called outside of a transaction
   */
  public boolean isMaster() throws SessionException;

}

