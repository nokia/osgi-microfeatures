package com.nextenso.radius.agent;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.apache.log4j.Logger;

import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.proxylet.radius.AuthenticationRule;
import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.acct.AcctUtils;
import com.nextenso.proxylet.radius.acct.CoAUtils;
import com.nextenso.proxylet.radius.acct.DisconnectUtils;
import com.nextenso.radius.agent.engine.MessageProcessingListener;
import com.nextenso.radius.agent.engine.RadiusProxyletEngine.Action;
import com.nextenso.radius.agent.impl.AccountingRequestFacade;
import com.nextenso.radius.agent.impl.AccountingResponseFacade;
import com.nextenso.radius.agent.impl.AuthenticationUtils;
import com.nextenso.radius.agent.impl.CoARequest;
import com.nextenso.radius.agent.impl.DisconnectRequest;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;
import com.alcatel.as.service.concurrent.ExecutorPolicy;

public class AcctCommand
		extends Command
		implements MessageProcessingListener {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.command.acct");

	private AccountingRequestFacade _acctRequest;
	private MessageDigest _digest;

	protected MessageDigest getDigest() {
		if (_digest == null) {
			try {
				_digest = MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException("MD5 Algorithm is not supported");
			}

		}
		return _digest;
	}

	public AcctCommand(MuxConnection connection, int socketId, long id, int identifier, int remoteIP, int remotePort) {
		super(connection, socketId, id, identifier, remoteIP, remotePort);
	}

	@Override
	public void handleRequest(byte[] buff, int off, int len, int code)
		throws IOException {
		try {
			String client = getRemoteIPAsString();
			AuthenticationRule rule = Utils.getAuthenticationManager().getRule(getRemoteIP());
			if (rule == null) {
				throw new SignatureException("Accounting Request Authentication Failed: Client (" + client + "): no secret for IP address=" + client);
			}

			byte[] secretValue = rule.getPassword();
			if (rule.requiresAuthentication()) {
				if (!AuthenticationUtils.authenticate(getDigest(), buff, off, len, secretValue)) {
					throw new SignatureException("Accounting Request Authentication Failed for Client (" + client + ") is not Authorized");
				}
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleRequest: Skipping client authentication for trusted client IP=" + client);
			}
			
			switch (code) {
				case CoAUtils.CODE_COA_REQUEST:
					_acctRequest = new CoARequest(getIdentifier(), true);
					break;
				case DisconnectUtils.CODE_DISCONNECT_REQUEST:
					_acctRequest = new DisconnectRequest(getIdentifier(), true);
					break;
				default:
					_acctRequest = new AccountingRequestFacade(getIdentifier(), true);
			}

			_acctRequest.setProxySecret(secretValue);
			_acctRequest.setCode(code);
			_acctRequest.setClient(client, getRemoteIP(), getRemotePort());
			_acctRequest.readAttributes(buff, off + 20, len - 20);
			_acctRequest.setClientAuthenticator(buff, off + 4);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleRequest: " + getDescription(_acctRequest));
			}
			
			// check Message-Authenticator for integrity
			if (rule.requiresAuthentication()) {
			    if (_acctRequest.getMessageAuthenticatorOffset () != -1){
				if (!AuthenticationUtils.checkRequestMessageAuthenticator(buff, off, len, _acctRequest.getMessageAuthenticatorOffset (), new String (secretValue, "ascii"))) {
				    _acctRequest = null;
				    throw new SignatureException("Message Authenticator check failed for Client (" + client + ")");
				}
			    }
			}

			AccountingResponseFacade acctResponse = (AccountingResponseFacade) getResponse();
			acctResponse.setClient(client, getRemoteIP(), getRemotePort());
			acctResponse.setProxySecret(secretValue);
			if (code == AcctUtils.CODE_ACCOUNTING_REQUEST) {
				acctResponse.setCode(AcctUtils.CODE_ACCOUNTING_RESPONSE);
			}

			RadiusAttribute proxyState = _acctRequest.getRadiusAttribute(RadiusUtils.PROXY_STATE);
			if (proxyState != null) {
				acctResponse.addRadiusAttribute((RadiusAttribute) proxyState.clone());
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
		State state = getState();
		if (state == State.INIT) {
			setState(State.RESPONSE_RECEIVED);
		} else {
			// a retransmission of response
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleResponse: Discarding response: " + getDescription() + ", state=" + state);
			}
			return;
		}

		try {
			if (!_acctRequest.authenticate(buff, off, len)) {
				throw new SignatureException("Accounting Response Authentication Failed");
			}

			AccountingResponseFacade acctResponse = (AccountingResponseFacade) getResponse();
			acctResponse.setCode(code);
			acctResponse.removeRadiusAttribute(RadiusUtils.PROXY_STATE);
			acctResponse.readAttributes(buff, off + 20, len - 20);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(getDescription(acctResponse));
			}
			
			// check Message-Authenticator for integrity
			if (acctResponse.getMessageAuthenticatorOffset () != -1){
				if (!_acctRequest.checkMessageAuthenticator (buff, off, len, acctResponse.getMessageAuthenticatorOffset ()))
					throw new SignatureException("Message Authenticator check failed for response");
			}

			Utils.handleResponseProxyState(acctResponse);
			acctResponse.setServer(_acctRequest.getServer());
			handleResponse(false);
		}
		catch (Throwable t) {
			handleException(t);
		}
	}

	public void run() {
		State state = getState();
		if (state == State.INIT) {
			handleRequest(true);
		} else if (state == State.RESPONSE_RECEIVED) {
			handleResponse(true);
		}
	}

	private void handleRequest(boolean ignoreMayBlock) {
		Utils.getEngine().handleRequest(_acctRequest, ignoreMayBlock, this);
	}

	private void handleResponse(boolean ignoreMayBlock) {
		AccountingResponseFacade acctResponse = (AccountingResponseFacade) getResponse();
		Utils.getEngine().handleResponse(acctResponse, ignoreMayBlock, this);
	}

	/**
	 * @see com.nextenso.radius.agent.Command#getRequest()
	 */
	@Override
	public RadiusMessageFacade getRequest() {
		return _acctRequest;
	}

	/**
	 * @see com.nextenso.radius.agent.Command#getResponse()
	 */
	@Override
	public RadiusMessageFacade getResponse() {
		return (RadiusMessageFacade) _acctRequest.getResponse();
	}

	/**
	 * @see com.nextenso.radius.agent.engine.MessageProcessingListener#messageProcessed(com.nextenso.radius.agent.impl.RadiusMessageFacade,
	 *      com.nextenso.radius.agent.engine.RadiusProxyletEngine.Action)
	 */
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
			if (message instanceof AccountingRequestFacade) {
				if (action == Action.REQUEST) {
					// set the proxy state
					_acctRequest.setProxyStateAttribute(getKey ().getId());
					if (sendRequest(message)) {
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
				} else if (action == Action.NO_RESPONSE) {
					abort();
				} else if (action == Action.MAY_BLOCK_RESPONSE) {
					setState(State.RESPONSE_RECEIVED);
					Utils.start(this);
				} else if (action == Action.MAY_BLOCK_REQUEST) {
					Utils.start(this);
				}

			} else if (message instanceof AccountingResponseFacade) {
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

	/**
	 * @see com.nextenso.radius.agent.engine.MessageProcessingListener#messageProcessingError(com.nextenso.radius.agent.impl.RadiusMessageFacade,
	 *      com.nextenso.proxylet.engine.ProxyletEngineException)
	 */
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
