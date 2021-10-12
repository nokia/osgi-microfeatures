// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import alcatel.tess.hometop.gateways.utils.Charset;

import com.nextenso.proxylet.admin.xml.GenericParser;

/**
   This class represents a fastcache entry in the proxylet admin table.
 */
public class ProxyletAdminEntry {
  private String key;
  private Object value;
  private Bearer bearer;
  private Protocol protocol;
  private String groupName;
  
  public ProxyletAdminEntry(byte[] key, byte[] value) throws IllegalArgumentException {
    int index;
    String sKey = Charset.makeString(key, 0, key.length);
    this.key = sKey;
    index = sKey.indexOf("/");
    if (index > 0)
      groupName = sKey.substring(0, index);
    else
      throw new IllegalArgumentException("No group name found in proxylet admin entry key: " + sKey);
    
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(value);
      ObjectInputStream ois = new ObjectInputStream(bais);
      this.value = ois.readObject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot read Proxylet admin entry value for key: " + this.key);
    }
    
    if (isBearerContext())
      initBearerData();
  }
  
  /**
     Initializes data related to the bearer.
   */
  private void initBearerData() throws IllegalArgumentException {
    String sProtocol;
    int dotIndex = key.lastIndexOf('.');
    if (dotIndex < 0)
      throw new IllegalArgumentException("No protocol information found in proxylet admin entry with key: "
          + key);
    
    sProtocol = key.substring(dotIndex + 1);
    this.protocol = Protocol.getProtocolInstance(sProtocol);
    if (this.protocol == null)
      throw new IllegalArgumentException("Unknown protocol found in proxylet admin entry with key: " + key);
    
    // Parses value
    GenericParser myparser = this.protocol.getParserInstance();
    if (myparser == null)
      return;
    if (((String) value).indexOf("PUBLIC \"null\"") > 0)
      return; //skip fake DB entries!
    try {
      Document xml = myparser.parseString((String) value);
      Bearer myBearer = protocol.getBearerInstance();
      myBearer.setNode(xml.getDocumentElement());
      this.bearer = myBearer;
    } catch (SAXParseException sp) {
      throw new IllegalArgumentException("*** ProxyletAdminEntry *** initBearerData **** " + protocol
          + " Context Parser SAX parse exception [system id=" + sp.getSystemId() + ",column="
          + sp.getColumnNumber() + ",line=" + sp.getLineNumber() + "]", sp);
    } catch (SAXException se) {
      throw new IllegalArgumentException("*** ProxyletAdminEntry *** initBearerData **** " + protocol
          + " Context Parser SAX exception " + se.getMessage() + ", exception is " + se.toString(), se);
    } catch (Throwable t) {
      throw new IllegalArgumentException("*** ProxyletAdminEntry *** initBearerData **** " + protocol
          + " Context Parser IO ERROR " + t.getMessage() + ", exception is " + t.toString(), t);
    }
  }
  
  /**
     Indicates if value contains a bearer context.
  */
  public boolean isBearerContext() {
    return (key.indexOf(PxletUtils.DEPLOYED_BEARER_CONTEXT_KEY_PROPERTY) > 0);
  }
  
  /**
     Indicates if value contains a proxy application list.
  */
  public boolean isProxyAppList() {
    return (key.indexOf(PxletUtils.DEPLOYED_PROXYLETSET_ID_LIST_PROPERTY_KEY) > 0);
  }
  
  public Protocol getBearerProtocol() {
    return protocol;
  }
  
  public Object getValue() {
    return value;
  }
  
  public Bearer getBearer() {
    return bearer;
  }
  
  public String getGroupName() {
    return groupName;
  }
}
