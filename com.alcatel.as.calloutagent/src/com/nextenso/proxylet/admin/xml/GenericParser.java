// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import alcatel.tess.hometop.gateways.utils.Charset;
import alcatel.tess.hometop.gateways.utils.Config;

import com.nextenso.proxylet.engine.xml.XMLUtils;

public class GenericParser extends XMLUtils implements /*PropValInterface,*/EntityResolver, ErrorHandler {
  static final Logger logger = Logger.getLogger("proxylet.admin");
  
  public String PUBLIC_ID;
  public String DTD;
  private InputSource DTD_INPUTSOURCE;
  
  protected Document xml;
  protected DocumentBuilder parser;
  
  public GenericParser(String dtd) {
    this("-//NEXTENSO//DTD CONTEXT 1.1//EN", dtd);
  }
  
  public GenericParser(String publicid, String dtd) {
    PUBLIC_ID = publicid;
    DTD = dtd;
    if (logger.isDebugEnabled())
      logger.debug("init " + toString());
    reset();
    parser = getDocumentBuilder();
    parser.setEntityResolver(this);
    parser.setErrorHandler(this);
  }
  
  public String toString() {
    return "GenericParser PUBLIC_ID=[" + PUBLIC_ID + "], DTD=[" + DTD + "]";
  }
  
  protected void reset() {
    xml = null;
  }
  
  public synchronized Document parseFile(String xmlFile) throws IOException, SAXException {
    InputSource is = new InputSource(Config.getResourceAsStream(xmlFile));
    reset();
    xml = parser.parse(is);
    return xml;
  }
  
  public synchronized Document parseString(String xmlString) throws IOException, SAXException {
    reset();
    InputStream xmlStream = new ByteArrayInputStream(Charset.makeBytes(xmlString));
    xml = parser.parse(xmlStream);
    return xml;
  }
  
  public synchronized Document parse(InputSource is) throws IOException, SAXException {
    reset();
    xml = parser.parse(is);
    return xml;
  }
  
  public String getPublicID() {
    return PUBLIC_ID;
  }
  
  public String getDTD() {
    return DTD;
  }
  
  /**********************************
   * Implementation of PropValInterface
   **********************************/
  
  public String isValueValid(String module, String instance, String name, Object value) {
    DocumentBuilder parser = getDocumentBuilder();
    parser.setEntityResolver(this);
    parser.setErrorHandler(this);
    try {
      parser.parse(new InputSource(new StringReader((String) value)));
    } catch (SAXParseException spe) {
      return ("XML Exception: " + spe.getMessage() + " at line " + spe.getLineNumber());
    } catch (Throwable t) {
      return ("Exception: " + t);
    }
    return null;
  }
  
  /**********************************
   * Implementation of EntityResolver
   **********************************/
  
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, java.io.IOException {
    if (PUBLIC_ID.equals(publicId)) {
      if (DTD_INPUTSOURCE == null) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(DTD);
        if (is == null)
          is = Config.getResourceAsStream(DTD);
        DTD_INPUTSOURCE = new InputSource(is);
      }
      return DTD_INPUTSOURCE;
    }
    throw new RuntimeException("Unknown publicId: " + publicId);
  }
  
  /**********************************
   * Implementation of ErrorHandler
   **********************************/
  
  public void error(SAXParseException e) throws SAXParseException {
    throw e;
  }
  
  public void fatalError(SAXParseException e) throws SAXParseException {
    throw e;
  }
  
  public void warning(SAXParseException e) throws SAXParseException {
    throw e;
  }
}
