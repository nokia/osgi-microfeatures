package com.alcatel.as.ioh.server;

import java.util.*;
import java.net.*;
import java.nio.*;

import org.osgi.annotation.versioning.ProviderType;

public interface TcpAgent {

    /**
     * Returns a unique id identifying the agent type.
     * Ids must be referenced in CASR confluence to guarantee uniqueness.
     */
    public int id ();
    /**
     * Returns the protocol handled by the Agent : mostly informational.
     */
    public String protocol ();
    
    /**
     * Called when a client connected.
     * Called in the TcpClient queue.
     */
    public default void clientOpened (TcpClient client){
    }
    
    /**
     * Called when client data arrive.
     * The data are given to the agent : no reuse.
     * Called in the TcpClient queue.
     */
    public default void clientData (TcpClient client, ByteBuffer data){
    }
    
    /**
     * Called when a client is closed.
     * Called in the TcpClient queue.
     */
    public default void clientClosed (TcpClient client){
    }

    
    /**
     * The context to wrap a client.
     */
    @ProviderType
    public interface TcpClient {

	/**
	 * Returns a unique id.
	 */
	public int id ();

	/**
	 * Attaches an object.
	 */
	public void attach (Object x);
	/**
	 * Returns the attachement.
	 */
	public <T> T attachment ();

	/**
	 * Returns the remote client address.
	 */
	public InetSocketAddress getRemoteAddress ();
	/**
	 * Returns the local address.
	 */
	public InetSocketAddress getLocalAddress ();

	
	/**
	 * Closes the client (the callback will come later - data can still arrive).
	 */
	public void close ();
	/**
	 * Send data.
	 * Indicate if the data are given away (copy=false) or not (copy=true).
	 */
	public void send (boolean copy, ByteBuffer... data);
	
    }
}
