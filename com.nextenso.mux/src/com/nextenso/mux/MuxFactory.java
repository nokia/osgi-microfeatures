// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;

import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.nextenso.mux.socket.TcpMessageParser;

/**
 * Factory class used to instantiates Mux Connections for {@link MuxHandler}. A Mux connection
 * is actually a plain TCP socket (with keep-alive support) on top of which message are
 * encapsulated in MUX boundaries, so that you can send "hello world" from one side and get
 * "hello World" to the other side, without dealing with fragmentation and reassembly. You also
 * can use the MUX header to insert additional information along with you data.
 */
public abstract class MuxFactory
{
    /**
     * Optional logger, which can be provided in the map passed to the connect/accept methods.
     */
    public final static Object OPT_LOGGER = new Object();

    /**
     * Optional local address which can be provided in the map passed to the connect method.
     */
    public final static Object OPT_LOCAL_ADDR = new Object();

    /**
     * Optional Attributes which can be provided in the map passed to the connect/accept method.
     * The value is Objects, and will be stored inside the Mux Connection. You can retrieve it
     * using the {@link MuxConnection#attachment()} method.
     */
    public final static Object OPT_ATTACH = new Object();

    /**
     * Optional Flags which can be provided in the map passed to the connect/accept method. The
     * value is an IntHashtable.
     */
    public final static Object OPT_FLAGS = new Object();

    /**
     * Optional timeout value for opening a mux connection. The value is a Long, in millis.
     * The value is 15000 ms by default.
     */
    public final static Object OPT_CONNECTION_TIMEOUT = new Object();

    /**
     * Optional input executor for handling mux connection input data. The value is an Executor
     * and if set, it will be used when handling input data. By default, the thread used to read
     * the socket is the reactor thread managing the mux connection. 
     */
    public final static Object OPT_INPUT_EXECUTOR = new Object();   
    
    /**
     * The protocol declared by the MuxHandler to be associated with the newly created MuxConnection
     */
    public final static Object PROTOCOL = new Object();
     
    /**
     * Returns the MuxFactory service. Notice that this service is an OSGi service.
     * 
     * @returns the MuxFactory service or null if the service is not yet available.
     */
    public static MuxFactory getInstance()
    {
        return ServiceLoader.getService(MuxFactory.class);
    }

    /**
     * Creates a not connected mux connection. You can then connect it using the connect method.
     * 
     * @param reactor
     * @param listener
     * @param muxHandler
     * @param to
     * @param stackId
     * @param stackName
     * @param stackHost
     * @param stackInstance
     * @param opts
     * @return A new MUX connection.
     */
    @SuppressWarnings("unchecked")
    public abstract MuxConnection newMuxConnection(Reactor reactor, ConnectionListener listener,
                                                   MuxHandler muxHandler, InetSocketAddress to, int stackId,
                                                   String stackName, String stackHost, String stackInstance,
                                                   Map opts);

    /**
     * Creates a local mux connection to be used independently of a stack.
     * 
     * @param muxHandler
     * @param appId
     * @param appName
     * @param parser
     * @param logger
     * @return a new MUX connection.
     */
    public MuxConnection newLocalMuxConnection(Reactor reactor, MuxHandler muxHandler, int appId,
					       String appName, TcpMessageParser parser, Logger logger){
	return newLocalMuxConnection (reactor, muxHandler, appId, appName, null, parser, logger);
    }
    /**
     * Creates a local mux connection to be used independently of a stack.
     * 
     * @param muxHandler
     * @param appId
     * @param appName
     * @param instanceName
     * @param parser
     * @param logger
     * @return a new MUX connection.
     */
    public abstract MuxConnection newLocalMuxConnection(Reactor reactor, MuxHandler muxHandler, int appId,
                                                        String appName, String instanceName, TcpMessageParser parser, Logger logger);
    
    /**
     * Connects a mux connection asynchronously. The reactor used to connect the connection is the
     * one you have provided in the newMuxConnection method.
     */
    public abstract void connect(MuxConnection connection);

    /**
     * This method is used by the super-agent, when accepting sockets connection.
     * 
     * @param reactor The reactor which manages the connection
     * @param listener The ConnectionListener which will be notified when the connection is
     *          established/closed.
     * @param muxHandler The sub-agent MuxHandler
     * @param from The listening address
     * @param opts Optional mux connection parameters.
     */
    @SuppressWarnings("unchecked")
    public abstract InetSocketAddress accept(Reactor reactor, ConnectionListener listener,
                                             MuxHandler muxHandler, InetSocketAddress from, Map opts)
        throws IOException;

    /**
     * This interface is called back when connections are established/accepted, and is only meant
     * to be used when connecting mux handlers.
     */
    public interface ConnectionListener
    {
        /**
         * The mux handler has been connected.
         * 
         * @param cnx the new mux connection
         * @param error an exception on any errors
         */
        void muxConnected(MuxConnection cnx, Throwable error);

        /**
         * The mux handler connection has been accepted.
         * 
         * @param cnx The new mux connection
         * @param error An exception on any errors
         */
        void muxAccepted(MuxConnection cnx, Throwable error);
        
        /**
         * The @link {@link MuxConnection#sendMuxStart()} method has been invoked on the mux connection.
         * @param cnx the cnx on which a mux start message has been sent
         * @return true if the mux connection 
         */
        default boolean muxStarted(MuxConnection cnx) { return true; }

        /**
         * The @link {@link MuxConnection#sendMuxSttop()} method has been invoked on the mux connection.
         * @param cnx the cnx on which a mux stop message has been sent
         */
        default boolean muxStopped(MuxConnection cnx) { return true; }

        /**
         * THe mux handler connection has been closed.
         * 
         * @param cnx The closed mux handler connection.
         */
        void muxClosed(MuxConnection cnx);
    }
}
