// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.diameter.agent.peer.Route;
import com.nextenso.proxylet.diameter.client.DiameterClient;

public class RoutesParser
		extends DiameterXMLUtils {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.parser.route");

	// TAGS
	//	private static final String TAG_ROUTES = "routes";
	//	private static final String TAG_DESTINATION_HOST = "destination-host";
	private static final String TAG_ROUTE = "route";
	private static final String TAG_DESTINATION_REALM = "destination-realm";
	private static final String TAG_APPLICATION = "application";
	private static final String TAG_ROUTING_PEER = "routing-peer";

	private static final String ATTR_ID = "id";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_METRICS = "metrics";

	private static final String ATTR_TYPE_VALUE_ACCT = "acct";
	private static final String ATTR_TYPE_VALUE_AUTH = "auth";

	private Document _document = null;

	public RoutesParser(String xml, String dtd)
			throws ConfigException {
		super();
		_document = parse(xml, dtd);
	}

	public List<Route> parseRoutes(String handlerName) {
		Element root = _document.getDocumentElement();

		ArrayList<Element> list = getChildElements(root, TAG_ROUTE);
		List<Route> routes = new ArrayList<Route>();
		for (Element route : list) {
			Element e = getChildElement(route, TAG_DESTINATION_REALM);
			String destRealm = e.getAttribute(ATTR_NAME);
			if (destRealm.equals("*")) {
				destRealm = null;
			}
			e = getChildElement(route, TAG_APPLICATION);
			String appId = e.getAttribute(ATTR_ID);
			if (appId.equals("*")) {
				appId = "-1";
			}

			String typeS = e.getAttribute(ATTR_TYPE);
			int type = DiameterClient.TYPE_ACCT | DiameterClient.TYPE_AUTH;
			if (ATTR_TYPE_VALUE_ACCT.equalsIgnoreCase(typeS)) {
				type = DiameterClient.TYPE_ACCT;
			} else if (ATTR_TYPE_VALUE_AUTH.equalsIgnoreCase(typeS)) {
				type = DiameterClient.TYPE_AUTH;
			}

			int metrics = Integer.MAX_VALUE;
			String s = route.getAttribute(ATTR_METRICS);
			try {
				if (s != null && ! "".equals(s)) {
					metrics = Integer.parseInt(s);
				}
			}
			catch (Exception parseException) {
				LOGGER.error("Bad weight value:" + s);
			}

			s = route.getAttribute(ATTR_ID);
			long routeId = -1;
			if (s != null && ! "".equals(s)) {
				routeId = parseLong(s);
			}

			String peerId = getChildElement(route, TAG_ROUTING_PEER).getAttribute(ATTR_ID);

			routes.add(new Route(handlerName, routeId, destRealm, parseLong(appId), parseLong(peerId), type, metrics));
		}
		return routes;
	}
}
