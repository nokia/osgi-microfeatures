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
