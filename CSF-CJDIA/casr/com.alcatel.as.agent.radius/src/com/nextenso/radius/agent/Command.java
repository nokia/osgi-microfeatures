package com.nextenso.radius.agent;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;
import com.nextenso.radius.agent.impl.RadiusServer;
import com.alcatel.as.service.concurrent.PlatformExecutor;

public abstract class Command
		implements Runnable {

	private static Logger LOGGER = Logger.getLogger("agent.radius.command");

	/**
	 * The Cache Task.
	 */
	private static class CacheTask
			extends TimerTask {

		private Command _cmd;

		CacheTask(Command cmd) {
			_cmd = cmd;
		}

		/**
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			try {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Response Cache Task for Command: " + _cmd.getDescription());
				}
				_cmd.release();
			}
			catch (Throwable t) {
				LOGGER.warn("Exception while handling Response Timeout", t);
			}
		}
	}


	/**
	 * The TimeoutTask class.
	 */
	private static class TimeoutTask
			extends TimerTask {

		private Command _cmd;

		TimeoutTask(Command cmd) {
			_cmd = cmd;
		}

		/**
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			try {
				int tries = _cmd.handleRequestTimeout();
				if (LOGGER.isDebugEnabled()) {
					switch (tries) {
						case -1:
							LOGGER.debug("Request Timeout Task for Command: " + _cmd + " : Discarded");
							break;
						case 0:
							LOGGER.debug("Request Timeout Task for Command: " + _cmd + " : Applied");
							break;
						default:
							LOGGER.debug("Request Timeout Task for Command: " + _cmd + " : new Attempt : #" + tries);
					}
				}
			}
			catch (Throwable t) {
				LOGGER.warn("Exception while handling Request Timeout", t);
			}
		}
	}

	protected enum State {
		INIT(-1),
		RESPONSE_RECEIVED(1),
		RESPONSE_SENT(2),
		ABORT(10);

		private int _value;

		private State(int value) {
			_value = value;
		}

		public int getValue() {
			return _value;
		}
	}

	private AtomicReference<State> _state = new AtomicReference<State>(State.INIT);

	private int _identifier;

	private int _remoteIP, _remotePort;
	private String _remoteIPAsString;
	private MuxConnection _connection;
	private long _id;
	private int _tries = 0;
	private int _socketIdReceived, _socketIdSend;
	private Agent _agent;
	private Utils.Key _key;
	protected PlatformExecutor _exec;

	protected Command(MuxConnection connection, int socketId, long id, int identifier, int remoteIP, int remotePort) {
		_connection = connection;
		_socketIdReceived = socketId;
		_socketIdSend = Utils.getSockId(connection);
		_id = id;
		_identifier = identifier;
		_remoteIP = remoteIP;
		_remotePort = remotePort;
	}
	public Command setPlatformExecutor (PlatformExecutor exec){
		_exec = exec;
		return this;
	}

	public void setKey (Utils.Key key){
		_key = key;
	}
	public Utils.Key getKey (){
		return _key;
	}

	/**
	 * Processes a request.
	 * 
	 * @param buffer The byte array representing the request content.
	 * @param offset The offset of the request in the buffer
	 * @param length The length of the request in the buffer
	 * @param code The radius code of the message.
	 * @throws IOException if a problem occurs.
	 */
	public abstract void handleRequest(byte[] buffer, int offset, int length, int code)
		throws IOException;

	/**
	 * Processes a response.
	 * 
	 * @param buffer The byte array representing the response content.
	 * @param offset The offset of the request in the buffer
	 * @param length The length of the request in the buffer
	 * @param code The radius code of the message.
	 * @throws IOException if a problem occurs.
	 */
	public abstract void handleResponse(byte[] buffer, int offset, int length, int code)
		throws IOException;

	/**
	 * Gets the request.
	 * 
	 * @return The request.
	 */
	protected abstract RadiusMessageFacade getRequest();

	/**
	 * Gets the response.
	 * 
	 * @return The response.
	 */
	protected abstract RadiusMessageFacade getResponse();

	/**
	 * Gets the request identifier.
	 * 
	 * @return The request identifier.
	 */
	public long getId() {
		return _id;
	}

	/**
	 * Gets the remote IP address as a integer.
	 * 
	 * @return The remote IP address.
	 */
	public int getRemoteIP() {
		return _remoteIP;
	}

	/**
	 * Gets the remote IP address as a string.
	 * 
	 * @return The remote IP address.
	 */
	public String getRemoteIPAsString() {
		if (_remoteIPAsString == null) {
			_remoteIPAsString = MuxUtils.getIPAsString(_remoteIP);
		}
		return _remoteIPAsString;
	}

	/**
	 * Gets the remote port.
	 * 
	 * @return The remote port.
	 */
	public int getRemotePort() {
		return _remotePort;
	}

	/**
	 * Gets the RADIUS identifier.
	 * 
	 * @return The RADIUS identifier.
	 */
	public int getIdentifier() {
		return _identifier;
	}

	/**
	 * Builds a description of this command.
	 * 
	 * @return A string representation of this command.
	 */
	public String getDescription() {
		StringBuilder buff = new StringBuilder("(IP=");
		buff.append(getRemoteIPAsString());
		buff.append("/port=");
		buff.append(String.valueOf(_remotePort));
		buff.append("/identifier=");
		buff.append(String.valueOf(_identifier));
		buff.append("/id=");
		buff.append(String.valueOf(_id));
		buff.append(")");
		return buff.toString();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * Builds a description of the message in this command.
	 * 
	 * @param message The RADIUS message.
	 * @return A string representation
	 */
	public String getDescription(RadiusMessageFacade message) {
		StringBuilder buff = new StringBuilder("Parsed Radius message: ");
		buff.append(getDescription());
		buff.append(" :\n");
		buff.append(message);
		return buff.toString();
	}

	/**
	 * Handles an exception.
	 * 
	 * @param t The exception.
	 */
	protected void handleException(Throwable t) {
		String text = "A problem occurred while processing "
				+ ((getState().getValue() >= State.RESPONSE_RECEIVED.getValue()) ? "the response for " : "") + "request " + getDescription();
		if (t instanceof ProxyletEngineException) {
			ProxyletEngineException exc = (ProxyletEngineException) t;
			if (exc.getProxylet() != null) {
				text += " Proxylet: " + exc.getProxylet().getProxyletInfo();
			}
			if (exc.getType() == ProxyletEngineException.ENGINE) {
				text += " : " + exc.getMessage();
				LOGGER.error(text);
			} else if (exc.getType() == ProxyletEngineException.PROXYLET) {
				LOGGER.error(text, exc.getThrowable());
			}
		} else if (t instanceof SignatureException) {
			if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn(text + " " + t.getMessage());
			}
		} else {
			LOGGER.error(text, t);
		}
		// abort cases 1. and 4.
		abort();
	}

	/**
	 * Handles retransmission.
	 * 
	 * @return true if a response has been retransmitted.
	 * @throws IOException if the response cannot be sent.
	 */
	public boolean handleRetransmission()
		throws IOException {
		if (getState() != State.RESPONSE_SENT) {
			return false;
		}
		sendResponse(getResponse(), false);
		return true;
	}

	/**
	 * Handles the case when no response has been received for a request.
	 * 
	 * @return the number of retries.
	 * @throws IOException if the request cannot be retransmitted.
	 */
	public int handleRequestTimeout()
		throws IOException {
		// keep >= and not == (in case Utils.REQ_MAX_TRY was modified)
		boolean maxTry = (_tries >= RadiusProperties.getRequestMaxTry());
		if (getState().getValue() > State.INIT.getValue()) {
			return -1;
		}
		if (maxTry) {
			setState(State.ABORT);
		}
		if (maxTry) {
			// abort case 3.
			abort();
			return 0;
		}
		if (sendRequest(getRequest())) {
			return incrementAndGetTries();
		}
		if (getState().getValue() >  State.INIT.getValue()) {
			return -1;
		}
		setState( State.ABORT);
		// abort case 2.
		abort();
		return 0;
	}

	/**
	 * Increments the number of retries.
	 * 
	 * @return The new number of retries.
	 */
	protected int incrementAndGetTries() {
		return ++_tries;
	}

	/**
	 * Abort cases: - 1. exception while processing request - 2. failure to send
	 * request - 3. timeout to get response - 4. exception while processing
	 * response - 5. failure to send response
	 */
	protected void abort() {
		setState(State.ABORT);
		release();
		if (getRequest() != null) {
			// in cas of a SignatureException, the request is null
			getRequest().abort();
		}
	}

	/**
	 * Gets the state of this command.
	 * 
	 * @return The state.
	 */
	public final State getState() {
		return _state.get();
	}

	/**
	 * Sets the state.
	 * 
	 * @param state The state.
	 */
	public final void setState(State state) {
		_state.set(state);
	}

	/**
	 * Sets the associated agent.
	 * 
	 * @param agent
	 */
	public void setAgent(Agent agent) {
		_agent = agent;
	}

	protected Agent getAgent() {
		return _agent;
	}

	/**
	 * Sends a request.
	 * 
	 * @param message The message to be sent.
	 * @return true if success.
	 * @throws IOException
	 */
	public boolean sendRequest(RadiusMessageFacade message)
		throws IOException {
		// get the server
		RadiusServer server = RadiusServer.getServer(message, getRemoteIP());
		if (server == null) {
			throw new IOException("Unable to forward the request: no destinaion server found");
		}
		// send
		ByteBuffer buff = new ByteBuffer(128);
		try {
			OutputStream out = buff.getOutputStream();
			message.writeTo(out);

			if (_connection.sendUdpSocketData(_socketIdSend, server.getIp(), server.getPort(), 0, 0, buff.toByteArray(false), 0, buff.size(), false)) {
				_exec.schedule(new TimeoutTask(this), RadiusProperties.getRequestTimeout(), java.util.concurrent.TimeUnit.SECONDS);
				return true;
			}
		}
		finally {
			buff.close();
		}
		// muxClosed will be called
		LOGGER.warn("Failed to send radius request");
		return false;
	}
	/**
	 * Sends a response.
	 * 
	 * @param message The message to be sent.
	 * @param doCache true if the response must be cached (to be used when the
	 *          request is transmitted again).
	 * @return true if the response is sent.
	 * @throws IOException
	 */
	public boolean sendResponse(RadiusMessageFacade message, boolean doCache)
		throws IOException {
		ByteBuffer buff = new ByteBuffer(128);
		try {
			OutputStream out = buff.getOutputStream();
			message.writeTo(out);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("sendResponse: response=" + message);
			}
			if (_connection.sendUdpSocketData(_socketIdReceived, getRemoteIP(), getRemotePort(), 0, 0, buff.toByteArray(false), 0, buff.size(), false)) {
				if (RadiusProperties.getResponseTimeout() > 0) {
					if (doCache) {
						_exec.schedule(new CacheTask(this), RadiusProperties.getResponseTimeout(), java.util.concurrent.TimeUnit.SECONDS);
					}
				} else {
					release();
				}
				return true;
			}
			LOGGER.warn("Failed to send radius response");
			return false;
		}
		finally {
			buff.close();
		}

	}


	public void release() {
	    Utils.removeCommand (_connection, getKey ());
	}


}
