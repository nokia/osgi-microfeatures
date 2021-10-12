// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.event;

import com.alcatel.as.session.distributed.SessionType;

/**
 * This class defines an event on a session.
 */

public class SessionEvent
{
  private int event;
  private SessionType type;
  private String sessionId;
  private Object etag;

  /**
   * Get the event type (events type are defined in {@link SessionEventFilter})
   * @return the event code
   */
  public int getEventType()
  {
    return event;
  }

  /**
   * Get the session ID
   * @return the session ID
   */
  public String getSessionId()
  {
    return sessionId;
  }

  /**
   * Get the session type
   * @return the session type
   */
  public SessionType getSessionType()
  {
    return type;
  }

  /**
   * Get the etag of the session
   * @return the etag of the session
   */
  public String getEtag()
  {
    if (etag != null)
    {
      return etag.toString();
    }
    return null;
  }

  /**
   * @internal
   * @param etag
   */
  public void setEtag(Object etag)
  {
    this.etag = etag;
  }

  /**
   * Constructor
   * @param sessionId the session ID
   * @param type the session type
   * @param event the event
   */
  public SessionEvent(String sessionId, SessionType type, int event)
  {
    this.sessionId = sessionId;
    this.type = type;
    this.event = event;
  }

  @Override
  public String toString()
  {
    return "SessionEvent [sid=" + sessionId + ",evt=" + SessionEventFilter.toString(event) + "]";
  }
  
  
}
