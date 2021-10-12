// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.xml;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.proxylet.engine.xml.XMLUtils;

public class DiameterXMLUtils
		extends XMLUtils {

	public static final String PUBLIC_ID_CONTEXT = "-//NEXTENSO//DTD CONTEXT 1.1//EN";

	private String _dtd;

	protected DiameterXMLUtils() {}

	/**
	 * 
	 * @param cnf
	 * @param propName
	 * @param dtd
	 * @return
	 * @throws XMLConfigException
	 */
	protected Document parse(Config cnf, String propName, String dtd)
		throws ConfigException {
		String xml = null;
		try {
			xml = cnf.getString(propName);
		}
		catch (Throwable t) {
			throw new ConfigException("Cannot load configuration " + propName + " in " + cnf, t);
		}
		return parse(xml, dtd);
	}

	protected Document parse(String xmlString, String dtd)
		throws ConfigException {
		_dtd = dtd;
		try {
			DocumentBuilder parser = getDocumentBuilder();
			parser.setEntityResolver(this);
			parser.setErrorHandler(this);
			return parser.parse(new InputSource(new StringReader(xmlString)));
		}
		catch (SAXParseException spe) {
			throw new ConfigException("XML Exception while parsing the configuration file: " + spe + " at line " + spe.getLineNumber() + " in "+xmlString);
		}
		catch (Throwable t) {
			throw new ConfigException("Exception while parsing the configuration file: "+xmlString, t);
		}
		finally {
			_dtd = null;
		}
	}

	protected static ArrayList<Element> getChildElements(Element e, String tagName) {
		ArrayList<Element> children = new ArrayList<Element>();
		NodeList list = e.getChildNodes();
		int l = list.getLength();
		for (int i = 0; i < l; i++) {
			Node n = list.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element elt = (Element) n;
			if (elt.getTagName().equalsIgnoreCase(tagName))
				children.add(elt);
		}
		return children;
	}

	protected static Element getChildElement(Element e, String tagName) {
		NodeList list = e.getChildNodes();
		int l = list.getLength();
		for (int i = 0; i < l; i++) {
			Node n = list.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if (((Element) n).getTagName().equalsIgnoreCase(tagName))
				return (Element) n;
		}
		return null;
	}

	protected static Element getChildElement(Element e) {
		NodeList list = e.getChildNodes();
		int l = list.getLength();
		for (int i = 0; i < l; i++) {
			Node n = list.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			return (Element) n;
		}
		return null;
	}

	protected static long parseLong(String l) {
		if (l.startsWith("0x"))
			return Long.parseLong(l, 16);

		return Long.parseLong(l);
	}

	protected static int parseint(String l) {
		if (l.startsWith("0x"))
			return Integer.parseInt(l, 16);

		return Integer.parseInt(l);
	}

	/**
	 * @see com.nextenso.proxylet.engine.xml.XMLUtils#resolveEntity(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId)
		throws SAXException, java.io.IOException {
		if (PUBLIC_ID_CONTEXT.equals(publicId))
			return new InputSource(this.getClass().getClassLoader().getResourceAsStream(_dtd));

		throw new RuntimeException("Unknown publicId: " + publicId);

	}

}
