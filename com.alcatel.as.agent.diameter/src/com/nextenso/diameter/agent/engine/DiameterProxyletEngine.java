// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.engine;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.peer.LocalPeer;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.diameter.DiameterProxylet;
import com.nextenso.proxylet.diameter.DiameterRequestProxylet;
import com.nextenso.proxylet.diameter.DiameterResponseProxylet;
import com.nextenso.proxylet.engine.AsyncProxyletManager;
import com.nextenso.proxylet.engine.ProxyletChain.ProxyletStateTracker;
import com.nextenso.proxylet.engine.ProxyletEngine;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.proxylet.engine.ProxyletUtils;

public class DiameterProxyletEngine
		extends ProxyletEngine {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.engine");
	private static final Object IGNORE_MAY_BLOCK_ATTRIBUTE = "agent.diameter.IgnoreMayBlock";
	private static final Object LISTENER_ATTRIBUTE = "agent.diameter.listener";

	public enum Action {
		REQUEST,
		MAY_BLOCK_REQUEST,
		RESPONSE,
		MAY_BLOCK_RESPONSE
	}

	private DiameterProxyletContext _context;
	private boolean _isUsingLicense;

	public DiameterProxyletEngine(DiameterProxyletContext context) {
		_context = context;
	}

	/**
	 * Check if license is valid for all the deployed proxylets.
	 * 
	 * @throws NoValidLicenseException if at least one deployed proxylet has no
	 *           valid license.
	 */
	@Override
	public final void checkLicense() {
	}

	public void resume(DiameterMessageFacade message, int status) {
		Boolean ignoreMayBlock = (Boolean) message.removeAttribute(IGNORE_MAY_BLOCK_ATTRIBUTE);
		MessageProcessingListener listener = (MessageProcessingListener) message.removeAttribute(LISTENER_ATTRIBUTE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resume:  ignoreMayBlock=" + ignoreMayBlock + ", listener=" + listener);
		}

		if (message instanceof DiameterRequestFacade) {
			resumeRequest((DiameterRequestFacade) message, status, ignoreMayBlock, listener);
		} else if (message instanceof DiameterResponseFacade) {
			resumeResponse((DiameterResponseFacade) message, status, ignoreMayBlock, listener);
		} else {
			throw new IllegalArgumentException("Not supported message type: " + message);
		}
	}

	public void resumeRequest(DiameterRequestFacade request, int status, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (!isValidRequestValue (status)) throw new IllegalArgumentException("Not supported status value=" + status);

		if (status == DiameterRequestProxylet.SUSPEND) { // does not make much sense though....
			suspend(request, ignoreMayBlock, listener);
			return;
		}
		if (status == DiameterRequestProxylet.NO_RESPONSE) {
			listener.messageProcessed(request, null);
			return;
		}
		
		DiameterProxyletChain requestChain = null;
		if (_context != null) {
			requestChain = _context.getRequestChain();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resumeRequest:  chain=" + requestChain);
		}

		boolean continueToProcessRequest = progressInRequestChain(requestChain, request, status, ignoreMayBlock, listener);

		if (continueToProcessRequest) {
			handleRequest(request, ignoreMayBlock, listener);
		}
	}

	/**
	 */
	public void resumeResponse(DiameterResponseFacade response, int status, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (status != DiameterResponseProxylet.FIRST_PROXYLET && status != DiameterResponseProxylet.NEXT_PROXYLET
				&& status != DiameterResponseProxylet.LAST_PROXYLET && status != DiameterResponseProxylet.SAME_PROXYLET && status != DiameterResponseProxylet.REDIRECT_FIRST_PROXYLET && status != DiameterResponseProxylet.REDIRECT_LAST_PROXYLET) {
			throw new IllegalArgumentException("not supported status value=" + status);
		}

		DiameterProxyletChain chain = null;
		if (_context != null) {
			chain = _context.getResponseChain();
		}
		if (chain != null) {
		    if (!progressInResponseChain(chain, response, status, ignoreMayBlock, listener))
			return;
		}

		handleResponse(response, ignoreMayBlock, listener);
	}

	/**
	 * Handles the request.
	 */
	public void handleRequest(DiameterRequestFacade request, boolean ignoreMayBlock, MessageProcessingListener listener) {
		LOGGER.debug("handleRequest...");
		if (listener == null) {
			throw new IllegalArgumentException("no listener");
		}

		// we retrieve the Proxylet chain
		DiameterProxyletChain requestChain = null;
		if (_context != null) {
			requestChain = _context.getRequestChain();
		}
		if (requestChain == null) {
			LOGGER.debug("handleRequest: no request chain -> returns Action.REQUEST");
			listener.messageProcessed(request, Action.REQUEST);
			return;
		}

		DiameterRequestProxylet proxylet = null;

		try {
			while ((proxylet = (DiameterRequestProxylet) requestChain.nextProxylet(request)) != null) {
				int accept = proxylet.accept(request);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("handleRequest: pxlet=" + proxylet.getProxyletInfo() + ", accept=" + accept);
				}
				
				// Store some attributes, in case the proxylet suspends the message
				storeSuspendInfo(request, ignoreMayBlock, listener);

				switch (accept) {
					case DiameterProxylet.IGNORE: {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: ignore");
						}
						requestChain.shift(request, 1);
						break;
					}
					case DiameterProxylet.ACCEPT_MAY_BLOCK:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: may block, ignore=" + ignoreMayBlock);
						}
						if (!ignoreMayBlock) {
							listener.messageProcessed(request, Action.MAY_BLOCK_REQUEST);
							return;
						}
						//$FALL-THROUGH$
					case DiameterProxylet.ACCEPT: {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: accept, call doRequest");
						}

						int status = processProxylet(proxylet, request);
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleRequest: status=" + status);
						}

						if (!isValidRequestValue(status)) {
							messageError(status, requestChain, proxylet, request, listener);
							return;
						}

						if (status == DiameterRequestProxylet.SUSPEND) {
							suspend(request, ignoreMayBlock, listener);
							return;
						}
						
						if (status == DiameterRequestProxylet.NO_RESPONSE) {
							listener.messageProcessed(request, null);
							return;
						}

						boolean continueToProcessRequest = progressInRequestChain(requestChain, request, status, ignoreMayBlock, listener);
						if (!continueToProcessRequest) {
							return;
						}
						break;
					}
					default:
						acceptError(accept, requestChain, proxylet, request, listener);
						return;
				}
			}

			LOGGER.debug("handleRequest: no more proxylet to call -> returns Action.REQUEST");
			listener.messageProcessed(request, Action.REQUEST);
		}
		catch (Throwable t) {
			listener.messageProcessingError(request, new ProxyletEngineException(requestChain, proxylet, t));
			return;
		}
	}

	/**
	 * Setup some attributes in the message, before calling doRequest/doResponse. These infos are needed
	 * in case the application is suspending the message (see resume method).
	 */
	private void storeSuspendInfo(DiameterMessageFacade request, boolean ignoreMayBlock,
        MessageProcessingListener listener)
    {
        request.setAttribute(IGNORE_MAY_BLOCK_ATTRIBUTE, ignoreMayBlock);
        request.setAttribute(LISTENER_ATTRIBUTE, listener);
    }

    /**
	 * Suspends
	 * 
	 * @param listener
	 */
	private void suspend(DiameterMessageFacade message, boolean ignoreMayBlock, MessageProcessingListener listener) {
		Utils.addPendingMessage();

		// IGNORE_MAY_BLOCK_ATTRIBUTE and LISTENER_ATTRIBUTE have already been set.
		LOGGER.debug("suspend: call AsyncProxyletManager");
		AsyncProxyletManager.suspend(message, message);
	}

	private int processProxylet(DiameterRequestProxylet proxylet, DiameterRequestFacade request) {
		int status = proxylet.doRequest(request);
		if (_isUsingLicense) {
			processedProxylet(proxylet);
		}
		return status;
	}

	private boolean isValidRequestValue(int status) {
	    switch (status){
	    case DiameterRequestProxylet.FIRST_PROXYLET:
	    case DiameterRequestProxylet.NEXT_PROXYLET:
	    case DiameterRequestProxylet.LAST_PROXYLET:
	    case DiameterRequestProxylet.SAME_PROXYLET:
	    case DiameterRequestProxylet.RESPOND_FIRST_PROXYLET:
	    case DiameterRequestProxylet.RESPOND_LAST_PROXYLET:
	    case DiameterRequestProxylet.SUSPEND:
	    case DiameterRequestProxylet.NO_RESPONSE:
		return true;
	    }
	    return false;
	}

	private boolean progressInRequestChain(DiameterProxyletChain chain, DiameterRequestFacade request, int status, boolean ignoreMayBlock,
			MessageProcessingListener listener) {
		ProxyletStateTracker tracker = request;
		if (status == DiameterRequestProxylet.FIRST_PROXYLET) {
			chain.reset(tracker);
		} else if (status == DiameterRequestProxylet.NEXT_PROXYLET) {
			chain.shift(tracker, 1);
		} else if (status == DiameterRequestProxylet.LAST_PROXYLET) {
			chain.pad(tracker);
		} else if (status == DiameterRequestProxylet.SAME_PROXYLET) {
			chain.shift(tracker, 0);
		} else if (status == DiameterRequestProxylet.RESPOND_FIRST_PROXYLET) {
			DiameterResponseFacade response = (DiameterResponseFacade) request.getResponse();
			DiameterProxyletChain responseChain = _context.getResponseChain();
			responseChain.reset(response);
			attachServerPeer(request);
			response.setLocalOrigin(true);
			handleResponse(response, ignoreMayBlock, listener);
			return false;
		} else if (status == DiameterRequestProxylet.RESPOND_LAST_PROXYLET) {
			attachServerPeer(request);
			request.getResponseFacade ().setLocalOrigin(true);
			listener.messageProcessed(request, Action.RESPONSE);
			return false;
		}

		return true;
	}

	private boolean isValidResponseValue(int status) {
	    switch (status){
	    case DiameterResponseProxylet.FIRST_PROXYLET:
	    case DiameterResponseProxylet.NEXT_PROXYLET:
	    case DiameterResponseProxylet.LAST_PROXYLET:
	    case DiameterResponseProxylet.SAME_PROXYLET:
	    case DiameterResponseProxylet.SUSPEND:
	    case DiameterResponseProxylet.REDIRECT_FIRST_PROXYLET:
	    case DiameterResponseProxylet.REDIRECT_LAST_PROXYLET:
		return true;
	    }
	    return false;
	}

	/**
	 * Sets the server peer of the request.
	 * 
	 * @param request The request.
	 */
	private void attachServerPeer(DiameterRequestFacade request) {
		if (request != null && request.getServerPeer() == null) {
			LocalPeer localPeer = Utils.getClientLocalPeer(request.getHandlerName());
			request.setServerPeer(localPeer);
		}
	}

	/**
	 *
	 */
	public void handleResponse(DiameterResponseFacade response, boolean ignoreMayBlock, MessageProcessingListener listener) {

		// we retrieve the Proxylet chain
		DiameterProxyletChain responseChain = _context.getResponseChain();
		if (responseChain == null) {
			listener.messageProcessed(response, Action.RESPONSE);
			return;
		}

		DiameterResponseProxylet proxylet = null;
		try {
			while ((proxylet = (DiameterResponseProxylet) responseChain.nextProxylet(response)) != null) {
				int accept = proxylet.accept(response);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("handleResponse: pxlet=" + proxylet.getProxyletInfo() + ", accept=" + accept);
				}

                // Store some attributes, in case the proxylet suspends the message
                storeSuspendInfo(response, ignoreMayBlock, listener);

				switch (accept) {
					case DiameterProxylet.IGNORE: {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleResponse: ignore");
						}
						responseChain.shift(response, 1);
						break;
					}
					case DiameterProxylet.ACCEPT_MAY_BLOCK:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleResponse: may block, ignore=" + ignoreMayBlock);
						}
						if (!ignoreMayBlock) {
							listener.messageProcessed(response, Action.MAY_BLOCK_RESPONSE);
							return;
						}
						//$FALL-THROUGH$
					case DiameterProxylet.ACCEPT: {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("handleResponse: accept, call doResponse");
						}
						int status = proxylet.doResponse(response);
						if (!isValidResponseValue(status)) {
							messageError(status, responseChain, proxylet, response, listener);
							return;
						}

						if (status == DiameterResponseProxylet.SUSPEND) {
							suspend(response, ignoreMayBlock, listener);
							return;
						}
						
						boolean continueToProcessResponse = progressInResponseChain(responseChain, response, status, ignoreMayBlock, listener);
						if (!continueToProcessResponse)
						    return;
						break;
					}
					default:
						acceptError(accept, responseChain, proxylet, response, listener);
						return;
				}
			}

			LOGGER.debug("handleResponse: no more proxylet to call -> returns Action.RESPONSE");
			listener.messageProcessed(response, Action.RESPONSE);
		}
		catch (Throwable t) {
			listener.messageProcessingError(response, new ProxyletEngineException(responseChain, proxylet, t));
			return;
		}
	}

	private boolean progressInResponseChain(DiameterProxyletChain responseChain, DiameterResponseFacade response, int status, boolean ignoreMayBlock, MessageProcessingListener listener) {
		if (status == DiameterResponseProxylet.FIRST_PROXYLET) {
			responseChain.reset(response);
			return true;
		} else if (status == DiameterResponseProxylet.NEXT_PROXYLET) {
			responseChain.shift(response, 1);
			return true;
		} else if (status == DiameterResponseProxylet.LAST_PROXYLET) {
			responseChain.pad(response);
			return true;
		} else if (status == DiameterResponseProxylet.SAME_PROXYLET) {
			responseChain.shift(response, 0);
			return true;
		} else if (status == DiameterResponseProxylet.REDIRECT_FIRST_PROXYLET){
		    _context.getRequestChain ().reset (response.getRequestFacade ());
		} else if (status == DiameterResponseProxylet.REDIRECT_LAST_PROXYLET){
		    _context.getRequestChain ().pad (response.getRequestFacade ());
		}
		// case of redirect : we need to reset the response
		responseChain.reset(response);
		response.resetForRedirect ();
		handleRequest(response.getRequestFacade (), ignoreMayBlock, listener);
		return false;
	}

	/**
	 * 
	 * @param accept
	 * @param chain
	 * @param proxylet
	 * @param listener
	 * @throws ProxyletEngineException
	 */
	private void acceptError(int accept, DiameterProxyletChain chain, Proxylet proxylet, DiameterMessageFacade message,
			MessageProcessingListener listener) {
		String text = "Invalid value returned from method accept: " + accept;
		listener.messageProcessingError(message, new ProxyletEngineException(chain, proxylet, text));
	}

	/**
	 */
	private void messageError(int status, DiameterProxyletChain chain, Proxylet proxylet, DiameterMessageFacade message,
			MessageProcessingListener listener) {
		String text = "Invalid return code from message processing: " + status;
		listener.messageProcessingError(message, new ProxyletEngineException(chain, proxylet, text));
	}

}
