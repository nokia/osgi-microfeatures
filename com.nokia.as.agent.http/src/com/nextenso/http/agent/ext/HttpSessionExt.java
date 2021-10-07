package com.nextenso.http.agent.ext;

import com.alcatel_lucent.convergence.services.PrivateAttributesNames;
import com.alcatel_lucent.ha.services.Flattable;
import com.nextenso.proxylet.http.HttpSession;

public interface HttpSessionExt extends HttpSession, Flattable {
  
  /**
   * Indicates that a session cookie (usually named JSESSIONID) has been set.
   * So, this session must be kept alive for next requests.
   *
   */
  public void sessionCookieSet();
  
  /**
   * Get the status of the session cookie (set or not)
   * @return session cookie status
   */
  public boolean isSessionCookieSet();
  
  /**
   * Invalidate the session (JSR-154)
   */
  public void jsr154Invalidate();
  
  /**
   * Get the "invalidated" status (JSR-154)
   * @return true when the HttpSessionListener (sessionDestroyed method) have been called
   */
  public boolean isJsr154Invalidated();
  
  /**
   * Indicates that a request working with this session is fully handled 
   */
  public void complete();
  
  /**
   * Get accessed time
   */
  public long getAccessedTime();
  
  /**
   * Get the context path of a web application
   */
  public String getContextPath();
  
  /**
   * Set the context path of a web application
   */
  public void setContextPath(String contextPath);
  
  /**
   * Ask for a new HTTP session
   */
  public String newSession();
  
  public String changeSessionId();
  
  public void setPrivateAttribute(PrivateAttributesNames name, Object attribute);
  
  public Object getPrivateAttribute(PrivateAttributesNames name);
  
}
