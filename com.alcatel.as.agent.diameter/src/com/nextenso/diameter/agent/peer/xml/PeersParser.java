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

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.LocalPeer;
import com.nextenso.diameter.agent.peer.StaticPeer;
import com.nextenso.proxylet.diameter.DiameterConnectionFilter;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;

public class PeersParser
		extends DiameterXMLUtils {

	//	private static final Logger LOGGER = Logger.getLogger("agent.diameter.peer.parser");

	// TAGS
	//	private  static final String TAG_PEERS = "peers";
	private static final String TAG_STATIC_PEER = "static-peer";
	private static final String TAG_DYNAMIC_PEER = "dynamic-peer";
	private static final String TAG_HOST = "host";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_CONNECTION = "connection";
	private static final String TAG_ORIGIN_HOST = "origin-host";
	private static final String TAG_ORIGIN_REALM = "origin-realm";

	private static final String ATTR_ID = "id";
	private static final String ATTR_HANDLER = "handler";
	private static final String ATTR_SECURE = "secure";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_IP = "ip";
	private static final String ATTR_PORT = "port";
	private static final String ATTR_RETRY_TIMEOUT = "retry-timeout";
	private static final String ATTR_NB_RETRIES = "nb-retries";
	private static final String ATTR_PROTOCOL = "protocol";
	private static final String ATTR_QUARANTINE = "quarantine";

	private static final String YES = "yes";
	private static final String NO = "no";

	private static String DTD_CONTEXT = "diameterAgent/diameterPeers.dtd";
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.peerparser");

	private Document _document = null;

	public PeersParser()
			throws ConfigException {
		super();
		String xml = DiameterProperties.getPeersXml();
		_document = parse(xml, DTD_CONTEXT);
	}

	/**
	 * Parses the static peers.
	 * 
	 * @param localPeer The local peer.
	 * 
	 * @return The peers.
	 */
	public List<StaticPeer> parseStaticPeers(LocalPeer localPeer) {
		List<StaticPeer> staticPeers = new ArrayList<StaticPeer>();
		if (localPeer == null) {
			return staticPeers;
		}

		Element root = _document.getDocumentElement();

		ArrayList<Element> peers = getChildElements(root, TAG_STATIC_PEER);
		for (Element peerElt : peers) {
			Element originHostElt = getChildElement(peerElt, TAG_ORIGIN_HOST);
			String originHost = originHostElt.getAttribute(ATTR_NAME);

			String att = peerElt.getAttribute(ATTR_ID);
			String handler = peerElt.getAttribute(ATTR_HANDLER);
			if (handler != null && handler.length () > 0 && !handler.equals (localPeer.getHandlerName ()))
				continue;
			long id = parseLong(att);
			if (originHost.equalsIgnoreCase(localPeer.getOriginHost())) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("parseStaticPeers: this peer is the local peer -> set the id and do not add to static peer list");
				}
				localPeer.setId(id);
				continue;
			}

			Element hostElt = getChildElement(peerElt, TAG_HOST);
			att = hostElt.getAttribute(ATTR_PROTOCOL);
			Protocol protocol = Protocol.TCP;
			if (att != null) {
				if (att.equalsIgnoreCase("tcp")) {
					protocol = Protocol.TCP;
				} else if (att.equalsIgnoreCase("sctp")) {
					protocol = Protocol.SCTP;
				}
			}

			String hostname = hostElt.getAttribute(ATTR_NAME);
			String portAtt = hostElt.getAttribute(ATTR_PORT);
			if (portAtt.length() == 0) { // no defined port
				portAtt = String.valueOf(DiameterPeer.DIAMETER_PORT);
			}
			int port = Integer.parseInt(portAtt);
			String secure = hostElt.getAttribute(ATTR_SECURE);
			if (secure == null) {
				secure = NO;
			}
			boolean isSecure = secure.equalsIgnoreCase(YES);
			
			List<String> hosts = new ArrayList<String> (2);
			java.util.StringTokenizer st = new java.util.StringTokenizer (hostname, " ,", false);
			while (st.hasMoreTokens ()) hosts.add (st.nextToken ());
			StaticPeer peerToAdd = new StaticPeer(localPeer, id, originHost, hosts, port, isSecure, protocol, true);
			staticPeers.add(peerToAdd);

			att = peerElt.getAttribute(ATTR_NB_RETRIES);
			if (att.length() > 0) {
				peerToAdd.setNbRetries(parseint(att));
			}
			att = peerElt.getAttribute(ATTR_RETRY_TIMEOUT);
			if (att.length() > 0) {
				peerToAdd.setRetryTimeout(parseint(att));
			}

			att = peerElt.getAttribute(ATTR_QUARANTINE);
			if (att.length() > 0) {
				peerToAdd.setQuarantineDelay(parseLong(att));
			}

			Element srcElt = getChildElement(peerElt, TAG_SOURCE);
			if (srcElt != null){
				String srcip = srcElt.getAttribute(ATTR_IP);
				String srcport = srcElt.getAttribute(ATTR_PORT);
				String[] srcips = null;
				
				int srcportN = -1;
				if (srcport != null && srcport.length () > 0) srcportN = Integer.parseInt (srcport);
				List<String> ips = null;
				if (srcip != null && srcip.length () > 0){
					ips = new ArrayList<String> (2);
					st = new java.util.StringTokenizer (srcip, " ,", false);
					while (st.hasMoreTokens ()) ips.add (st.nextToken ());
					srcips = ips.toArray (new String[0]);
				}
				if (LOGGER.isDebugEnabled ())
				    LOGGER.debug ("parseStaticPeers : found SOURCE tag : originHost="+originHost+", src-ips="+ips+", src-port="+srcportN);
				peerToAdd.setSrc (srcips, srcportN);
			}

		}
		return staticPeers;
	}

	public List<DiameterConnectionFilter> parseWhiteListFilters() {
		List<DiameterConnectionFilter> res = new ArrayList<DiameterConnectionFilter>();

		Element root = _document.getDocumentElement();
		ArrayList<Element> peers = getChildElements(root, TAG_DYNAMIC_PEER);
		for (Element peer : peers) {

			String origin = null;
			Element e = getChildElement(peer, TAG_ORIGIN_HOST);
			if (e != null) {
				origin = e.getAttribute(ATTR_NAME);
			}

			String realm = null;
			e = getChildElement(peer, TAG_ORIGIN_REALM);
			if (e != null) {
				realm = e.getAttribute(ATTR_NAME);
			}

			Element connection = getChildElement(peer, TAG_CONNECTION);
			String secure = connection.getAttribute(ATTR_SECURE);
			int encLevel = Utils.getEncLevel(secure);

			DiameterConnectionFilter dPeer = new DiameterConnectionFilter(origin, realm, encLevel);
			res.add(dPeer);
		}
		return res;
	}

}
