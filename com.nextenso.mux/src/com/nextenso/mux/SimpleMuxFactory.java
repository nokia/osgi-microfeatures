package com.nextenso.mux;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import alcatel.tess.hometop.gateways.reactor.Reactor;

import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * Factory class used to instantiates Mux Connections for {@link SimpleMuxHandler}. A Mux
 * connection is actually a plain TCP socket (with keep-alive support) on top of which message
 * are encapsulated in MUX boundaries, so that you can send "hello world" from one side and get
 * "hello World" to the other side, without dealing with fragmentation and reassembly. You also
 * can use the MUX header to insert additional information along with you data.
 */
public abstract class SimpleMuxFactory
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
     * The value is an Object, and will be stored inside the Mux Connection attachment. You can
     * retrieve it using the {@link MuxConnection#attachment()} method.
     */
    public final static Object OPT_ATTACH = new Object();

    /**
     * Optional timeout value for opening a mux connection. The value is a Long, in milliseconds.
     */
    public final static Object OPT_CONNECTION_TIMEOUT = new Object();

    /**
     * Optional flag telling if you need to be called back in your {@link SimpleMuxHandler} using
     * NIO byte buffers. The value must be a Boolean (FALSE by default).
     */
    public final static Object OPT_USE_NIO = new Object();

    /**
     * The Optional KeepAlive configuration passed to the connect/accept methods. The key specify
     * the keepAlive interval in milliseconds used to check the connection. <br/>
     * The value must be a Long. <br/>
     * '0' means 'no keepAlive' (default). <br/>
     */
    public final static Object OPT_KEEP_ALIVE = new Object();

    /**
     * The configuration key specifying the keepAlive idle factor. The value must be a Long. <br/>
     * 0 or '-1' is the default value and means 'default value (usually configurable)'. <br/>
     * Any positive value indicates an idle factor as needed by the algorithm used to compute the
     * delay after which one consider the connection death.
     */
    public final static Object OPT_KEEP_ALIVE_IDLE_FACTOR = new Object();

    /**
     * Optional Flags which can be provided in the map passed to the connect/accept method. The
     * value is an IntHashtable.
     */
    public final static Object OPT_FLAGS = new Object();

    /**
     * Optional Application Name that can be provided in the connect/accept methods.
     */
    public final static Object OPT_APP_NAME = new Object();

    /**
     * Optional input executor for handling mux connection input data. The value is an Executor
     * and if set, it will be used when handling input data. By default, the thread used to read
     * the socket is the reactor thread managing the mux connection. 
     */
    public final static Object OPT_INPUT_EXECUTOR = new Object();   
    
    /**
     * Gets the SimpleMuxFactory service. Notice that this service is an OSGi service.
     * 
     * @returns the SimpleMuxFactory or null if the service is not yet available.
     */
    public static SimpleMuxFactory getInstance()
    {
        return ServiceLoader.getService(SimpleMuxFactory.class);
    }

    /**
     * Creates a not connected new mux connection.
     * 
     * @param reactor The reactor which manages the connection
     * @param simpleMuxHandler The simple MuxHandler which will be notified when the socket is
     *          accepted/connected.
     * @param to The address where to connect.
     * @param opts Optional mux connection parameters.
     * @return the mux connection that will be connected asynchronously.
     */
    public abstract MuxConnection newMuxConnection(Reactor reactor, SimpleMuxHandler simpleMuxHandler,
                                                   InetSocketAddress to, Map opts);

    /**
     * Connects a mux connection asynchronously. The reactor used to connect the connection is the
     * one you have provided in the newMuxConnection method.
     */
    public abstract void connect(MuxConnection connection);

    /**
     * This method is used when accepting simple Mux connection.
     * 
     * @param reactor The reactor which manages the connection.
     * @param simpleMuxHandler The simple MuxHandler which will be notified when the socket is
     *          accepted/connected.
     * @param from The address to be listened to
     * @param opts The optional parameters (see constants above).
     * @return the Listened address.
     * @throws IOException if the address could not be listened
     */
    public abstract InetSocketAddress accept(Reactor reactor, SimpleMuxHandler simpleMuxHandler,
                                             InetSocketAddress from, Map opts) throws IOException;
}
