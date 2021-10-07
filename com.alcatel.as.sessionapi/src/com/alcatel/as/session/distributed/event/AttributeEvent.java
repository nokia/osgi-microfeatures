package com.alcatel.as.session.distributed.event;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionType;

/**
 * This class defines an event on a session attribute.
 */

public class AttributeEvent extends SessionEvent
{
  private Session.Attribute attribute;

  /**
   * Get the session attribute
   * @return the attribute
   */
  public final Session.Attribute getAttribute()
  {
    return attribute;
  }

  /**
   * Constructor
   * @param sessionId the session ID
   * @param type the session type
   * @param attribute the session attribute
   * @param event the event
   */
  public AttributeEvent(String sessionId, SessionType type, Session.Attribute attribute, int event)
  {
    super(sessionId, type, event);
    this.attribute = attribute;
  }

  @Override
  public String toString()
  {
    return "AttributeEvent [sid=" + getSessionId() + ",evt=" + SessionEventFilter.toString(getEventType()) 
        + ",name=" + attribute.getName() + "]";
  }
  
}
