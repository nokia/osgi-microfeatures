// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.event;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.alcatel.as.session.distributed.SessionData;


/**
 * This class defines a filter for Session events.
 * <p/>
 * A SessionEventFilter may be associated to a per session, global or per session type listener.
 */
public class SessionEventFilter
{

  /**
   * The default name for my group
   */
  public static final String MY_GROUP = "";
  
  /**
   * @internal
   * @deprecated
   * The session prefix that accepts all the Sessions. <br/>
   * Its values is "".
   */
  public static final String ACCEPT_ALL_SESSIONS = "";
  /**
   * The session attribute prefix that accepts all the session attributes. 
   * <p>Its values is:</p>
   * <pre>
   *    <code>new String[] {""}</code>.
   * </pre>
   */
  public static final String[] ACCEPT_ALL_ATTRIBUTES = new String[] { "" };

  /**
   * The event representing a session created.
   */
  public static final int EVENT_SESSION_CREATED = 1;
  /**
   * The event representing a session destroyed.
   */
  public static final int EVENT_SESSION_DESTROYED = 2;
  /**
   * The event representing a session expired.
   */
  public static final int EVENT_SESSION_EXPIRED = 4;
  /**
   * <p>
   * This event is raised when a session is activated after the crash of the agent holding the
   * master session copy. It tells your application the corresponding session will be considered
   * master in this JVM, and give your application a chance to reactivate some code, timer,
   * data, etc ...
   * <p>
   * It is only delivered to the agent hosting the secondary session before becoming master.
   * <p>
   * This event is only relevant using per session type listeners, and should be used instead of
   * the ActivationListener.
   */
  public static final int EVENT_SESSION_ACTIVATED = 8192;

  /**
   * This event is raised when all the connections with the agents hosting the session have been
   * lost. It tells your application the corresponding session will be considered lost.
   * <p>
   * This event is only relevant using per session listeners.
   * 
   * @since ASR 3.4 [IMSAS0FAG250408]
   */
  public static final int EVENT_SESSION_LOST = 16384;
  
  /**
   * This event is raised when the activation of the sessions begins.
   * <p>
   * This event is only relevant using per session type listeners.
   */
  public static final int EVENT_SESSION_ACTIVATION_BEGIN = 2 << 16;
    
  /**
   * This event is raised when the activation of the sessions is ended.
   * <p>
   * This event is only relevant using per session type listeners.
   */
  public static final int EVENT_SESSION_ACTIVATION_END = 2 << 17;

  /**
   * This event is raised when the session is secured again (replicated).
   * <p>
   * This event is only relevant using per session listeners.
   */
  public static final int EVENT_SESSION_SECURED = 2 << 18;

  /**
   * This event is raised when the session is not secured (not replicated).
   * <p>
   * This event is only relevant using per session listeners.
   */
  public static final int EVENT_SESSION_UNSECURED = 2 << 19;

  /**
   * Events are raised when the the agent is the master copy of the session.
   */
  public static final int EVENT_IF_MASTER = 2 << 20;

  /**
   * The event representing all the session events.
   */
  public static final int EVENT_SESSION_ALL =
      EVENT_SESSION_CREATED | EVENT_SESSION_DESTROYED | EVENT_SESSION_EXPIRED
          | EVENT_SESSION_ACTIVATED | EVENT_SESSION_LOST
          | EVENT_SESSION_ACTIVATION_BEGIN | EVENT_SESSION_ACTIVATION_END
          | EVENT_SESSION_SECURED | EVENT_SESSION_UNSECURED;

  /**
   * The event representing an attribute added.
   * <p>
   * This event is raised when a new attribute is set.
   * </p>
   * @see SessionData#setAttribute(String, java.io.Serializable) 
   */
  public static final int EVENT_ATTRIBUTE_ADDED = 16;
  /**
   * The event representing an attribute removed.
   * <p>
   * This event is raised when an existing attribute is removed.
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_REMOVED = 32;
  /**
   * The event representing an attribute updated.
   * <p>
   * This event is raised when an existing attribute is set.
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_UPDATED = 64;
  /**
   * The event representing an attribute when the session is destroyed.
   * <p>
   * This event is raised for each existing attribute.
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_DESTROYED = 128;

  /**
   * The event representing an attribute and can occur in two cases:
   * <ul>
   * <li>When the session is expired: this event is raised for each existing attribute.</li>
   * <li>Individually for an attribute having a timeout: this event is raised when there is no activity on this attribute</li>
   * </ul> 
   * <p>
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_EXPIRED = 256;
  
  /**
   * The event representing an attribute when the session is activated.
   * <p>
   * This event is raised for each existing attribute.
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_ACTIVATED = 2 << 15;
  
  /**
   * @internal
   * TODO expose it
   * The event representing an attribute that is not modified.
   * <p>
   * This event is raised when this attribute is member of a working set,
   * and when another member of this working set is changed.
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_UNMODIFIED = 2 << 14;
  

  private static final int EVENT_ATTRIBUTE_REPLICATED = 512;

  /**
   * The event representing an attribute sent.
   * <p>
   * This event is raised for each sent attribute.
   * </p>
   */
  public static final int EVENT_ATTRIBUTE_SENT = 1024;

  /**
   * The event representing all the attribute events.
   */
  public static final int EVENT_ATTRIBUTE_ALL = EVENT_ATTRIBUTE_SENT 
    | EVENT_ATTRIBUTE_ADDED | EVENT_ATTRIBUTE_REMOVED| EVENT_ATTRIBUTE_UPDATED | EVENT_ATTRIBUTE_UNMODIFIED
    | EVENT_ATTRIBUTE_DESTROYED | EVENT_ATTRIBUTE_EXPIRED | EVENT_ATTRIBUTE_ACTIVATED;

  /**
   * This event is signaled when one or more listeners have been explicitly removed from the
   * session.
   * <p>
   * You can check the number of listeners attached to a session using the
   * Session.getListenersSize method.
   * <p>
   * This event is only relevant using per session type listeners.
   */
  public static final int EVENT_LISTENERS_REMOVED = 2048;

  /**
   * This event is signaled when one or more listeners have been lost because the agent hosting
   * the listener crashed.
   * <p>
   * You can check the number of listeners attached to a session using the
   * Session.getListenersSize method.
   * <p>
   * This event is only relevant using per session type listeners.
   * <p>
   * This event is possibly returned together with the EVENT_SESSION_ACTIVATED.
   */
  public static final int EVENT_LISTENERS_LOST = 4096;

  /**
   * The event representing all the listener events.
   */
  public static final int EVENT_LISTENERS_ALL = EVENT_LISTENERS_REMOVED | EVENT_LISTENERS_LOST;

  /**
   * The event representing all the session, attribute and listener events.
   */
  public static final int EVENT_ALL = EVENT_SESSION_ALL | EVENT_ATTRIBUTE_ALL | EVENT_LISTENERS_ALL;

  /**
   * @internal
   * This flag indicates that you are interested by the events of a group.
   * <br>It is only significant for the session-type listeners.
   */
  public static final int EVENT_RESERVED_2= 1 << 28;
  
  /**
   * @internal
   * Reserved event for internal use
   * @since ASR 3.4 [IMSAS0FAG250408]
   */
  public static final int EVENT_RESERVED_1 = 1 << 30;

  private String destinationGroup = MY_GROUP;
  private String[] attributes;
  private int events;
  private List<String> workingSet;
  private Pattern pattern;

  /**
   * Constructor to listen to specific subset of attributes. 
   * <br>Give the array of attributes you want to listen to.
   * 
   * @param attributes the prefixes for attribute names, null is equal to ACCEPT_ALL_ATTRIBUTES
   * @param events the events to listen for.
   */
  public SessionEventFilter(String[] attributes, int events)
  {
    if (attributes == null)
      attributes = ACCEPT_ALL_ATTRIBUTES;
    this.attributes = attributes;
    this.events = events;
  }

  /**
   * @internal
   * Constructor to listen to specific subset of attributes. 
   * <br>Give the array of attributes you want to listen to.
   * <br>Give a working set of attributes for which you want to be notified in the same callback, even if some of them have not changed.
   * <br>The working set is only used by session-type listeners.
   * 
   * @param attributes the prefixes for attribute names, null is equal to ACCEPT_ALL_ATTRIBUTES
   * @param events the events to listen for.
   * @param workingSet an array of attribute names.
   */
  public SessionEventFilter(String[] attributes, int events, String[] workingSet)
  {
    this(attributes, events);
    this.workingSet = Arrays.asList(workingSet);
  }

  /**
   * Constructor to listen to specific subset of attributes. 
   * <br>Give the array of attributes and the group name you want to listen to.
   * <br>The group name is only used by session-type listeners.
   * 
   * <p>
   * Attention: 
   * <br>Using this kind of filter can consume a lot of resources of the cluster (CPU load, band-with, memory), 
   * depending on the number of nodes and of session activity, and is potentially dangerous.
   * 
   * @param attributes the prefixes for attribute names, null is equal to ACCEPT_ALL_ATTRIBUTES
   * @param events the events to listen for.
   * @param destinationGroup the destination group name listened for (empty String means "my group").
   */
  public SessionEventFilter(String[] attributes, int events, String destinationGroup)
  {
    this(attributes, events | EVENT_RESERVED_2);
    if (destinationGroup == null)
      throw new NullPointerException ("null destination group");
    this.destinationGroup = destinationGroup;
  }

  /**
   * @internal
   * Constructor to listen to specific subset of attributes. 
   * <br>Give the array of attributes and the group name you want to listen to.
   * <br>Give a working set of attributes for which you want to be notified in the same callback, even if some of them have not changed.
   * <br>The group name and the working set are only used by session-type listeners.
   * 
   * <p>
   * Attention: 
   * <br>Using this kind of filter can consume a lot of resources of the cluster (CPU load, band-with, memory), 
   * depending on the number of nodes and of session activity, and is potentially dangerous.
   * 
   * @param attributes the prefixes for attribute names, null is equal to ACCEPT_ALL_ATTRIBUTES
   * @param events the events to listen for.
   * @param destinationGroup the destination group name listened for (empty String means "my group").
   * @param workingSet an array of attribute names.
   */
  public SessionEventFilter(String[] attributes, int events, String destinationGroup, String[] workingSet)
  {
    this(attributes, events, destinationGroup);
    this.workingSet = Arrays.asList(workingSet);
  }

  /**
   * Constructor with an events mask.
   * 
   * @param events the events to listen for.
   */
  public SessionEventFilter(int events)
  {
    this.attributes = ACCEPT_ALL_ATTRIBUTES;
    this.events = events;
  }

  /**
   * @internal
   * Constructor with an events mask.
   * <br>Give a working set of attributes for which you want to be notified in the same callback, even if some of them have not changed.
   * <br>The working set is only used by session-type listeners.
   * 
   * @param events the events to listen for.
   * @param workingSet an array of attribute names.
   */
  public SessionEventFilter(int events, String[] workingSet)
  {
    this(events);
    this.workingSet = Arrays.asList(workingSet);
  }

  /**
   * Constructor with an events mask and and a destination group name listened for.
   * <br>The group name is only used by session-type listeners.
   * 
   * <p>
   * Attention: 
   * <br>Using this kind of filter can consume a lot of resources of the cluster (CPU load, band-with, memory), 
   * depending on the number of nodes and of session activity, and is potentially dangerous.
   * 
   * @param events the events to listen for.
   * @param destinationGroup the destination group name listened for (empty String means "my group").
   */
  public SessionEventFilter(int events, String destinationGroup)
  {
    this(events | EVENT_RESERVED_2);
    if (destinationGroup == null)
      throw new NullPointerException ("null destination group");
    this.destinationGroup = destinationGroup;
  }

  /**
   * @internal
   * Constructor with an events mask and and a destination group name listened for.
   * <br>Give a working set of attributes for which you want to be notified in the same callback, even if some of them have not changed.
   * <br>The group name and the working set are only used by session-type listeners.
   * 
   * <p>
   * Attention: 
   * <br>Using this kind of filter can consume a lot of resources of the cluster (CPU load, band-with, memory), 
   * depending on the number of nodes and of session activity, and is potentially dangerous.
   * 
   * @param events the events to listen for.
   * @param destinationGroup the destination group name listened for (empty String means "my group").
   */
  public SessionEventFilter(int events, String destinationGroup, String[] workingSet)
  {
    this(events, destinationGroup);
    this.workingSet = Arrays.asList(workingSet);
  }

  /**
   * @internal
   * Constructor.
   * 
   * @param attributes the prefixes for attribute names, null is equal to ACCEPT_ALL_ATTRIBUTES
   * @deprecated use SessionEventFilter(String, int)
   */
  public SessionEventFilter(String sessions, String[] attributes, int events)
  {
    if (attributes == null)
      attributes = ACCEPT_ALL_ATTRIBUTES;
    this.attributes = attributes;
    this.events = events;
  }

  /**
   * Constructor to listen to attributes whose name matches a regular expression 
   * 
   * @param attributeRegex a regular expression for attribute names that you want to listen for,
   * @param events the events to listen for.
   * @throws PatternSyntaxException
   */
  public SessionEventFilter(String attributeRegex, int events) throws PatternSyntaxException
  {
    this.attributes = ACCEPT_ALL_ATTRIBUTES;
    this.events = events;
    this.pattern = Pattern.compile(attributeRegex);
  }

  /**
   * Constructor to listen to attributes whose name matches a regular expression and for a destination group
   * <br>The group name and the working set are only used by session-type listeners.
   * 
   * <p>
   * Attention: 
   * <br>Using this kind of filter can consume a lot of resources of the cluster (CPU load, band-with, memory), 
   * depending on the number of nodes and of session activity, and is potentially dangerous.

   * @param attributeRegex a regular expression for attribute names that you want to listen for,
   * @param events the events to listen for.
   * @param destinationGroup the destination group name listened for (empty String means "my group").
   * @throws PatternSyntaxException
   */
  public SessionEventFilter(String attributeRegex, int events, String destinationGroup) throws PatternSyntaxException
  {
    this(attributeRegex, events | EVENT_RESERVED_2);
    if (destinationGroup == null)
      throw new NullPointerException ("null destination group");
    this.destinationGroup = destinationGroup;
  }
 
  /**
   * @internal
   * @deprecated
   * Returns the prefix for sessionIds.
   * 
   * @return the prefix for sessionIds, possibly null.
   */
  public String getSessionFilter()
  {
    return null;
  }

  /**
   * Returns the prefix for attribute names.
   * 
   * @return the prefix for attribute names, possibly null.
   */
  public String[] getSessionAttributeFilter()
  {
    return attributes;
  }

  /**
   * Returns the events to listen for.
   * 
   * @return the events.
   */
  public int getEventFilter()
  {
    return events;
  }
  
  /**
   * Get the destination group name listened for.
   * <br>This is only used by session-type listeners.
   * @return the group name
   */
  public final String getDestinationGroup()
  {
    return destinationGroup;
  }

  /**
   * @internal
   * TODO expose it
   * Get the working set of attributes expected in the same callback
   * @return the working set of attributes expected in the same callback
   */
  public List<String> getWorkingSet()
  {
    return workingSet;
  }
  
  /**
   * @internal
   */
  public Pattern getAttributePattern() {
    return this.pattern;
  }

  public String toString()
  {
    return toString(events);
  }

  public static String toString(int events)
  {
    if (events != 0)
    {
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < 31; i++)
      {
        int mask = 1 << i;
        switch (events & mask) {
        case 0:
          break;
        case EVENT_SESSION_CREATED:
          buf.append("addSes");
          buf.append("|");
          break;
        case EVENT_SESSION_ACTIVATED:
          buf.append("actSes");
          buf.append("|");
          break;
        case EVENT_SESSION_DESTROYED:
          buf.append("delSes");
          buf.append("|");
          break;
        case EVENT_SESSION_EXPIRED:
          buf.append("expSes");
          buf.append("|");
          break;
        case EVENT_SESSION_LOST:
          buf.append("lostSes");
          buf.append("|");
          break;
        case EVENT_SESSION_ACTIVATION_BEGIN:
          buf.append("actBegin");
          buf.append("|");
          break;
        case EVENT_SESSION_ACTIVATION_END:
          buf.append("actEnd");
          buf.append("|");
          break;
        case EVENT_SESSION_SECURED:
          buf.append("secured");
          buf.append("|");
          break;
        case EVENT_SESSION_UNSECURED:
          buf.append("unsecured");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_ADDED:
          buf.append("addAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_DESTROYED:
          buf.append("dstAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_EXPIRED:
          buf.append("expAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_ACTIVATED:
          buf.append("actAttr");
          buf.append("|");
          break;          
        case EVENT_ATTRIBUTE_REMOVED:
          buf.append("delAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_REPLICATED:
          buf.append("replAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_SENT:
          buf.append("sentAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_UPDATED:
          buf.append("updAttr");
          buf.append("|");
          break;
        case EVENT_ATTRIBUTE_UNMODIFIED:
          buf.append("unmoAttr");
          buf.append("|");
          break;
        case EVENT_LISTENERS_LOST:
          buf.append("lostLstnr");
          buf.append("|");
          break;
        case EVENT_LISTENERS_REMOVED:
          buf.append("delLstnr");
          buf.append("|");
          break;
        case EVENT_RESERVED_2:
          buf.append("GROUP");
          buf.append("|");
          break;
        default:
          buf.append(events & mask);
          buf.append("|");
          break;
        }
      }
      return buf.toString();
    }
    else {
      return "empty";
    }
  }

}
