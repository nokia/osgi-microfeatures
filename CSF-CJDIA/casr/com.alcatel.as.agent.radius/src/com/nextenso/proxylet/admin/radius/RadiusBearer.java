package com.nextenso.proxylet.admin.radius;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;


/**
 *
 * @author  R&D Nextenso
 */

public class RadiusBearer extends Bearer implements Bearer.Factory {
  public final static String ACCT_REQUEST_CHAIN   = "acct-request-chain";
  public final static String ACCT_RESPONSE_CHAIN  = "acct-response-chain";
  public final static String AUTH_REQUEST_CHAIN   = "auth-request-chain";
  public final static String AUTH_RESPONSE_CHAIN  = "auth-response-chain";

  static List<String> _chainTypes = new ArrayList<String>();
  static {
    _chainTypes.add(ACCT_REQUEST_CHAIN);
    _chainTypes.add(ACCT_RESPONSE_CHAIN);
    _chainTypes.add(AUTH_REQUEST_CHAIN);
    _chainTypes.add(AUTH_RESPONSE_CHAIN);
  }
  

  public final static String ACCT_REQUEST_LISTENER    = "acct-request-listener";
  public final static String ACCT_RESPONSE_LISTENER   = "acct-response-listener";
  public final static String AUTH_REQUEST_LISTENER    = "auth-request-listener";
  public final static String AUTH_RESPONSE_LISTENER   = "auth-response-listener";

  static List<String> _listenerTypes = new ArrayList<String>();
  static {
    _listenerTypes.add(ACCT_REQUEST_LISTENER);
    _listenerTypes.add(ACCT_RESPONSE_LISTENER);
    _listenerTypes.add(AUTH_REQUEST_LISTENER);
    _listenerTypes.add(AUTH_RESPONSE_LISTENER);
	_listenerTypes.add(CONTEXT_LISTENER);
  }
  
  static List <String>_criterionTypes = new ArrayList<String>();
  static {
    _criterionTypes.add(FROM);
    _criterionTypes.add(UNTIL);
    _criterionTypes.add(DAY);
    _criterionTypes.add(DATE);
    _criterionTypes.add(MONTH);
    _criterionTypes.add(MESSAGE_ATTR);
    _criterionTypes.add(OR);
    _criterionTypes.add(AND);
    _criterionTypes.add(NOT);
    _criterionTypes.add(REFERENCE);  
    _criterionTypes.add(ALL);
  }
  //String _nextHop = null;

  
  /** Creates a new instance of RadiusBearer */
  public RadiusBearer() {
    super(Protocol.RADIUS);
  }
  
  /**
   * Builds a new RadiusBearer with a DOM Node.
   * @param node The node.
   */
  public RadiusBearer(Node node) {
    this();
    setNode(node);
  }

  /**
   * implements Bearer.Factory
   */
  public Bearer newBearer(Node node) {
    return new RadiusBearer(node);
  }
  
  
  /**
   * Gets the list of supported chain types.
   * @return The list of supported chain types (Strings).
   */
  @Override
	public Iterator getChainTypes() {
    return _chainTypes.iterator();
  }
  
  /**
   * Gets the list of supported listener types.
   * @return The list of supported listener types (Strings).
   */
  @Override
	public Iterator getListenerTypes() {
    return _listenerTypes.iterator();
  }
  
  /**
   * Gets the list of supported criterion types.
   * @return The list of supported criterion types.
   */
  @Override
	public Iterator getCriterionTypes() {
    return _criterionTypes.iterator();
  }
}
