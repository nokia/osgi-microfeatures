package com.nextenso.http.agent.ext;

import com.nextenso.proxylet.http.event.HttpSessionEvent;

public interface HttpSessionActivationListener {

    /**
     * Notification that the session has just been activated.
     * @param se Session event
     */
    void sessionDidActivate(HttpSessionEvent se);

    /**
     * Notification that the session is about to be passivated
     * @param se Session event
     */
    void sessionWillPassivate(HttpSessionEvent se);

}
