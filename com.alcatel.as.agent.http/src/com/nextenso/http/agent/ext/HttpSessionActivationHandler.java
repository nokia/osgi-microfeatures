// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.ext;

public interface HttpSessionActivationHandler {
  
  /**
   * Registers a HttpSessionActivationListener
   * @param listener the HttpSessionActivationListener to add to the list of listeners.
   */
  public void registerActivationSessionListener(HttpSessionActivationListener listener);
  
  /**
   * Deregisters a HttpSessionActivationListener
   * @param listener the HttpSessionActivationListener to remove from the list of listeners.
   */
  public void deregisterActivationSessionListener(HttpSessionActivationListener listener);
  
}
