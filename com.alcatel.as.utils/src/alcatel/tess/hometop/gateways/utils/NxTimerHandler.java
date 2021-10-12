// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

/**
  This interface needs to be implemented by all entities which want
  to use timers to allow these entities to be notified when a timer
  expires.
*/
public interface NxTimerHandler {
  /**
    Timer expiration notification. This method is called by the
    timer handler when a timer expires.

    @param userObject User object associated with the timer
  */
  public void timerExpired(Object userObject);
}
