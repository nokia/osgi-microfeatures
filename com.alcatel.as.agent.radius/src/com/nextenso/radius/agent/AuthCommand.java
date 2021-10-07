package com.nextenso.radius.agent;

import java.io.IOException;
import java.security.SignatureException;

import org.apache.log4j.Logger;

import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.proxylet.radius.AuthenticationRule;
import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.radius.agent.engine.MessageProcessingListener;
import com.nextenso.radius.agent.engine.RadiusProxyletEngine.Action;
import com.nextenso.radius.agent.impl.AccessRequestFacade;
import com.nextenso.radius.agent.impl.AccessResponseFacade;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;
import com.nextenso.radius.agent.impl.AuthenticationUtils;
import com.alcatel.as.service.concurrent.ExecutorPolicy;

public class AuthCommand
		extends Command
		implements MessageProcessingListener {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.command.auth");

	private AccessRequestFacade _authRequest;

	public AuthCommand(MuxConnection connection, int socketId, long id, int identifier, int remoteIP, int remotePort) {
		super( connection, socketId, id, identifier, remoteIP, remotePort);
	}

	@Override
	public void handleRequest(byte[] buff, int off, int len, int code)
		throws IOException {
		try {
			String client = getRemoteIPAsString();
			// the proxy secret and the authenticator must be set before parsing for auth
			AuthenticationRule rule = Utils.getAuthenticationManager().getRule(getRemoteIP());
			if (rule == null) {
				throw new SignatureException("Access Request Authentication Failed: Client (" + client + ") is not Authorized");
			}

			_authRequest = new AccessRequestFacade(getIdentifier(), true);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleRequest: receive a request= " + _authRequest);
			}

			byte[] secretValue = rule.getPassword();
			_authRequest.setProxySecret(secretValue);
			_authRequest.setCode(code);
			_authRequest.setClient(client, getRemoteIP(), getRemotePort());
			_authRequest.setAuthenticator(buff, off + 4);
			_authRequest.readAttributes(buff, off + 20, len - 20);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleRequest: request=" + getDescription(_authRequest));
			}
			
			// check Message-Authenticator for integrity
			if (rule.requiresAuthentication()) {
			    if (_authRequest.getMessageAuthenticatorOffset () != -1){
				if (!AuthenticationUtils.checkMessageAuthenticator(buff, off, len, _authRequest.getMessageAuthenticatorOffset (), buff, off+4, new String (secretValue, "ascii"))) {
				    _authRequest = null;
				    throw new SignatureException("Message Authenticator check failed for Client (" + client + ")");
				}
			    }
			}
			
			AccessResponseFacade authResponse = (AccessResponseFacade) _authRequest.getResponse();
			authResponse.setProxySecret(secretValue);
			authResponse.setClient(client, getRemoteIP(), getRemotePort());

			RadiusAttribute proxyState = _authRequest.getRadiusAttribute(RadiusUtils.PROXY_STATE);
			if (proxyState != null) {
				authResponse.addRadiusAttribute((RadiusAttribute) proxyState.clone());
			}
			handleRequest(false);
		}
		catch (Throwable t) {
			handleException(t);
		}
	}

	@Override
	public void handleResponse(byte[] buff, int off, int len, int code)
		throws IOException {
		if (getState() == State.INIT) {
			setState(State.RESPONSE_RECEIVED);
		} else {
			// a retransmission of response
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Discarding response: " + getDescription() + " : state=" + getState());
			}
			return;
		}
		try {
			if (!_authRequest.authenticate(buff, off, len)) {
				throw new SignatureException("Access Response Authentication Failed");
			}

			AccessResponseFacade authResponse = (AccessResponseFacade) _authRequest.getResponse();
			authResponse.setCode(code);
			authResponse.removeRadiusAttribute(RadiusUtils.PROXY_STATE);
			authResponse.readAttributes(buff, off + 20, len - 20);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleResponse: " + getDescription(authResponse));
			}

			// check Message-Authenticator for integrity
			if (authResponse.getMessageAuthenticatorOffset () != -1){
				if (!_authRequest.checkMessageAuthenticator (buff, off, len, authResponse.getMessageAuthenticatorOffset ()))
					throw new SignatureException("Message Authenticator check failed for response");
			}

			Utils.handleResponseProxyState(authResponse);
			authResponse.setServer(_authRequest.getServer());
			handleResponse(false);
		}
		catch (Throwable t) {
			handleException(t);
		}
	}

	public void run() {
		State state = getState();
		try {
			if (state == State.INIT) {
				handleRequest(true);
			} else if (state == State.RESPONSE_RECEIVED) {
				handleResponse(true);
			}
		}
		catch (Throwable t) {
			handleException(t);
		}
	}

	private void handleRequest(boolean ignoreMayBlock) {
		Utils.getEngine().handleRequest(_authRequest, ignoreMayBlock, this);
	}

	private void handleResponse(boolean ignoreMayBlock) {
		AccessResponseFacade authResponse = (AccessResponseFacade) _authRequest.getResponse();
		Utils.getEngine().handleResponse(authResponse, ignoreMayBlock, this);
	}

	@Override
	public RadiusMessageFacade getRequest() {
		return _authRequest;
	}

	@Override
	public RadiusMessageFacade getResponse() {
		return (RadiusMessageFacade) _authRequest.getResponse();
	}

	@Override
	public void messageProcessed(final RadiusMessageFacade message, final Action action) {
		Runnable r = new Runnable (){
				public void run (){
					messageProcessedInlined (message, action);
				}
			};
		_exec.execute (r, ExecutorPolicy.INLINE);
	}
	private void messageProcessedInlined(RadiusMessageFacade message, Action action) {
		try {
			if (message instanceof AccessRequestFacade) {
				if (action == Action.REQUEST) {
					// set the proxy state
					_authRequest.setProxyStateAttribute(getKey ().getId());
					if (sendRequest(_authRequest)) {
						incrementAndGetTries();
					} else {
						// abort case 2.
						abort();
					}
				} else if (action == Action.RESPONSE) {
					if (sendResponse(getResponse(), true)) {
						setState(State.RESPONSE_SENT);
					} else {
						// abort case 5.
						abort();
					}
				} else if (action == Action.MAY_BLOCK_RESPONSE) {
					setState(State.RESPONSE_RECEIVED);
					Utils.start(this);
				} else if (action == Action.MAY_BLOCK_REQUEST) {
					Utils.start(this);
				}

			} else if (message instanceof AccessResponseFacade) {
				if (action == Action.RESPONSE) {
					if (sendResponse(message, true)) {
						setState(State.RESPONSE_SENT);
					} else {
						// abort case 5.
						abort();
					}
				} else if (action == Action.MAY_BLOCK_RESPONSE) {
					Utils.start(this);
				}

			}
		}
		catch (Throwable t) {
			handleException(t);
		}
	}

	@Override
	public void messageProcessingError(final RadiusMessageFacade message, final ProxyletEngineException error) {
		Runnable r = new Runnable (){
				public void run (){
					handleException (error);
				}
			};
		_exec.execute (r, ExecutorPolicy.INLINE);
	}

}
