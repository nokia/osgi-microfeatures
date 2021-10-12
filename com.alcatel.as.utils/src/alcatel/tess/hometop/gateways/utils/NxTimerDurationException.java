// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

/**
  Exception thrown when a timer duration is out of range
*/
public class NxTimerDurationException extends Exception {
  /**
    Constructor without message.
  */
  public NxTimerDurationException() {
    super();
  }
  
  /**
    Constructor with user message.
  */
  public NxTimerDurationException(String message) {
    super(message);
  }
}
