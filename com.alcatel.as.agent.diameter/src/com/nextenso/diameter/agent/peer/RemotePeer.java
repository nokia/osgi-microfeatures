// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.peer.Peer.ProcessMessageResultListener;
import com.nextenso.diameter.agent.peer.statemachine.DiameterStateMachine;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterApplication;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.AddressFormat;

/**
 * The Remote Peer (dynamic or static).
 */
public class RemotePeer extends Peer {
  
  private static final Logger LOGGER = Logger.getLogger("agent.diameter.peer.remote");
  private static final Logger LOGGER_OVERLOAD = Logger.getLogger("agent.diameter.peer.remote.overload");
  
  /** static values **/
  private boolean _isSecure;
  /** variable values **/
  private final DiameterStateMachine _stateMachine;
  private String _originRealm, _productName;
  private byte[][] _ips;
  private long _vendorId = -1L;
  private long _originStateId = -1L;
  private long _firmwareRevision = -1L;
  private boolean _isRelay = false;
  private AtomicInteger _overloadMonitor = new AtomicInteger(0);
  private boolean _readDisabled = false;
  private DiameterAVP[] _rawCapsAVPs;
  private List<String> _hosts;
  
  private final Set<Long> _supportedVendorIdList = new HashSet<Long>();
  private final Set<Long> _inbandSecurityIdList = new HashSet<Long>();
  private final Set<Long> _authApplicationsIdList = new HashSet<Long>();
  private final Set<Long> _acctApplicationsIdList = new HashSet<Long>();
  private final List<DiameterApplication> _specificApplications = new ArrayList<DiameterApplication>();
  private final List<DiameterApplication> _supportedApplications = new ArrayList<DiameterApplication>();
  
  private String _localOriginHost, _localOriginRealm;
  private volatile boolean _isActive = false;
  private Long _quarantineDelayInMs = null;
  private long _quarantineEndTime = 0L;
  private boolean _connected = false; // checks if peerListeners.connected was called : indeed, a peer may be closed before connected was called --> dont want to call peerListeners.disconnected
  
  /**
   * Constructor for this class. Dynamic peer (acts as server). This Object is
   * garbaged when the connection is closed.
   * 
   * @param handlerName The handler name.
   * @param originHost The origin host.
   * @param originRealm The origin realm.
   * @param host The host.
   * @param port The port.
   * @param secure true if encrypted.
   */
  public RemotePeer(String handlerName, String originHost, String originRealm, String host, int port,
                    boolean secure, Protocol protocol) {
    super(handlerName, originHost, host, port, protocol);
    setId (SEED_REMOTE_R.incrementAndGet ());
    _stateMachine = new DiameterStateMachine(this);
    _originRealm = originRealm;
    _isSecure = secure;
    String localOriginHost = Utils.getServerOriginHost(handlerName);
    String localOriginRealm = DiameterProperties.getOriginRealm();
    setLocalOriginHost(localOriginHost);
    setLocalOriginRealm(localOriginRealm);
    
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("new Peer: " + this);
    }
  }
  
  protected void setLocal(String originHost, DiameterAVP originHostAvp) {
      // NOT USED ANYMORE - kept for baselining
      throw new RuntimeException ("Not implemented");
  }
    
  public void setLocalOriginHost(String originHost) {
    _localOriginHost = originHost;
  }
  public void setLocalOriginRealm(String originRealm) {
      _localOriginRealm = originRealm;
  }
  
  protected RemotePeer(String handlerName, long id, String originHost, String originRealm, List<String> hosts,
                       int port, boolean secure, Protocol protocol) {
    super(handlerName, originHost, hosts, port, protocol);
    setId (id);
    _stateMachine = new DiameterStateMachine(this);
    _originRealm = originRealm;
    _isSecure = secure;
    
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("new Peer: " + this);
    }
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getSupportedApplications()
   */
  public List<DiameterApplication> getSupportedApplications() {
    return _supportedApplications;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#setSupportedApplications(java.util.List)
   */
  public void setSupportedApplications(List<DiameterApplication> applications)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Method not allowed on RemotePeer");
  }
  
  /**
   * Gets the state machine for RFC3588.
   * 
   * @return The state machine.
   */
  public DiameterStateMachine getStateMachine() {
    return _stateMachine;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#connect()
   */
  public void connect() {
    throw new UnsupportedOperationException("Cannot to connect a not locally initiated peer");
  }
  public void connect(int port, String... localIP){
    throw new UnsupportedOperationException("Cannot to connect a not locally initiated peer");
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getLocalDiameterPeer()
   */
  public DiameterPeer getLocalDiameterPeer() {
    return Utils.getTableManager().getLocalDiameterPeer(getHandlerName());
  }
  
  /**
   * Gets the local Origin-Host.
   * 
   * @return The local Origin-Host.
   */
  public String getLocalOriginHost() {
    return _localOriginHost;
  }
  public String getLocalOriginRealm (){
    return _localOriginRealm;
  }
  
  /**
   * Indicates to the peer it has been connected.
   */
  public void connected() {
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("connected: peer= " + this);
    }
    _connected = true;

    if (isLocalInitiator ()){
	// outbound : cache if no reconnected allowed
	if (!DiameterProperties.peerReconnectEnabled ())
	    _hosts = getHosts ();
    } else {
	// inbound : cache
	_hosts = getHosts ();
    }
    
    for (final DiameterPeerListener listener : getDiameterListeners()) {
      listener.connected(RemotePeer.this);
    }
    
    Utils.getTableManager().connected(this);
    getStateMachine().getSocket().activate(true);
  }
  
  public void sctpAddressChanged (String addr, int port, DiameterPeerListener.SctpAddressEvent event){
      for (DiameterPeerListener listener : getDiameterListeners()) {
	  listener.sctpAddressChanged(this, addr, port, event);
      }
      Utils.getTableManager().sctpAddressChanged(this, addr, port, event);
  }
  // called when the connection is open / else it is called on StaticPeer before connect() to store the options
  public void setSctpSocketOptions (java.util.Map options){
    PeerSocket ps = getStateMachine().getSocket();
    if (ps != null)
	ps.setSctpSocketOptions (options);
  }
  public void setParameters (java.util.Map<String, String> params){
    PeerSocket ps = getStateMachine().getSocket();
    if (ps != null)
	ps.setParameters (params);
  }
  
  /**
   * Indicates to the peer it has been disconnected.
   * 
   * @param disconnectedReason The reason.
   * @param wasConnecting
   */
  public void disconnected(final int disconnectedReason, boolean wasConnecting) {
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("disconnected: peer=" + this);
    }
    PeerTable peerTable = Utils.getTableManager().getPeerTable(getHandlerName());
    if (peerTable != null) {
      peerTable.disconnected(this);
    }
    
    if (wasConnecting) {
      final String message = "disconnectedReason=" + disconnectedReason;
      for (DiameterPeerListener listener : getDiameterListeners()) {
        listener.connectionFailed(RemotePeer.this, message);
      }
      Utils.getTableManager().connectionFailed(this, message);
    } else if (_connected) {
      for (DiameterPeerListener listener : getDiameterListeners()) {
        listener.disconnected(RemotePeer.this, disconnectedReason);
      }
      Utils.getTableManager().disconnected(this, disconnectedReason);
    }
    // reset the state machine and clean the AVP, wait for new CER.
    reset();
  }
  
  /**
   * Resets the content of this peer.
   */
  protected void reset() {
    LOGGER.debug("reset internal attributes");
    //_stateMachine.reset(this);
    if (isLocalInitiator () &&
	DiameterProperties.peerReconnectEnabled ()
	){
	// for incoming peers : no need to reset the structure
	// can be dangerous if listener.connected()/disconnected() is still in flight
	_originRealm = null;
	_productName = null;
	_ips = null;
	_vendorId = -1;
	_originStateId = -1;
	_firmwareRevision = -1;
    
	_supportedVendorIdList.clear();
	_inbandSecurityIdList.clear();
	_authApplicationsIdList.clear();
	_acctApplicationsIdList.clear();
	_specificApplications.clear();
    }
    _connected = false; // bug CSFS-6114
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#isRelay()
   */
  public boolean isRelay() {
    return _isRelay;
  }
  
  public long processCER(DiameterRequestFacade cer) {
    try{

      // check if there is no remote agent - the CER holds a Disconnect AVP to mark it
      DiameterAVP avp = cer.getDiameterAVP(DiameterBaseConstants.AVP_DISCONNECT_CAUSE);
      if (avp != null){
	  LOGGER.info ("Received CER with No remote peer indication (Disconnect cause AVP present)");
	  return Utils.fillCEA(cer.getRequestFacade().getResponseFacade(), DiameterBaseConstants.RESULT_CODE_DIAMETER_TOO_BUSY);
      }
      
      readPeerCapabilities(cer);
      cer.setClientPeer (this);
    
      long result = DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS;
      if (!isLocalDiameterPeer() && !isLocalInitiator()) {
	// check dynamic peer
	result = Utils.checkConnectionFilters(this);
	if (result != DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
	  return Utils.fillCEA(cer.getRequestFacade().getResponseFacade(), result);
	}
      }
    
      boolean isCompliant = Utils.getCapabilities().isCompliantMessage(cer);
      if (!isCompliant) {
	result = DiameterBaseConstants.RESULT_CODE_DIAMETER_NO_COMMON_APPLICATION;
      }
    
      return Utils.fillCEA(cer.getRequestFacade().getResponseFacade(), result);
    } catch(DiameterMessageFacade.ParsingException pe){
      throw new RuntimeException (pe);
    }
  }
  
  public boolean processCEA(DiameterMessageFacade cea) {
    try{
      readPeerCapabilities(cea);
      
      boolean isCeaCompliant = false;
      long result = cea.getResponseFacade().getResultCode();
      if (result == DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
	isCeaCompliant = Utils.getCapabilities().isCompliantMessage(cea);
      }
      
      return isCeaCompliant;
    } catch(DiameterMessageFacade.ParsingException pe){
      throw new RuntimeException (pe);
    }
  }
  
  /**
   * Reads the capabilities in the message.
   * 
   * @param peerMsg the message to read.
   */
  public void readPeerCapabilities(DiameterMessage peerMsg) throws DiameterMessageFacade.ParsingException {

    _rawCapsAVPs = new DiameterAVP[peerMsg.getDiameterAVPsSize ()];
    for (int i=0; i<_rawCapsAVPs.length; i++){
      _rawCapsAVPs[i] = peerMsg.getDiameterAVP (i);
    }
    
    DiameterAVP avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
    if (avp != null) {
      byte[] value = avp.getValue();
      setOriginHost(IdentityFormat.getIdentity(value, 0, value.length));
    }
    
    avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
    if (avp != null) {
      byte[] value = avp.getValue();
      _originRealm = IdentityFormat.getIdentity(value, 0, value.length);
    }
    
    avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
    if (avp == null)
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_MISSING_AVP, "Missing Host IP Address AVP", DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
    try{
      _ips = new byte[avp.getValueSize()][];
      for (int k = 0; k < avp.getValueSize(); k++) {
	byte[] value = avp.getValue(k);
	if (value == null ||
	    value.length < 3) throw new Exception ();
	int type = AddressFormat.INSTANCE.getAddressType (value, 0);
	switch (type){
	case AddressFormat.IPV4:
	  if (value.length != 6) throw new Exception ();
	  break;
	case AddressFormat.IPV6:
	  if (value.length != 18) throw new Exception ();
	  break;
	default:
	    if (DiameterProperties.checkHostIPAddressFamily ()){ // http://www.iana.org/assignments/address-family-numbers/address-family-numbers.xhtml
		if ((type >= 1 && type <= 30) ||
		    (type >= 16384 && type <= 16398)){
		    // ok
		}else{
		    throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Host IP Address AVP (Unknown address family : "+type+")", DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
		}
	    }
	}
	_ips[k] = value;
      }
    }catch(DiameterMessageFacade.ParsingException pe){
	throw pe;
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Host IP Address AVP", DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
    }
    
    avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
    if (avp == null) throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_MISSING_AVP, "Missing Vendor Id AVP", DiameterBaseConstants.AVP_VENDOR_ID).setDefValue (new byte[4]);
    try{
      _vendorId = Unsigned32Format.getUnsigned32(avp.getValue(), 0);
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Vendor Id AVP", DiameterBaseConstants.AVP_VENDOR_ID).setDefValue (new byte[4]);
    }

    avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_PRODUCT_NAME);
    if (avp == null) throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_MISSING_AVP, "Missing Product Name AVP", DiameterBaseConstants.AVP_PRODUCT_NAME);
    try{
      byte[] value = avp.getValue();
      _productName = UTF8StringFormat.getUtf8String(value, 0, value.length);
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Product Name AVP", DiameterBaseConstants.AVP_PRODUCT_NAME);
    } 

    try{
      avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_STATE_ID);
      if (avp != null) {
	_originStateId = Unsigned32Format.getUnsigned32(avp.getValue(), 0);
      }
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Origin State Id AVP", DiameterBaseConstants.AVP_ORIGIN_STATE_ID).setDefValue (new byte[4]);
    }

    try{
      avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_SUPPORTED_VENDOR_ID);
      fillSet(avp, _supportedVendorIdList);
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Supported Vendor Id AVP", DiameterBaseConstants.AVP_SUPPORTED_VENDOR_ID);
    }
      
    try{
      avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
      fillSet(avp, _authApplicationsIdList);
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Auth Application Id AVP", DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
    }
  
    try{
      avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
      fillSet(avp, _acctApplicationsIdList);
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Acct Application Id AVP", DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
    }

    try{
      avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
      fillApplicationList(avp, _specificApplications);
    }catch(Exception e){
      throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Vendor Specific Application Id AVP", DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
    }
    
    // we consolidate in _supportedApplications
    _supportedApplications.addAll(_specificApplications);
    for (long id : _authApplicationsIdList)
      _supportedApplications.add(new DiameterApplication(id, 0, true));
    for (long id : _acctApplicationsIdList)
      _supportedApplications.add(new DiameterApplication(id, 0, false));
    
    _isRelay = _authApplicationsIdList.contains(DiameterBaseConstants.APPLICATION_RELAY)
        || _acctApplicationsIdList.contains(DiameterBaseConstants.APPLICATION_RELAY);

    try{
	avp = peerMsg.getDiameterAVP(DiameterBaseConstants.AVP_FIRMWARE_REVISION);
	if (avp != null) {
	    _firmwareRevision = Unsigned32Format.getUnsigned32(avp.getValue(), 0);
	}
    }catch(Exception e){
	throw new DiameterMessageFacade.ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid Firmware revision AVP", DiameterBaseConstants.AVP_FIRMWARE_REVISION).setDefValue (new byte[4]);
    }
  }
  
  private void fillApplicationList(DiameterAVP specificVendorAvp, List<DiameterApplication> list) {
    if (specificVendorAvp == null) {
      LOGGER.debug("fillApplicationList: No Vendor Specific Applications");
      return;
    }
    for (int i = 0; i < specificVendorAvp.getValueSize(); i++) {
      byte[] value = specificVendorAvp.getValue(i);
      DiameterAVP avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID, value, false);
      long vendorId = -1;
      if (avp != null) {
        vendorId = Unsigned32Format.getUnsigned32(avp.getValue(), 0);
      }
      avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID, value, false);
      boolean isAuth = (avp != null);
      if (!isAuth) {
        avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID, value, false);
      }
      
      long appId = -1;
      if (avp != null) {
        appId = Unsigned32Format.getUnsigned32(avp.getValue(), 0);
      }
      DiameterApplication app = new DiameterApplication(appId, vendorId, isAuth);
      list.add(app);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("fillApplicationList: add application" + app);
      }
    }
  }
  
  /**
   * Loads the application identifiers.
   * 
   * @param avp The application AVP
   */
  private void fillSet(DiameterAVP avp, Set<Long> set) {
    if (avp == null) {
      return;
    }
    for (int i = 0; i < avp.getValueSize(); i++) {
      set.add(Unsigned32Format.getUnsigned32(avp.getValue(i), 0));
    }
  }
  
  /**
   * Creates and returns a DPR with the filled Disconnect-Cause AVP
   * 
   * @param cause The value to fill the Disconnect-Cause AVP
   * @return The DPR.
   */
  public DiameterRequestFacade createDPR(int cause) {
    DiameterRequestFacade dpr = new DiameterRequestFacade(DiameterBaseConstants.COMMAND_DPR, this);
    dpr.setDefaultOriginAVPs();
    DiameterAVP disconnectCauseAVP = new DiameterAVP(DiameterBaseConstants.AVP_DISCONNECT_CAUSE);
    disconnectCauseAVP.setValue(EnumeratedFormat.toEnumerated(cause), false);
    dpr.addDiameterAVP(disconnectCauseAVP);
    dpr.setRetryTimeoutInMs (DiameterProperties.getDprTimeout ());
    return dpr;
  }
  
  /**
   * Process the DPA. Do nothing.
   * 
   * @param dpa The DPA.
   */
  public void processDPA(DiameterMessageFacade dpa) {
    // nothing to do so far (never called?)
    // TODO : remove from static peers ? hard when load balancing
  }
  
  /**
   * @see com.nextenso.diameter.agent.peer.Peer#sendMessage(com.nextenso.diameter.agent.impl.DiameterMessageFacade)
   */
  @Override
  public void sendMessage(DiameterMessageFacade message) {
    getStateMachine().sendMessage(message);
  }
  
  /**
   * @see com.nextenso.diameter.agent.peer.Peer#processMessage(com.nextenso.diameter.agent.impl.DiameterMessageFacade,
   *      boolean)
   */
  @Override
  public void processMessage(final DiameterMessageFacade message, final boolean mainThread) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("RemotePeer.processMessage: mainThread=" + mainThread);
    if (mainThread) {
      // a message was received
      setIsActive(true);
      if (message.isRequest())
        messageStart();
    }
    Runnable r = new Runnable() {
      public void run() {
        try {
	    message.parseData();
	} catch (DiameterMessageFacade.ParsingException pe){
	    if (message.isRequest ()){
		LOGGER.warn(RemotePeer.this + " : exception while parsing request : "+pe.getMessage ());
		DiameterResponseFacade resp = message.getResponseFacade ();
		((DiameterRequestFacade)message).setClientPeer (RemotePeer.this);
		resp.setLocalOrigin(true);
		resp.setResultCode (pe.result ());
		if (pe.failedAVP () != null) resp.addDiameterAVP (pe.failedAVP ());
		requestResultListener.handleResult(resp);
	    } else {
		// drop response;
		LOGGER.warn(RemotePeer.this + " : exception while parsing response - dropping it : "+pe.getMessage ());
		requestResultListener.handleResult(null);
	    }
	    return;
        } catch (Exception e) {
          LOGGER.warn(RemotePeer.this + " : exception while parsing message - closing", e);
          close();
          return;
        }
        LocalPeer localPeer = Utils.getClientLocalPeer(getHandlerName());
        if (message.isRequest()) {
	    if (localPeer == null){
		// lets drop the req by precaution
		LOGGER.warn (RemotePeer.this+" : Local Peer is gone, dropping request : "+message.getSessionId ());
		return;
	    }
	    processRequest(message.getRequestFacade(), mainThread, localPeer, requestResultListener);
	    return;
        }
        
        // message is a response
        DiameterRequestFacade request = message.getRequestFacade();
        if (request.isDirectClientRequest()) {
          localPeer.sendMessage(message);
          return;
        }
        
        // quarantine the peer if needed
        if (request.isAlternativePeerFound()) {
          LOGGER.debug("processMessage: send the message to an alternative peer because quarantine is used");
          request.send();
          return;
        }
        
        processResponse(message.getResponseFacade(), mainThread, responseResultListener);
      }
    };
    if (DiameterProperties.isMessageScheduled() && mainThread) {
      LOGGER.debug("scheduling message in processing thread pool");
      Utils.schedule(r, message.getSessionId());
    } else {
      LOGGER.debug("executing message in current thread");
      r.run();
    }
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getOriginRealm()
   */
  public String getOriginRealm() {
    return _originRealm;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getHostIPAddresses()
   */
  public byte[][] getHostIPAddresses() {
    if (_ips == null) {
      return null;
    }
    byte[][] res = new byte[_ips.length][];
    for (int i = 0; i < _ips.length; i++) {
      int len = _ips[i].length;
      byte[] copy = new byte[len];
      System.arraycopy(_ips[i], 0, copy, 0, len);
      res[i] = copy;
    }
    
    return res;
  }

    public List<String> getHosts(){
	if (_hosts != null) return _hosts; // cached under conditions only
	PeerSocket ps = getStateMachine().getSocket();
	if (ps == null) return new ArrayList<> (1); // def is blank list
	return ps.getHosts();
    }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getVendorId()
   */
  public long getVendorId() {
    return _vendorId;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getProductName()
   */
  public String getProductName() {
    return _productName;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getOriginStateId()
   */
  public long getOriginStateId() {
    return _originStateId;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getAuthApplications()
   */
  public long[] getAuthApplications() {
    return getLongArray(_authApplicationsIdList);
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getAcctApplications()
   */
  public long[] getAcctApplications() {
    return getLongArray(_acctApplicationsIdList);
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getSpecificApplications()
   * @deprecated
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public List<DiameterApplication> getSpecificApplications() {
    return _specificApplications;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getSupportedVendorIds()
   */
  public long[] getSupportedVendorIds() {
    return getLongArray(_supportedVendorIdList);
  }
  
  /**
   * 
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getInbandSecurityId()
   */
  public long[] getInbandSecurityId() {
    return getLongArray(_inbandSecurityIdList);
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getFirmwareRevision()
   */
  public long getFirmwareRevision() {
    return _firmwareRevision;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#isEncrypted()
   */
  public boolean isEncrypted() {
    return _isSecure;
  }

  public DiameterAVP[] getCapabilitiesExchangeAVPs (){
      return _rawCapsAVPs;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#disconnect(int)
   */
  public void disconnect(int disconnectCause) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("disconnect: disconnectCause=" + disconnectCause);
    }
    
    if (isConnecting() || isConnected()) {
      LOGGER.debug("disconnect: isConnected -> disconnect");
      getStateMachine().disconnect(disconnectCause);
    } else {
      LOGGER.debug("disconnect: is not connected or connecting -> do nothing");
    }
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#isConnected()
   */
  public boolean isConnected() {
    return getStateMachine().isConnected();
  }
  
  public boolean isConnecting() {
    return getStateMachine().isConnecting();
  }
  
  public boolean isDisconnected() {
    return getStateMachine().isDisconnected();
  }
  
  public boolean isDisconnecting() {
    return getStateMachine().isDisconnecting();
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder buff = new StringBuilder(getClass().getSimpleName());
    buff.append(" [id=").append(getId());
    buff.append(" , originHost=").append(getOriginHost());
    buff.append(", protocol=").append(getProtocol());
    buff.append(", hash=").append(hashCode());
    boolean first = true;
    buff.append(", host=");
    for (String host : getConfiguredHosts ()){
      if (first) first = false;
      else buff.append ('/');
      if (host.equals (getHost()))
	  buff.append ("*");
      buff.append(host);
    }
    buff.append(", port=").append(getPort());
    buff.append(", secure=").append(isEncrypted());
    buff.append(", handler name=").append(getHandlerName());
    buff.append(", local originHost/Realm=").append(getLocalOriginHost()).append('/').append(getLocalOriginRealm());
    buff.append(']');
    return buff.toString();
  }
  
  /**
   * Sets the isActive.
   * 
   * @param isActive The isActive.
   */
  protected void setIsActive(boolean isActive) {
    _isActive = isActive;
  }
  
  /**
   * Gets the isActive.
   * 
   * @return The isActive.
   */
  protected boolean isActive() {
    return _isActive;
  }
  
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
  
  private ProcessMessageResultListener requestResultListener = new ProcessMessageResultListener() {
    public void handleResult(DiameterMessageFacade result) {
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("handleResult: result=" + result);
      }
      if (result == null) {
        messageDone();
        return;
      }
      if (result.isRequest()) {
        RemotePeer destinationPeer = (RemotePeer) result.getServerPeer();
        destinationPeer.sendMessage(result);
        return;
      }
      // result is a response
      try {
        messageDone();
        sendMessage(result);
      } catch (IllegalStateException t) {
        if (getLogger().isInfoEnabled()) {
          getLogger().info("Failed to send :\n" + result);
        }
      }
    }
  };
  private ProcessMessageResultListener responseResultListener = new ProcessMessageResultListener() {
    public void handleResult(DiameterMessageFacade result) {
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("handleResult: result=" + result);
      }
      if (result == null) {
        // NOTE : this case should never happen !
        // RISK on messageDone()
        return;
      }
      if (result.isRequest ()){
	  RemotePeer destinationPeer = (RemotePeer) result.getServerPeer();
	  destinationPeer.sendMessage(result);
	  return;
      }
      Peer clientPeer = (Peer) result.getClientPeer(); // not 'this' when processing a response
      try {
	  if (clientPeer instanceof RemotePeer)
	      ((RemotePeer) clientPeer).messageDone();
	  clientPeer.sendMessage(result);
      } catch (IllegalStateException t) {
        if (getLogger().isInfoEnabled()) {
          getLogger().info("Failed to send :\n" + result);
        }
      }
    }
  };
  
  public void close() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("close");
    }
    disconnect(DiameterProperties.getDprReason());
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#getQuarantineDelay()
   */
  @Override
  public Long getQuarantineDelay() {
    if (_quarantineDelayInMs == null) {
      return DiameterProperties.getDefaultQuarantineDelay();
    }
    return _quarantineDelayInMs;
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#setQuarantineDelay(java.lang.Long)
   */
  @Override
  public void setQuarantineDelay(Long delayInMs) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("setQuarantineDelay: delay=" + delayInMs + ", this=" + this);
    }
    _quarantineDelayInMs = delayInMs;
  }
  
  public void quarantine() {
    long delay = getQuarantineDelay();
    if (delay > 0) {
      _quarantineEndTime = System.currentTimeMillis() + delay;
    }
  }
  
  /**
   * @see com.nextenso.proxylet.diameter.DiameterPeer#isQuarantined()
   */
  public boolean isQuarantined() {
    return _quarantineEndTime > System.currentTimeMillis();
  }
  
  public void messageStart() {
    if (DiameterProperties.isOverloadActive()) {
      if (_overloadMonitor.incrementAndGet() == DiameterProperties.getOverloadHighWM()) {
        synchronized (_overloadMonitor) {
          // Recheck in case messageDone has been called concurrently
          if (_overloadMonitor.get() >= DiameterProperties.getOverloadHighWM()) {
            if (!_readDisabled) {
              if (LOGGER_OVERLOAD.isInfoEnabled())
                LOGGER_OVERLOAD.info(this + " : DisableRead");
              PeerSocket ps = getStateMachine().getSocket();
              if (ps != null)
                ps.disableRead();
              _readDisabled = true;
            }
          }
        }
      }
    }
  }
  
  public void messageDone() {
    if (DiameterProperties.isOverloadActive()) {
      if (_overloadMonitor.decrementAndGet() == DiameterProperties.getOverloadLowWM()) {
        synchronized (_overloadMonitor) {
          // Recheck in case messageStart has been called concurrently
          if (_overloadMonitor.get() <= DiameterProperties.getOverloadLowWM()) {
            if (_readDisabled) {
              if (LOGGER_OVERLOAD.isInfoEnabled())
                LOGGER_OVERLOAD.info(this + " : EnableRead");
              PeerSocket ps = getStateMachine().getSocket();
              if (ps != null)
                ps.enableRead();
              _readDisabled = false;
            }
          }
        }
      }
    }
  }
}
