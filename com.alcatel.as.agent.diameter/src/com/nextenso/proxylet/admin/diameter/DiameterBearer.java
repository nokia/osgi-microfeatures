// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.diameter;

import static com.nextenso.proxylet.engine.ProxyletApplication.REQUEST_CHAIN;
import static com.nextenso.proxylet.engine.ProxyletApplication.REQUEST_LISTENER;
import static com.nextenso.proxylet.engine.ProxyletApplication.RESPONSE_CHAIN;
import static com.nextenso.proxylet.engine.ProxyletApplication.RESPONSE_LISTENER;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;

/**
 *
 */
public class DiameterBearer
		extends Bearer implements Bearer.Factory {

	static List _chainTypes = new ArrayList();
	static {
		_chainTypes.add(REQUEST_CHAIN);
		_chainTypes.add(RESPONSE_CHAIN);
	}

	static List _listenerTypes = new ArrayList();
	static {
		_listenerTypes.add(CONTEXT_LISTENER);
		_listenerTypes.add(REQUEST_LISTENER);
		_listenerTypes.add(RESPONSE_LISTENER);
	}

	public final static String APPLICATION = "application";
	public final static String ALL_APPLICATIONS = "all-applications";

	static List _criterionTypes = new ArrayList();
	static {
		_criterionTypes.add(FROM);
		_criterionTypes.add(UNTIL);
		_criterionTypes.add(DAY);
		_criterionTypes.add(DATE);
		_criterionTypes.add(MONTH);
		_criterionTypes.add(TIME);
		_criterionTypes.add(MESSAGE_ATTR);
		_criterionTypes.add(AND);
		_criterionTypes.add(OR);
		_criterionTypes.add(NOT);
		_criterionTypes.add(REFERENCE);
		_criterionTypes.add(ALL);
		_criterionTypes.add(APPLICATION);
		_criterionTypes.add(ALL_APPLICATIONS);
	}
	String _nextHop = null;

	/**
	 * Builds a new Diameter Bearer
	 */
	public DiameterBearer() {
		super(Protocol.DIAMETER);
	}

	/**
	 * Builds a new Diameter Bearer with a DOM Node.
	 * 
	 * @param node The node.
	 */
	public DiameterBearer(Node node) {
		this();
		setNode(node);
	}

        /**
         * implements Bearer.Factory
         */
        public Bearer newBearer(Node node) {
          return new DiameterBearer(node);
        }

	/**
	 * Gets the list of supported chain types.
	 * 
	 * @return The list of supported chain types (Strings).
	 */
	@Override
	public Iterator getChainTypes() {
		return _chainTypes.iterator();
	}

	/**
	 * Gets the list of supported listener types.
	 * 
	 * @return The list of supported listener types (Strings).
	 */
	@Override
	public Iterator getListenerTypes() {
		return _listenerTypes.iterator();
	}

	/**
	 * Gets the list of supported criterion types.
	 * 
	 * @return The list of supported criterion types.
	 */
	@Override
	public Iterator getCriterionTypes() {
		return _criterionTypes.iterator();
	}

}
