package com.nextenso.proxylet.http.event;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.event.ProxyletEvent;

/**
 * This class encapsulates a disconnection event.
 * <p/>The Agent may or may not use it instead of AbortEvent.
 */
public class DisconnectionEvent extends AbortEvent {
        
    /**
     * A String that identifies the Client
     */
    public static final String CLIENT = "client";
    /**
     * A String that identifies the Server
     */
    public static final String SERVER = "server";
    
    /**
     * Constructs a new DisconnectionEvent.
     * @param source should be CLIENT or SERVER.
     * @param data the ProxyletData involved, may be an HttpRequest or an HttpResponse.
     */
    public DisconnectionEvent (String source, ProxyletData data){
	super(source, data);
    }
    
    /**
     * Specifies if the client performed the diconnection (source == CLIENT).
     * @return true or false
     */
    public boolean clientDisconnected (){
	return (CLIENT.equals (getSource ()));
    }
    
    /**
     * Specifies if the server performed the diconnection (source == SERVER).
     * @return true or false
     */
    public boolean serverDisconnected (){
	return (SERVER.equals (getSource ()));
    }
    
}
