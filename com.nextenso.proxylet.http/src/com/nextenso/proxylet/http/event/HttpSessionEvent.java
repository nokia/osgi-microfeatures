package com.nextenso.proxylet.http.event;

import java.util.EventObject;
import com.nextenso.proxylet.http.HttpSession;

/**
 * This class encapsulates an event concerning an HttpSession.
 */
public class HttpSessionEvent extends EventObject {
    
    /**
     * Constructs a new HttpSessionEvent.
     * @param source the HttpSession involved.
     */
    public HttpSessionEvent(HttpSession source){
	super(source);
    }
    
    /**
     * Returns the HttpSession involved.
     * @return the HttpSession
     */
    public HttpSession getSession(){
	Object source = getSource();
	return (source instanceof HttpSession)? (HttpSession)source : null;
    }
        
}
