// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.Utils;

import com.nextenso.proxylet.admin.xml.GenericParser;

/**
 * The authorized protocols for bearers
 */
public class Protocol {
  
  final static Logger _logger = Logger.getLogger("as.proxylet.admin");
  
  // well-known protocols
  public static final String HTTP_STR = "HTTP";
  public static final String SMS_STR = "SMS";
  public static final String SMPP_STR = "SMPP";
  public static final String RADIUS_STR = "RADIUS";
  public static final String DIAMETER_STR = "DIAMETER";
  public static final String SMTP_STR = "SMTP";
  public static final String WSP_STR = "WSP";
  public static final String TCP_STR = "TCP";
  public static final String WAP_STR = "WSPGW";
  public static final String SIP_STR = "SIP";
  public static final String RTSP_STR = "RTSP";
  public static final String WEB_STR = "WEB";
  public static final String DS_STR = "SESSIONS";
  public static final String REGISTRY_STR = "REGISTRY";
  
  /**
   * The HTTP Protocol
   */
  public static final Protocol HTTP = new Protocol(HTTP_STR);
  
  /**
   * The SMS Protocol
   */
  public static final Protocol SMS = new Protocol(SMS_STR);
  
  /**
   * The SMPP Protocol
   */
  public static final Protocol SMPP = new Protocol(SMPP_STR);
  
  /**
   * The RADIUS Protocol
   */
  public static final Protocol RADIUS = new Protocol(RADIUS_STR);
  
  /**
   * The DIAMETER Protocol
   */
  public static final Protocol DIAMETER = new Protocol(DIAMETER_STR);
  
  /**
   * The SMTP Protocol
   */
  public static final Protocol SMTP = new Protocol(SMTP_STR);
  /**
   * The WSP Protocol
   */
  public static final Protocol WSP = new Protocol(WSP_STR);
  /**
   * The TCP Protocol
   */
  public static final Protocol TCP = new Protocol(TCP_STR);
  /**
   * The WAP Protocol
   */
  public static final Protocol WAP = new Protocol(WAP_STR);
  /**
   * The SIP Protocol
   */
  public static final Protocol SIP = new Protocol(SIP_STR);
  /**
  * The RTSP Protocol
  */
  public static final Protocol RTSP = new Protocol(RTSP_STR);
  /**
   * The WEB Protocol
   */
  public static final Protocol WEB = new Protocol(WEB_STR);
  /**
   * The "DistributedSession" Protocol
   */
  public static final Protocol DS = new Protocol(DS_STR);
  /**
   * The "Registry" Protocol
   */
  public static final Protocol REGISTRY = new Protocol(REGISTRY_STR);
  
  private String _id = null;
  private static TreeMap<String, Protocol> _allProtocols = new TreeMap<String, Protocol>();
  static {
    // add well-known protocols
    _allProtocols.put(HTTP_STR, Protocol.HTTP);
    _allProtocols.put(SMS_STR, Protocol.SMS);
    _allProtocols.put(SMPP_STR, Protocol.SMPP);
    _allProtocols.put(RADIUS_STR, Protocol.RADIUS);
    _allProtocols.put(DIAMETER_STR, Protocol.DIAMETER);
    _allProtocols.put(SMTP_STR, Protocol.SMTP);
    _allProtocols.put(WSP_STR, Protocol.WSP);
    _allProtocols.put(TCP_STR, Protocol.TCP);
    _allProtocols.put(WAP_STR, Protocol.WAP);
    _allProtocols.put(SIP_STR, Protocol.SIP);
    _allProtocols.put(RTSP_STR, Protocol.RTSP);
    _allProtocols.put(WEB_STR, Protocol.WEB);
    _allProtocols.put(DS_STR, Protocol.DS);
    _allProtocols.put(REGISTRY_STR, Protocol.REGISTRY);
    
    // find extra protocols from classpath
    try {
      URL url = ClassLoader.getSystemResource("calloutAgent");
      if (url != null) {
        File dir = new File(url.toURI());
        if (dir.isDirectory()) {
          FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return name.endsWith(".desc");
            }
          };
          File[] children = dir.listFiles(filter);
          for (int i = 0; i < children.length; i++) {
            File propsFile = children[i];
            if (propsFile.isFile() && propsFile.canRead()) {
              Properties props = new Properties();
              try {
                props.load(new FileInputStream(propsFile));
                String protocolName = props.getProperty("protocol");
                if (protocolName != null) {
                  addProtocol (protocolName) ;
                }
              } catch (FileNotFoundException ignored) {
              } catch (IOException ignored) {
              }
            }
          }
        }
      }
    } catch (Exception e) {
    }
  }

  public static Protocol addProtocol (String protocolName) {
    protocolName = protocolName.toUpperCase();
    Protocol p = _allProtocols.get(protocolName) ;
    if (p == null) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("Found extra protocol " + protocolName);
      }
      p = new Protocol(protocolName);
      _allProtocols.put(protocolName, p) ;
    }
    return p ;
  }
  
  private Protocol(String id) {
    _id = id;
  }
  
  /**
   * Returns a String representation of the protocol.
   * @return The String representation.
   */
  public String toString() {
    return _id;
  }
  
  /**
   * Gets the list of all existing protocols.
   * @return The list of all existing protocols.
   */
  public static Iterator getProtocols() {
    return _allProtocols.values().iterator();
  }
  
  /**
   * Gets a Protocol object given a String.
   * @param protocol a Protocol String
   * @return The Protocol Object or null if no valid protocol exists corresponding to this String.
   */
  public static Protocol getProtocolInstance(String protocol) {
    protocol = protocol.toUpperCase() ;
    return _allProtocols.get(protocol);
  }
  
  /**
   * Gets a Parser instance for the Protocol.
   * @return The GenericParser object instance corresponding to Protocol or null if no Parser is known for this Protocol.
   */
  public GenericParser getParserInstance() {
    String locase = toString().toLowerCase();
    if ("smpp".equals(locase))
      locase = "sms"; //ugly special cases.. FIXME change DTD name!
    else if ("smtp".equals(locase))
      locase = "mail";
    String  dtd = "com/nextenso/proxylet/admin/dtd/" + locase + "Context.dtd";
    
    // Not sure why this check is done here, since the dtd is also loaded from the GenericParser constructor ...
    
    if (Utils.getResource(dtd, new ClassLoader[] {
        Thread.currentThread().getContextClassLoader(),
        getClass().getClassLoader(),
        ClassLoader.getSystemClassLoader()        
    }) != null) {
      return new GenericParser(dtd);
    }
    _logger.info("dtd not found " + dtd + ". ignore.");
    return null;
  }
  
  /**
   * Gets the Context DTD PUBLIC ID for the Protocol.
   * @return The Context DTD PUBLIC ID corresponding to the Protocol or null if no Context DTD PUBLIC ID is known for this Protocol.
   */
  public String getPublicID() {
    GenericParser myParser = getParserInstance();
    if (myParser == null) {
      return null;
    } else {
      return myParser.getPublicID();
    }
  }
  
  /**
   * Gets a Bearer instance for the Protocol.
   * @return The Bearer object instance corresponding to Protocol or null if no Bearer is known for this Protocol.
   */
  public Bearer getBearerInstance() {
    String locase = toString().toLowerCase();
    String cap = locase.substring(0, 1).toUpperCase() + locase.substring(1, locase.length());
    String classname = "com.nextenso.proxylet.admin." + locase + "." + cap + "Bearer";
    try {
      //try loading the bearer class using a classname pattern
      return (Bearer) Class.forName(classname).newInstance();
    } catch (Throwable t) {
      _logger.info("Failed loading Bearer based on classname pattern: " + classname
          + ". Using default Bearer.");
      return new DefaultBearer(this);
    }
  }
  
}
