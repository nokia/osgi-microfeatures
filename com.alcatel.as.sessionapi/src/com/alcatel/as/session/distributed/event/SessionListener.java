// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.event;

import java.util.List;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionManager;

/**
  Using 
  <ul>
  <li>{@linkplain SessionManager#addSessionListener(com.alcatel.as.session.distributed.SessionType, String, SessionListener, SessionEventFilter)} 
  <li>or {@linkplain Session#addSessionListener(SessionListener, SessionEventFilter)} 
  <li>or {@linkplain SessionManager#addSessionTypeListener(com.alcatel.as.session.distributed.SessionType, SessionListener, SessionEventFilter)} 
  </ul>
  your application can be notified 
  upon a session creation, update or destruction. 
  <p>Notifications are performed on a per-transaction basis. 
  <br>For example, if a transaction update several times the same attribute, 
  the listener will be notified once with the resulting final value for that attribute. 
  <p>The session API guarantees your application with a very strong at-least-once/at-most-once semantics, even in case of agent failure.   
  <p>The following code sample shows how a listener is added to an existing session. The <code>eventHandler</code> user callback is called every time a transaction 
  update or destroy the session. 
  <div style="width:700px;">
  <p>The listener:
  <code><pre>
  public class MyListener implements SessionListener
  {

  &#64;Override
  public void handleEvent(List<SessionEvent> events)
  {
    // First, get the 2-tuple session-identifier/session-type 
    // that uniquely identifies the session on which we are notified. 
    // Since all events in the list will return the same tuple, we use the first event. 
    String sessionId = events.get(0).getSessionId();
    SessionType type = events.get(0).getSessionType();
    
    // then, handle each event
    for (SessionEvent event : events) {
      switch (event.getEventType()) {
      case SessionEventFilter.EVENT_SESSION_CREATED:
        System.out.println("session " + sessionId + " created");
        break;
        
      case SessionEventFilter.EVENT_SESSION_DESTROYED:
        System.out.println("session " + sessionId + " destroyed");
        break;
      
      case SessionEventFilter.EVENT_ATTRIBUTE_ADDED:
        AttributeEvent attrEvent = (AttributeEvent) event;
        Attribute attr = attrEvent.getAttribute();
        System.out.println("session " + sessionId + ",attribute " + attr + " added");
        break;

      default:
        break;
      }
    }

  }
  </pre></code>
<p>And the code adding the listener:
<code><pre>
  mySessionManager.execute(new Transaction(mySessionType,  "bob", Transaction.TX_GET | Transaction.TX_SERIALIZED) {
    private static final long serialVersionUID = 1L;

    &#64;Override
    public void execute(Session session) throws SessionException
    {
      session.addSessionListener(new MyListener(), filter);
      session.commit(null);
    }
    
  });
</pre></code>
</div>

  <p>
  Although the principle is simple, be careful to be comfortable with the transactional, sharing and high availability features of the session API. 
  <p>
  <hr>
  The first key issue is that your listeners are always notified <EM>outside</EM> transaction boundaries. This means for example that:
  <ul>
  <li> if you are notified upon a session creation, it means the sessions has indeed been created. But it may have been destroyed since. 
  <li> if you are notified upon an attribute update, it means one transaction which updated that attribute with that value did commit. But maybe a subsequent transaction already updated further that attribute (in which case the notification for that later transaction is in the pipe).  That is, if once notified, you need to know the actual session state, you must explicitely access the session, hence entering a new transaction.   
  <li> if you are notified upon a session destruction, it means the sessions has indeed been destroyed. But it may have been recreated since. 
  </ul>
  <hr>
  <p>The second issue is that your listener is in no way highly-available. A listener is nothing but a piece of code running in one agent. We call that agent the
  listener <EM>home agent</EM>. If an agent crashes, all its listener are clearly lost. 

  <hr>
  <p>A third issue is the difference between <B><EM>global listeners</EM></B> versus <B><EM>per-session listeners</EM></B> versus <B><EM>session-type listeners</EM></B>

  <p><B><EM>global listeners</EM></B>

 <p><EM>Global</EM> listeners are created using <CODE>SessionManager.addSessionListener(..)</CODE>. They can be created independently of 
an existing session. Use this, for instance, to be notified upon a given session
 creation. I.e., "notify me when session 'bob' will be created". Registering a new global listener is <EM>not</EM> a 
transactional operation, you execute it outside a transaction, you need not commit it. 
  <p>
  A global listener is registered only in the agent where the <CODE>SessionManager.addSessionListener(..)</CODE> has been executed. 
  However, it will be notified wherever the session is actually created, updated or destroyed. 
  Remember session distribution is completely transparent to the application. 
  <p>
  When is a global listener removed then ? Either after you explicitly remove it using 
  {@linkplain com.alcatel.as.session.distributed.SessionManager#removeSessionListener SessionManager.removeSessionListener(..)} 
  or if the agent where the listener has been added crashes or quits. 

  <p><B><EM>per-session listeners</EM></B>

  <p><EM>Per-session</EM> listeners are always added to an existing session. 
  Adding a listener to a session is just like adding an attribute, it must be executed as part of a transaction, and will take effect only if you commit 
  that transaction. However, unlike an attribute, the listener will be removed from the session, should the listener home agent crash.
  <p>
  Keep in mind that a per-session listener is registered and running only in the agent where you added the listener. Should that agent crash, 
  the listener is removed from the session. Listeners are in no ways "highly-available", only sessions are.
  <p> 
  If you have the choice, always prefer per-session listeners, preferably added in the same transaction used to create the session. 
  The listener will then be de facto registered on the agent holding the session, thus reducing traffic required for remote notification. 
  Should that agent crash, you may use the {@linkplain com.alcatel.as.session.distributed.event#SessionActivationListener SessionActivationListener}) to replug a similar listener on the agent where the session will be re-instanciated.  
  
  <p><B><EM>session-type listeners</EM></B>
  <p><EM>session-type</EM> listeners is a third kind of listener having the ability to listen all the sessions of a given type.
  <p>Most of the time, you need only one listener per session-type.
  <p>The listener can listen only the local agent or whole the agents of a group.

  <hr>
  <p>A fourth issue is to know which events are generated for which kind of listeners</p>
  <table border="1">
  <tr>
  <td>Event</td><td><B><EM>global listeners</EM></B></td><td><B><EM>per-session listeners</EM></B></td><td><B><EM>session-type listeners</EM></B></td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_CREATED}      </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_DESTROYED}    </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_EXPIRED}      </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_LOST}         </td><td>N</td><td>Y</td><td>N</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_ACTIVATED}    </td><td>N</td><td>N</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_ACTIVATION_BEGIN} </td><td>N</td><td>N</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_ACTIVATION_END}   </td><td>N</td><td>N</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_SECURED}      </td><td>Y</td><td>Y</td><td>N</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_SESSION_UNSECURED}    </td><td>Y</td><td>Y</td><td>N</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_ATTRIBUTE_ADDED}      </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_ATTRIBUTE_UPDATED}    </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_ATTRIBUTE_SENT}       </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_ATTRIBUTE_REMOVED}    </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_ATTRIBUTE_DESTROYED}  </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_ATTRIBUTE_EXPIRED}    </td><td>Y</td><td>Y</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_LISTENERS_REMOVED}    </td><td>N</td><td>N</td><td>Y</td>
  </tr>
  <tr>
  <td>{@linkplain SessionEventFilter#EVENT_LISTENERS_LOST}       </td><td>N</td><td>N</td><td>Y</td>
  </tr>
  </table>
 */
public interface SessionListener {

  /**
   * Invoked when the listener is notified 
   * @param events list of events
   */
  public void handleEvent(List<SessionEvent> events);

}
