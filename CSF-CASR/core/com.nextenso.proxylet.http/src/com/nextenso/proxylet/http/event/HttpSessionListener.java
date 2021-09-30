package com.nextenso.proxylet.http.event;

import java.util.EventListener;

/**
 * The interface to implement to be notified when HttpSessionEvents occurs.
 * <p/>The HttpSessionListeners are registered in the Agent in the deployment phase.
 */
public interface HttpSessionListener extends EventListener {
    
    /**
     * Called when an HttpSession is created
     * @param event the HttpSession encapsulating the new HttpSession.
     */
    public void sessionCreated(HttpSessionEvent event);
    
    /**
     * Called when an HttpSession is destroyed
     * @param event the HttpSession encapsulating the destroyed HttpSession.
     */
    public void sessionDestroyed(HttpSessionEvent event);
}

