package com.nextenso.radius.agent.engine;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.engine.AsyncProxyletManager;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.ProxyletChain.ProxyletStateTracker;
import com.nextenso.proxylet.engine.ProxyletEngine;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.proxylet.engine.ProxyletUtils;
import com.nextenso.proxylet.radius.RadiusProxylet;
import com.nextenso.proxylet.radius.acct.AccountingRequestProxylet;
import com.nextenso.proxylet.radius.acct.AccountingResponseProxylet;
import com.nextenso.proxylet.radius.auth.AccessRequestProxylet;
import com.nextenso.proxylet.radius.auth.AccessResponseProxylet;
import com.nextenso.radius.agent.impl.AccessRequestFacade;
import com.nextenso.radius.agent.impl.AccessResponseFacade;
import com.nextenso.radius.agent.impl.AccountingRequestFacade;
import com.nextenso.radius.agent.impl.AccountingResponseFacade;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;

public class RadiusProxyletEngine
		extends ProxyletEngine {

	public enum Action {
		REQUEST,
		MAY_BLOCK_REQUEST,
		RESPONSE,
		MAY_BLOCK_RESPONSE,
		NO_RESPONSE
	}

	private static Logger LOGGER = Logger.getLogger("agent.radius.pxlet.engine");
	private static final Object IGNORE_MAY_BLOCK_ATTRIBUTE = "agent.radius.IgnoreMayBlock";
	private static final Object LISTENER_ATTRIBUTE = "agent.radius.listener";

	private RadiusProxyletContainer _container;
	private boolean _isUsingLicense = true;

	public RadiusProxyletEngine(RadiusProxyletContainer container) {
		_container = container;
	}

	public RadiusProxyletContainer getProxyletContainer() {
		return _container;
	}

	/**
	 * Check if license is valid for all the deployed proxylets.
	 * 
	 * @throws NoValidLicenseException if at leat one deployed proxylet has no
	 *           valid license.
	 */
	@Override
	public final void checkLicense() {
		
		_isUsingLicense = ProxyletUtils.isInAgentMode();
		if (!_isUsingLicense) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: Checking license for radius proxylets");
		}

		RadiusProxyletContext radiusCtx = getProxyletContainer().getRadiusProxyletContext();
		if (radiusCtx != null) {
			checkLicenseForContext(radiusCtx);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: Radius proxylets license check ok");
		}

	}

	/*********************************************
	 * External Calls
	 ********************************************/

	/**
	 * Returns REQUEST, RESPONSE, MAY_BLOCK_REQUEST, MAY_BLOCK_RESPONSE
	 */
	public void handleRequest(AccountingRequestFacade request, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("handleRequest: ignoreMayBlock=" + ignoreMayBlock);
		}
		// we retrieve the Proxylet chain
		RadiusProxyletChain chain = getProxyletContainer().getChain(request);
		if (chain == null) {
			LOGGER.debug("handleRequest: no request chain -> returns Action.REQUEST");
			listener.messageProcessed(request, Action.REQUEST);
			return;
		}

		AccountingRequestProxylet proxylet = null;
		try {
			while ((proxylet = (AccountingRequestProxylet) chain.nextProxylet(request)) != null) {
				int accept = proxylet.accept(request);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("handleRequest: accept=" + accept + " for proxylet " + proxylet);
				}

				switch (accept) {
					case RadiusProxylet.IGNORE:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: ignore -> go to next");
						}
						chain.shift(request, 1);
						break;

					case RadiusProxylet.ACCEPT_MAY_BLOCK:
						if (!ignoreMayBlock) {
							listener.messageProcessed(request, Action.MAY_BLOCK_REQUEST);
							return;
						}
						//$FALL-THROUGH$

					case RadiusProxylet.ACCEPT:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: accept, call doRequest");
						}

						int status = processProxylet(proxylet, request);
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: status=" + status);
						}
						if (!isValidAccountingRequestValue(status)) {
							messageError(status, chain, proxylet);
							return;
						}

						if (status == RadiusProxylet.SUSPEND) {
							suspend(request, ignoreMayBlock, listener);
							return;
						}

						if (status == AccountingRequestProxylet.NO_RESPONSE) {
							return;
						}

						boolean continueToProcessRequest = progressInRequestChain(chain, request, status, ignoreMayBlock, listener);
						if (!continueToProcessRequest) {
							return;
						}

						break;
					default:
						acceptError(accept, chain, proxylet);
				}
			}
		}
		catch (ProxyletEngineException ee) {
			listener.messageProcessingError(request, ee);
			return;
		}
		catch (Throwable t) {
			listener.messageProcessingError(request, new ProxyletEngineException(chain, proxylet, t));
			return;
		}
		listener.messageProcessed(request, Action.REQUEST);
	}

	private boolean isValidAccountingRequestValue(int status) {
		boolean res = (isValidAccessRequestValue(status) || status == AccountingRequestProxylet.NO_RESPONSE);
		return res;
	}

	private boolean isValidAccessRequestValue(int status) {
		boolean res = (status == RadiusProxylet.FIRST_PROXYLET || status == RadiusProxylet.NEXT_PROXYLET || status == RadiusProxylet.LAST_PROXYLET
				|| status == RadiusProxylet.SAME_PROXYLET || status == RadiusProxylet.RESPOND_FIRST_PROXYLET
				|| status == RadiusProxylet.RESPOND_LAST_PROXYLET || status == RadiusProxylet.SUSPEND);
		return res;
	}

	private int processProxylet(AccountingRequestProxylet proxylet, AccountingRequestFacade request) {
		int status = proxylet.doRequest(request);
		if (_isUsingLicense) {
			processedProxylet(proxylet);
		}
		return status;
	}

	private int processProxylet(AccessRequestProxylet proxylet, AccessRequestFacade request) {
		int status = proxylet.doRequest(request);
		if (_isUsingLicense) {
			processedProxylet(proxylet);
		}
		return status;
	}

	private boolean progressInRequestChain(RadiusProxyletChain chain, AccessRequestFacade request, int status, boolean ignoreMayBlock,
			MessageProcessingListener listener) {
		ProxyletStateTracker tracker = request;
		if (status == RadiusProxylet.FIRST_PROXYLET) {
			chain.reset(tracker);
		} else if (status == RadiusProxylet.NEXT_PROXYLET) {
			chain.shift(tracker, 1);
		} else if (status == RadiusProxylet.LAST_PROXYLET) {
			chain.pad(tracker);
		} else if (status == RadiusProxylet.SAME_PROXYLET) {
			chain.shift(tracker, 0);
		} else if (status == RadiusProxylet.RESPOND_FIRST_PROXYLET) {
			AccessResponseFacade response = (AccessResponseFacade) request.getResponse();
			RadiusProxyletChain pc = _container.getChain(response);
			pc.reset(response);
			handleResponse(response, ignoreMayBlock, listener);
			return false;
		} else if (status == RadiusProxylet.RESPOND_LAST_PROXYLET) {
			listener.messageProcessed(request, Action.RESPONSE);
			return false;
		}

		return true;
	}

	private boolean progressInRequestChain(RadiusProxyletChain chain, AccountingRequestFacade request, int status, boolean ignoreMayBlock,
			MessageProcessingListener listener) {
		ProxyletStateTracker tracker = request;
		if (status == RadiusProxylet.FIRST_PROXYLET) {
			chain.reset(tracker);
		} else if (status == RadiusProxylet.NEXT_PROXYLET) {
			chain.shift(tracker, 1);
		} else if (status == RadiusProxylet.LAST_PROXYLET) {
			chain.pad(tracker);
		} else if (status == RadiusProxylet.SAME_PROXYLET) {
			chain.shift(tracker, 0);
		} else if (status == RadiusProxylet.RESPOND_FIRST_PROXYLET) {
			AccountingResponseFacade response = (AccountingResponseFacade) request.getResponse();
			RadiusProxyletChain pc = _container.getChain(response);
			pc.reset(response);
			handleResponse(response, ignoreMayBlock, listener);
			return false;
		} else if (status == RadiusProxylet.RESPOND_LAST_PROXYLET) {
			listener.messageProcessed(request, Action.RESPONSE);
			return false;
		}

		return true;
	}

	/**
	 * Returns RESPONSE, MAY_BLOCK_RESPONSE
	 */
	public void handleResponse(AccountingResponseFacade response, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("no listener");
		}
		// we retrieve the Proxylet chain
		RadiusProxyletChain chain = getProxyletContainer().getChain(response);
		AccountingResponseProxylet proxylet = null;
		try {
			while ((proxylet = (AccountingResponseProxylet) chain.nextProxylet(response)) != null) {
				int accept = proxylet.accept(response);
				switch (accept) {
					case RadiusProxylet.IGNORE:
						chain.shift(response, 1);
						break;
					case RadiusProxylet.ACCEPT_MAY_BLOCK:
						if (!ignoreMayBlock) {
							listener.messageProcessed(response, Action.MAY_BLOCK_RESPONSE);
							return;
						}
						//$FALL-THROUGH$
					case RadiusProxylet.ACCEPT:
						int status = proxylet.doResponse(response);
						if (!isValidResponseStatus(status)) {
							messageError(status, chain, proxylet);
						}

						if (status == RadiusProxylet.SUSPEND) {
							suspend(response, ignoreMayBlock, listener);
							return;
						}

						progressInResponseChain(chain, response, status);
						break;
					default:
						acceptError(accept, chain, proxylet);
						break;
				}
			}
		}
		catch (ProxyletEngineException ee) {
			listener.messageProcessingError(response, ee);
			return;
		}
		catch (Throwable t) {
			listener.messageProcessingError(response, new ProxyletEngineException(chain, proxylet, t));
			return;
		}
		LOGGER.debug("handleResponse: no more proxylet to call -> returns Action.RESPONSE");
		listener.messageProcessed(response, Action.RESPONSE);
	}

	/**
	 * Returns REQUEST, RESPONSE, MAY_BLOCK_REQUEST, MAY_BLOCK_RESPONSE
	 */
	public void handleRequest(AccessRequestFacade request, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("handleRequest: ignoreMayBlock=" + ignoreMayBlock);
		}
		if (listener == null) {
			throw new IllegalArgumentException("no listener");
		}

		// we retrieve the Proxylet chain
		RadiusProxyletChain chain = getProxyletContainer().getChain(request);
		if (chain == null) {
			LOGGER.debug("handleRequest: no request chain -> returns Action.REQUEST");
			listener.messageProcessed(request, Action.REQUEST);
			return;
		}

		AccessRequestProxylet proxylet = null;
		try {
			while ((proxylet = (AccessRequestProxylet) chain.nextProxylet(request)) != null) {
				int accept = proxylet.accept(request);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("handleRequest: accept=" + accept + " for proxylet " + proxylet);
				}

				switch (accept) {
					case RadiusProxylet.IGNORE:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: ignore -> go to next");
						}
						chain.shift(request, 1);
						break;

					case RadiusProxylet.ACCEPT_MAY_BLOCK:
						if (!ignoreMayBlock) {
							listener.messageProcessed(request, Action.MAY_BLOCK_REQUEST);
							return;
						}
						//$FALL-THROUGH$

					case RadiusProxylet.ACCEPT:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: accept, call doRequest");
						}

						int status = processProxylet(proxylet, request);
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: status=" + status);
						}
						if (!isValidAccessRequestValue(status)) {
							messageError(status, chain, proxylet);
							return;
						}

						if (status == RadiusProxylet.SUSPEND) {
							suspend(request, ignoreMayBlock, listener);
							return;
						}

						if (status == AccountingRequestProxylet.NO_RESPONSE) {
							return;
						}

						boolean continueToProcessRequest = progressInRequestChain(chain, request, status, ignoreMayBlock, listener);
						if (!continueToProcessRequest) {
							return;
						}

						break;
					default:
						acceptError(accept, chain, proxylet);
				}
			}
		}
		catch (ProxyletEngineException ee) {
			listener.messageProcessingError(request, ee);
			return;
		}
		catch (Throwable t) {
			listener.messageProcessingError(request, new ProxyletEngineException(chain, proxylet, t));
			return;
		}
		listener.messageProcessed(request, Action.REQUEST);
	}

	/**
	 * Returns RESPONSE, MAY_BLOCK_RESPONSE
	 * 
	 * @param listener
	 */
	public void handleResponse(AccessResponseFacade response, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("no listener");
		}
		// we retrieve the Proxylet chain
		RadiusProxyletChain chain = getProxyletContainer().getChain(response);
		AccessResponseProxylet proxylet = null;
		try {
			while ((proxylet = (AccessResponseProxylet) chain.nextProxylet(response)) != null) {
				int accept = proxylet.accept(response);
				switch (accept) {
					case RadiusProxylet.IGNORE:
						chain.shift(response, 1);
						break;
					case RadiusProxylet.ACCEPT_MAY_BLOCK:
						if (!ignoreMayBlock) {
							listener.messageProcessed(response, Action.MAY_BLOCK_RESPONSE);
							return;
						}
						//$FALL-THROUGH$
					case RadiusProxylet.ACCEPT:
						int status = proxylet.doResponse(response);
						if (!isValidResponseStatus(status)) {
							messageError(status, chain, proxylet);
						}

						if (status == RadiusProxylet.SUSPEND) {
							suspend(response, ignoreMayBlock, listener);
							return;
						}

						progressInResponseChain(chain, response, status);
						break;
					default:
						acceptError(accept, chain, proxylet);
						break;
				}
			}
		}
		catch (ProxyletEngineException ee) {
			listener.messageProcessingError(response, ee);
			return;
		}
		catch (Throwable t) {
			listener.messageProcessingError(response, new ProxyletEngineException(chain, proxylet, t));
			return;
		}
		LOGGER.debug("handleResponse: no more proxylet to call -> returns Action.RESPONSE");
		listener.messageProcessed(response, Action.RESPONSE);
	}

	private boolean isValidResponseStatus(int status) {
		boolean res = (status == RadiusProxylet.FIRST_PROXYLET || status == RadiusProxylet.NEXT_PROXYLET || status == RadiusProxylet.LAST_PROXYLET
				|| status == RadiusProxylet.SAME_PROXYLET || status == RadiusProxylet.SUSPEND);
		return res;
	}

	private void progressInResponseChain(RadiusProxyletChain chain, ProxyletStateTracker response, int status) {
		if (status == RadiusProxylet.FIRST_PROXYLET) {
			chain.reset(response);
		} else if (status == RadiusProxylet.NEXT_PROXYLET) {
			chain.shift(response, 1);
		} else if (status == RadiusProxylet.LAST_PROXYLET) {
			chain.pad(response);
		} else if (status == RadiusProxylet.SAME_PROXYLET) {
			chain.shift(response, 0);
		}
	}

	private void acceptError(int accept, ProxyletChain chain, Proxylet proxylet)
		throws ProxyletEngineException {
		String text = "Invalid value returned from method accept: " + accept;
		throw new ProxyletEngineException(chain, proxylet, text);
	}

	private void messageError(int status, ProxyletChain chain, Proxylet proxylet)
		throws ProxyletEngineException {
		String text = "Invalid return code from message processing: " + status;
		throw new ProxyletEngineException(chain, proxylet, text);
	}

	private void suspend(RadiusMessageFacade message, boolean ignoreMayBlock, MessageProcessingListener listener) {
		// addPendingMessage;
		message.setAttribute(IGNORE_MAY_BLOCK_ATTRIBUTE, ignoreMayBlock);
		message.setAttribute(LISTENER_ATTRIBUTE, listener);
		LOGGER.debug("suspend: call AsyncProxyletManager");
		AsyncProxyletManager.suspend(message, message);
	}

	public void resume(RadiusMessageFacade message, int status) {
		Boolean ignoreMayBlock = (Boolean) message.removeAttribute(IGNORE_MAY_BLOCK_ATTRIBUTE);
		MessageProcessingListener listener = (MessageProcessingListener) message.removeAttribute(LISTENER_ATTRIBUTE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resume:  ignoreMayBlock=" + ignoreMayBlock + ", listener=" + listener);
		}

		if (message instanceof AccessRequestFacade) {
			resumeAccessRequest((AccessRequestFacade) message, status, ignoreMayBlock, listener);
		} else if (message instanceof AccountingRequestFacade) {
			resumeAccountingRequest((AccountingRequestFacade) message, status, ignoreMayBlock, listener);
		} else if (message instanceof AccessResponseFacade) {
			resumeAccessResponse((AccessResponseFacade) message, status, ignoreMayBlock, listener);
		} else if (message instanceof AccountingResponseFacade) {
			resumeAccountingResponse((AccountingResponseFacade) message, status, ignoreMayBlock, listener);
		} else {
			throw new IllegalArgumentException("Not supported message type: " + message);
		}
	}

	private void resumeAccessRequest(AccessRequestFacade request, int status, Boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (!isValidAccessRequestValue(status)) {
			throw new IllegalArgumentException("Not supported status value=" + status);
		}

		RadiusProxyletChain requestChain = null;
		RadiusProxyletContext context = (RadiusProxyletContext) request.getProxyletContext();
		if (context != null) {
			requestChain = context.getAccessRequestChain();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resumeAccessRequest:  chain=" + requestChain);
		}

		if (status == RadiusProxylet.SUSPEND) {
			suspend(request, ignoreMayBlock, listener);
			return;
		}

		boolean continueToProcessRequest = progressInRequestChain(requestChain, request, status, ignoreMayBlock, listener);
		if (continueToProcessRequest) {
			handleRequest(request, ignoreMayBlock, listener);
		}
	}

	private void resumeAccessResponse(AccessResponseFacade message, int status, Boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (!isValidResponseStatus(status)) {
			throw new IllegalArgumentException("Not supported status value=" + status);
		}

		RadiusProxyletChain chain = null;
		RadiusProxyletContext context = (RadiusProxyletContext) message.getProxyletContext();
		if (context != null) {
			chain = context.getAccessResponseChain();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resumeAccessResponse:  chain=" + chain);
		}

		if (status == RadiusProxylet.SUSPEND) {
			suspend(message, ignoreMayBlock, listener);
			return;
		}

		progressInResponseChain(chain, message, status);
		handleResponse(message, ignoreMayBlock, listener);
	}

	private void resumeAccountingRequest(AccountingRequestFacade request, int status, Boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (!isValidAccountingRequestValue(status)) {
			throw new IllegalArgumentException("Not supported status value=" + status);
		}

		RadiusProxyletChain chain = null;
		RadiusProxyletContext context = (RadiusProxyletContext) request.getProxyletContext();
		if (context != null) {
			chain = context.getAccountingRequestChain();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resumeAccountingRequest:  chain=" + chain);
		}

		if (status == RadiusProxylet.SUSPEND) {
			suspend(request, ignoreMayBlock, listener);
			return;
		}

		boolean continueToProcessRequest = progressInRequestChain(chain, request, status, ignoreMayBlock, listener);
		if (continueToProcessRequest) {
			handleRequest(request, ignoreMayBlock, listener);
		}
	}

	private void resumeAccountingResponse(AccountingResponseFacade message, int status, Boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (!isValidResponseStatus(status)) {
			throw new IllegalArgumentException("Not supported status value=" + status);
		}

		RadiusProxyletChain chain = null;
		RadiusProxyletContext context = (RadiusProxyletContext) message.getProxyletContext();
		if (context != null) {
			chain = context.getAccountingResponseChain();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resumeAccessResponse:  chain=" + chain);
		}

		if (status == RadiusProxylet.SUSPEND) {
			suspend(message, ignoreMayBlock, listener);
			return;
		}

		progressInResponseChain(chain, message, status);
		handleResponse(message, ignoreMayBlock, listener);
	}

}
