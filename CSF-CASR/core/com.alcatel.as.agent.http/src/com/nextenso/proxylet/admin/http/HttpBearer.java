package com.nextenso.proxylet.admin.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;

/**
 *
 */
public class HttpBearer extends Bearer implements Bearer.Factory {
  public final static String REQUEST_CHAIN = "request-chain";
  public final static String RESPONSE_CHAIN = "response-chain";
  
  static List<String> _chainTypes = new ArrayList<String>();
  static {
    _chainTypes.add(REQUEST_CHAIN);
    _chainTypes.add(RESPONSE_CHAIN);
  }
  
  public final static String SESSION_LISTENER = "session-listener";
  public final static String REQUEST_LISTENER = "request-listener";
  public final static String RESPONSE_LISTENER = "response-listener";
  
  static List<String> _listenerTypes = new ArrayList<String>();
  static {
    _listenerTypes.add(SESSION_LISTENER);
    _listenerTypes.add(REQUEST_LISTENER);
    _listenerTypes.add(RESPONSE_LISTENER);
    _listenerTypes.add(CONTEXT_LISTENER);
  }
  
  public final static String SESSION_ATTR = "session-attr";
  public final static String MESSAGE_ATTR = "message-attr";
  public final static String HEADER = "header";
  public final static String CLID = "clid";
  public final static String IPSRC = "ipsrc";
  public final static String IPDEST = "ipdest";
  public final static String PORT = "port";
  public final static String DOMAIN = "domain";
  public final static String PATH = "path";
  
  static List<String> _criterionTypes = new ArrayList<String>();
  static {
    _criterionTypes.add(FROM);
    _criterionTypes.add(UNTIL);
    _criterionTypes.add(DAY);
    _criterionTypes.add(DATE);
    _criterionTypes.add(MONTH);
    _criterionTypes.add(SESSION_ATTR);
    _criterionTypes.add(MESSAGE_ATTR);
    _criterionTypes.add(HEADER);
    _criterionTypes.add(CLID);
    _criterionTypes.add(IPSRC);
    _criterionTypes.add(IPDEST);
    _criterionTypes.add(PORT);
    _criterionTypes.add(DOMAIN);
    _criterionTypes.add(PATH);
    _criterionTypes.add(AND);
    _criterionTypes.add(OR);
    _criterionTypes.add(NOT);
    _criterionTypes.add(REFERENCE);
    _criterionTypes.add(ALL);
  }
  String _nextHop = null;
  
  /**
   * Builds a new HTTP Bearer 
   */
  public HttpBearer() {
    super(Protocol.HTTP);
  }
  
  /**
   * Builds a new HTTP Bearer with a DOM Node.
   * @param node The node.
   */
  public HttpBearer(Node node) {
    this();
    setNode(node);
  }
  
  /**
    Builds a new HTTP Bearer with a subprotocol.
    */
  protected HttpBearer(Protocol p) {
    super(p);
  }
  
  public Bearer newBearer(Node node) {
    return new HttpBearer(node);
  }
  
  /**
   * Gets the list of supported chain types.
   * @return The list of supported chain types (Strings).
   */
  public Iterator<String> getChainTypes() {
    return _chainTypes.iterator();
  }
  
  /**
   * Gets the list of supported listener types.
   * @return The list of supported listener types (Strings).
   */
  public Iterator<String> getListenerTypes() {
    return _listenerTypes.iterator();
  }
  
  /**
   * Gets the list of supported criterion types.
   * @return The list of supported criterion types.
   */
  public Iterator<String> getCriterionTypes() {
    return _criterionTypes.iterator();
  }
  
}
