// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.keycloak.jaxrs.adapter;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class WebXmlParser extends org.xml.sax.helpers.DefaultHandler {
	private final static Logger log = Logger.getLogger("" + JaxrsBearerTokenFilterImpl.class);

	public WebXml parse(File webXML) throws SAXException, IOException, ParserConfigurationException {
		setupParser().parse(webXML, this);
		return this.web;
	}

	public WebXml parse(String webXML) throws SAXException, IOException, ParserConfigurationException {
		setupParser().parse(new InputSource(new StringReader(webXML)), this);
		return this.web;
	}

	private SAXParser setupParser() throws ParserConfigurationException, SAXException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false); // We don't want validation
		factory.setNamespaceAware(false); // No namespaces please
		SAXParser parser = factory.newSAXParser();
		return parser;
	}

	StringBuffer accumulator; // Accumulate text
	WebXml web;
	SecurityConstraint constraint; // current Security Constraint
	WebResourceCollection collection; // current WebResourceCollection
	Set<String> authRoles; // current authRoles
	List<String> patterns; // current patterns
	List<String> methods; // current methods
	List<String> omittedMethods; // current omittedMethods

	String webResourceName, authConstraint; // Remember text

	// Called at the beginning of parsing. We use it as an init() method
	public void startDocument() {
		accumulator = new StringBuffer();
		web = new WebXml();
	}

	// When the parser encounters plain text (not XML elements), it calls
	// this method, which accumulates them in a string buffer.
	// Note that this method may be called multiple times, even with no
	// intervening elements.
	public void characters(char[] buffer, int start, int length) {
		accumulator.append(buffer, start, length);
	}

	// At the beginning of each new element, erase any accumulated text and setup
	// nested elements.
	public void startElement(String namespaceURL, String localName, String qname, Attributes attributes) {
		accumulator.setLength(0);
		if (qname.equals("security-constraint")) {
			constraint = new SecurityConstraint();
			constraint.setWebResourceCollection(new ArrayList<WebResourceCollection>());
			web.addConstraint(constraint);
		}
		if (qname.equals("web-resource-collection")) {
			collection = new WebResourceCollection();
			patterns = new ArrayList<>();
			methods = new ArrayList<String>();
			omittedMethods = new ArrayList<String>();
			collection.setPatterns(patterns);
			collection.setMethods(methods);
			collection.setOmittedMethods(omittedMethods);
			constraint.addWebResourceCollection(collection);
		}
		if (qname.equals("auth-constraint")) { // optional
			authRoles = new HashSet<>();
			constraint.setAuthRoles(authRoles);
		}
	}

	// Read values from elements
	public void endElement(String namespaceURL, String localName, String qname) {
		if (qname.equals("web-resource-name")) {
			collection.setName(accumulator.toString().trim());
		}
		if (qname.equals("url-pattern")) {
			patterns.add(accumulator.toString().trim());
		}
		if (qname.equals("role-name")) {
			authRoles.add(accumulator.toString().trim());
		}
		if (qname.equals("http-method")) {
			methods.add(accumulator.toString().trim());
		}
		if (qname.equals("http-method-omission")) {
			omittedMethods.add(accumulator.toString().trim());
		}
	}

	// Issue a warning
	public void warning(SAXParseException exception) {
		log.warn("WARNING: line " + exception.getLineNumber() + ": " + exception.getMessage());
	}

	// Report a parsing error
	public void error(SAXParseException exception) {
		log.error("ERROR: line " + exception.getLineNumber() + ": " + exception.getMessage());
	}

	// Report a non-recoverable error and exit
	public void fatalError(SAXParseException exception) throws SAXException {
		log.fatal("FATAL: line " + exception.getLineNumber() + ": " + exception.getMessage());
		throw (exception);
	}
}
