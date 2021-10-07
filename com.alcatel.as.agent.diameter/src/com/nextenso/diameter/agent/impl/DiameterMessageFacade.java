package com.nextenso.diameter.agent.impl;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.LinkedList;
import java.util.Collections;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ByteOutputStream;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.diameter.agent.peer.RemotePeer;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterApplication;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.engine.AsyncProxyletManager;
import com.nextenso.proxylet.engine.AsyncProxyletManager.ProxyletResumer;
import com.nextenso.proxylet.impl.ProxyletDataImpl;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;

public abstract class DiameterMessageFacade
		extends ProxyletDataImpl
		implements DiameterMessage, ProxyletResumer {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.message");
	private static final String DEFAULT_ORIGIN_STR = "diameter.default.origin.value";
	private static final byte[] DEFAULT_ORIGIN = IdentityFormat.toIdentity(DEFAULT_ORIGIN_STR);
	private static final byte[] EMPTY_LENGTH = new byte[] { 0, 0, 0 };
	private static volatile boolean SYNCHRONIZED = true;

	private List<DiameterAVP> _avps;
	private int _command;
	private int _flags;
	private long _applicationId;
	private long _stackSessionId; // the sessionId used by the stack
	private InetSocketAddress[] _receptionAddresses;
	private boolean _isSupportingRouteRecord = true;
	private long _stackTimestamp=0L;

	public static void setSynchronized (boolean sync){
		SYNCHRONIZED = sync;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#isSupportingRouteRecord()
	 */
	@Override
	public boolean isSupportingRouteRecord() {
		return _isSupportingRouteRecord;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#setSupportingRouteRecord(boolean)
	 */
	@Override
	public void setSupportingRouteRecord(boolean support) {
		_isSupportingRouteRecord = support;
	}

	private DiameterApplication _application;

	protected DiameterMessageFacade(long application, int command, int flags) {
		super (DiameterProperties.isMessageSynchronized ());
		_avps = new LinkedList<DiameterAVP> ();
		if (SYNCHRONIZED){
			_avps = Collections.synchronizedList (_avps);
		}
		_applicationId = application;
		_command = command;
		_flags = flags;
	}

	public void setStackTimestamp (long timestamp){
		_stackTimestamp = timestamp;
	}

	public long getStackTimestamp (){ return _stackTimestamp;}

	public void writeStackTimestamp (ByteOutputStream baos){
		// to be overridden but not made abstract to avoid changing h2
	}

	public static void writeStackTimestamp (ByteOutputStream baos, long timestamp){
		baos.write ((byte) (timestamp >> 40));
		baos.write ((byte) (timestamp >> 32));
		baos.write ((byte) (timestamp >> 24));
		baos.write ((byte) (timestamp >> 16));
		baos.write ((byte) (timestamp >> 8));
		baos.write ((byte) (timestamp));
	}

	protected void setStackSessionId(long stackSessionId) {
		_stackSessionId = stackSessionId;
	}

	protected long getStackSessionId() {
		return _stackSessionId;
	}

	public boolean hasFlag(int flag) {
		return ((_flags & flag) == flag);
	}

	public void setFlag(int flag, boolean set) {
		if (set) {
			_flags |= flag;
		} else {
			_flags &= ~flag;
		}
	}

	public int getFlags() {
		return _flags;
	}

	public void setFlags(int flags) {
		_flags = flags;
	}

	/******************* abstract methods ****************/

	public abstract String getHandlerName();

	public abstract int getClientHopIdentifier();

	public abstract int getServerHopIdentifier();

	public abstract boolean isRequest();

	public abstract boolean isLocalOrigin();

	public abstract DiameterRequestFacade getRequestFacade();

	public abstract DiameterResponseFacade getResponseFacade();

	public abstract int getOutgoingClientHopIdentifier();

	public abstract int getEndIdentifier();

	protected abstract String getLocalOriginHost();

	protected abstract String getLocalOriginRealm();
    
	public abstract void send(PeerSocket socket);

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getVersion()
	 */
	public int getVersion() {
		return Utils.getVersion();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterCommand()
	 */
	public int getDiameterCommand() {
		return _command;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVP(int)
	 */
	public DiameterAVP getDiameterAVP(int index) {
		if (!_parsed) throw new RuntimeException ();
		try {
			return _avps.get(index);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVP(long,
	 *      long)
	 */
	public DiameterAVP getDiameterAVP(long code, long vendorId) {
		if (SYNCHRONIZED){
			synchronized (_avps){
				return getDiameterAVP_internal (code, vendorId);
			}
		} else return getDiameterAVP_internal (code, vendorId);
	}
	private DiameterAVP getDiameterAVP_internal(long code, long vendorId) {
		if (!_parsed) throw new RuntimeException ();
		for (DiameterAVP avp : _avps) {
			if (avp != null && avp.getAVPCode() == code && avp.getVendorId() == vendorId) {
				return avp;
			}
		}

		return null;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVP(com.nextenso.proxylet.diameter.DiameterAVPDefinition)
	 */
	public DiameterAVP getDiameterAVP(DiameterAVPDefinition definition) {
		return getDiameterAVP(definition.getAVPCode(), definition.getVendorId());
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#addDiameterAVP(com.nextenso.proxylet.diameter.DiameterAVP)
	 */
	public void addDiameterAVP(DiameterAVP avp) {
		addDiameterAVP(-1, avp);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#addDiameterAVP(int,
	 *      com.nextenso.proxylet.diameter.DiameterAVP)
	 */
	public void addDiameterAVP(int index, DiameterAVP avp) {
		if (!_parsed) throw new RuntimeException ();
		if (avp != null) {
			if (index >= 0)
				_avps.add(index, avp);
			else
				_avps.add (avp);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#addDiameterAVP(com.nextenso.proxylet.diameter.DiameterAVPDefinition)
	 */
	public DiameterAVP addDiameterAVP(DiameterAVPDefinition def) {
		DiameterAVP avp = getDiameterAVP(def);
		if (avp == null) {
			avp = new DiameterAVP(def);
			addDiameterAVP(avp);
		}
		return avp;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#removeDiameterAVP(int)
	 */
	public DiameterAVP removeDiameterAVP(int index) {
		if (!_parsed) throw new RuntimeException ();
		try {
			return _avps.remove(index);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#removeDiameterAVP(com.nextenso.proxylet.diameter.DiameterAVPDefinition)
	 */
	public DiameterAVP removeDiameterAVP(DiameterAVPDefinition definition) {
		return removeDiameterAVP(definition.getAVPCode(), definition.getVendorId());
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#removeDiameterAVP(long,
	 *      long)
	 */
	public DiameterAVP removeDiameterAVP(long code, long vendorId) {
		if (SYNCHRONIZED){
			synchronized (_avps){
				return removeDiameterAVP_internal (code, vendorId);
			}
		} else return removeDiameterAVP_internal (code, vendorId);
	}
	private DiameterAVP removeDiameterAVP_internal(long code, long vendorId) {
		if (!_parsed) throw new RuntimeException ();
		int n = _avps.size ();
		for (int i = 0; i<n; i++){
			DiameterAVP avp = _avps.get (i);
			if (avp.getAVPCode() == code && avp.getVendorId() == vendorId) {
				_avps.remove (i);
				return avp;
			}
		}
		return null;
	}
	public int removeDiameterAVPs(long code, long vendorId) {
		if (SYNCHRONIZED){
			synchronized (_avps){
				return removeDiameterAVPs_internal (code, vendorId);
			}
		} else return removeDiameterAVPs_internal (code, vendorId);
	}
	private int removeDiameterAVPs_internal(long code, long vendorId) {
		if (!_parsed) throw new RuntimeException ();
		int removed = 0;
		for (int i = 0; i<_avps.size (); ){
			DiameterAVP avp = _avps.get (i);
			if (avp.getAVPCode() == code && avp.getVendorId() == vendorId) {
				_avps.remove (i);
				removed++;
			} else
				i++;
		}
		return removed;
	}

	public void addDiameterAVPs (DiameterMessage fromMessage, boolean clone){
		if (SYNCHRONIZED){
			synchronized (_avps){
				synchronized (((DiameterMessageFacade)fromMessage)._avps){
					addDiameterAVPs_internal ((DiameterMessageFacade) fromMessage, clone);
				}
			}
		} else addDiameterAVPs_internal ((DiameterMessageFacade) fromMessage, clone);
	}
	private void addDiameterAVPs_internal(DiameterMessageFacade fromMessage, boolean clone){
		if (!_parsed) throw new RuntimeException ();
		for (int i = 0; i<fromMessage._avps.size (); i++){
			DiameterAVP avp = fromMessage._avps.get (i);
			int index = getDiameterAVPIndex_internal (avp.getAVPCode(), avp.getVendorId());
			if (index == -1)
				addDiameterAVP (clone ? (DiameterAVP) avp.clone () : avp);
		}
	}
	public void setDiameterAVPs (DiameterMessage fromMessage, boolean clone){
		if (SYNCHRONIZED){
			synchronized (_avps){
				synchronized (((DiameterMessageFacade)fromMessage)._avps){
					setDiameterAVPs_internal ((DiameterMessageFacade) fromMessage, clone);
				}
			}
		} else setDiameterAVPs_internal ((DiameterMessageFacade) fromMessage, clone);
	}
	private void setDiameterAVPs_internal(DiameterMessageFacade fromMessage, boolean clone){
		if (!_parsed) throw new RuntimeException ();
		_avps.clear ();
		for (DiameterAVP avp : fromMessage._avps)
			_avps.add (clone ? (DiameterAVP) avp.clone () : avp);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#removeDiameterAVPs()
	 */
	public void removeDiameterAVPs() {
		if (!_parsed) throw new RuntimeException ();
		_avps.clear();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVPs()
	 */
	public Enumeration getDiameterAVPs() {
		if (!_parsed) throw new RuntimeException ();
		final Vector<DiameterAVP> list = new Vector<DiameterAVP>();
		list.addAll(_avps);
		return list.elements();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVPsSize()
	 */
	public int getDiameterAVPsSize() {
		if (!_parsed) throw new RuntimeException ();
		return _avps.size();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVPIndex(long,
	 *      long)
	 */
	public int getDiameterAVPIndex(long code, long vendorId) {
		if (SYNCHRONIZED){
			synchronized (_avps){
				return getDiameterAVPIndex_internal (code, vendorId);
			}
		} else return getDiameterAVPIndex_internal (code, vendorId);
	}
	private int getDiameterAVPIndex_internal(long code, long vendorId) {
		if (!_parsed) throw new RuntimeException ();
		int n = _avps.size ();
		for (int i = 0; i<n; i++){
			DiameterAVP avp = _avps.get (i);
			if (avp.getAVPCode() == code && avp.getVendorId() == vendorId) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterAVPIndex(com.nextenso.proxylet.diameter.DiameterAVPDefinition)
	 */
	public int getDiameterAVPIndex(DiameterAVPDefinition definition) {
		return getDiameterAVPIndex(definition.getAVPCode(), definition.getVendorId());
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterApplication()
	 */
	public long getDiameterApplication() {
		return _applicationId;
	}

	public void setDefaultOriginAVPs() {
		DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
		if (avp == null) {
			avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
			addDiameterAVP(avp);
		}
		avp.setValue(DEFAULT_ORIGIN, true);

		avp = getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
		if (avp == null) {
			avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
			addDiameterAVP(avp);
		}
		avp.setValue(DEFAULT_ORIGIN, true);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#setOriginHostAVP()
	 */
	public void setOriginHostAVP() {
		DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
		boolean hasChanged = false;
		String current = null;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setOriginHostAVP: existing Origin-Host AVP=" + avp);
		}
		if (avp == null) {
			avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
			addDiameterAVP(avp);
		} else {
			byte[] currentValue = avp.getValue();
			if (currentValue != null) {
				current = new String(currentValue);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("setOriginHostAVP: current value=" + current + ", default=" + DEFAULT_ORIGIN_STR);
				}

				if (!DEFAULT_ORIGIN_STR.equals(current)) {
					hasChanged = true;
				}
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setOriginHostAVP: hasChanged=" + hasChanged);
		}

		if (!hasChanged) {
			String originHost = getLocalOriginHost();
			byte[] avpValue = IdentityFormat.toIdentity(originHost);
			avp.setValue(avpValue, false);
		}

		if (hasChanged &&
		    getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES &&
		    getDiameterCommand () == DiameterBaseConstants.COMMAND_CER){
		    // in case the caps listener modified the originHost/Realm in the CER/CEA
		    Peer peer = isRequest () ? (Peer) getServerPeer () : (Peer) getClientPeer ();
		    if (peer == null) return; // possible for a subsequent CER/CEA
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug (peer+" : setting originHost to : "+current);
		    if (!peer.isLocalDiameterPeer ()){
			((RemotePeer) peer).setLocalOriginHost (current);
		    }
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#setOriginRealmAVP()
	 */
	public void setOriginRealmAVP() {
		DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setOriginRealmAVP: existing Origin-Realm AVP=" + avp);
		}

		boolean hasChanged = false;
		String current = null;
		if (avp == null) {
			avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
			addDiameterAVP(avp);
		} else {
			byte[] currentValue = avp.getValue();
			if(currentValue != null) {
			 current = new String(currentValue);
				if (!DEFAULT_ORIGIN_STR.equals(current)) {
					hasChanged = true;
				}
			}
		}

		if (!hasChanged) {
			String originRealm = getLocalOriginRealm();
			byte[] avpValue = IdentityFormat.toIdentity(originRealm);
			avp.setValue(avpValue, false);
		}

		if (hasChanged &&
		    getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES &&
		    getDiameterCommand () == DiameterBaseConstants.COMMAND_CER){
		    // in case the caps listener modified the originHost/Realm in the CER/CEA
		    Peer peer = isRequest () ? (Peer) getServerPeer () : (Peer) getClientPeer ();
		    if (peer == null) return; // possible for a subsequent CER/CEA
		    if (!peer.isLocalDiameterPeer ()){
			if (LOGGER.isDebugEnabled ())
			    LOGGER.debug (peer+" : setting originRealm to : "+current);
			((RemotePeer) peer).setLocalOriginRealm (current);			
		    }
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		if (isRequest()) {
			buff.append("DiameterRequest [\n");
		} else {
			buff.append("DiameterResponse [\n");
		}

		String application = Utils.getApplicationName(getDiameterApplication());
		if (application != null) {
			application = " (" + application + ")";
		} else {
			application = "";
		}

		buff.append('\t').append("Application-Id = 0x").append(Long.toHexString(getDiameterApplication())).append(application).append('\n');
		String command = Utils.getCommandName(this, true);
		command = Integer.toString(getDiameterCommand()) + " (" + command + ")";

		buff.append('\t').append("Command=").append(command).append('\n');
		buff.append('\t').append("Flags=");
		if (isRequest()) {
			buff.append(" REQ");
			if (hasFlag(DiameterRequestFacade.PROXIABLE_FLAG)) {
				buff.append(" PXY");
			}
			if (hasFlag(DiameterRequestFacade.RETRANSMITTED_FLAG)) {
				buff.append(" RETR");
			}
		} else {
			if (hasFlag(DiameterResponseFacade.E_FLAG)) {
				buff.append("ERR");
			} else {
				buff.append('-');
			}
		}
		buff.append('\n');
		buff.append('\t').append("HopByHop Identifier (client-side) = ").append(getClientHopIdentifier()).append('\n');
		buff.append('\t').append("HopByHop Identifier (server-side) = ").append(getServerHopIdentifier()).append('\n');
		buff.append('\t').append("EndToEnd Identifier = ").append(getEndIdentifier()).append('\n');
		buff.append('\t').append("Reception Address = ").append(getReceptionAddress()).append('\n');
		if (_parsed){
			for (DiameterAVP avp : _avps) {
				buff.append('\t').append(avp.toString()).append('\n');
			}
		} else {
			buff.append('\t').append ("AVP List : <not parsed>\n");
		}
		Enumeration enumeration = getAttributeNames ();
		while (enumeration.hasMoreElements ()){
		    Object name = enumeration.nextElement ();
		    buff.append ("\tAttribute[name=").append(name.toString ()).append(", value=").append(getAttribute (name).toString ()).append("]\n");
		}
		buff.append(']');
		return buff.toString();
	}

	private volatile byte[] _data;
	private volatile boolean _parsed = true;
	public void setData (byte[] data, int offset, int length){
		_data = new byte[length];
		System.arraycopy (data, offset, _data, 0, length);
		_parsed = false;
	}
	public void parseData() throws IOException {
		if (_parsed) return; // possible if a pxlet is ACCEPT_MAY_BLOCK
		readData (_data, 0, _data.length);
		_data = null;
	}

	public void readData(byte[] data, int offset, int length)
		throws IOException {
		_parsed = true;
		DiameterParser.Handler handler = new DiameterParser.Handler (){
				public boolean newAVP (long code, long vendorId, int flags, byte[] data, int off, int len){
					if (!isRequest () && code == 999L && vendorId == 0L){
					    // this is a hack in the diameter ioh to indicate local origin
					    getResponseFacade().setLocalOrigin(true);
					    setOriginHostAVP();
					    setOriginRealmAVP();
					    return false;
					}
					DiameterAVP avp = getDiameterAVP(code, vendorId);
					if (avp == null) {
						avp = new DiameterAVP(code, vendorId, flags);
						addDiameterAVP(avp);
					}
					avp.addValue(data, off, len, flags, true);
					return false;
				}
			};
		DiameterParser.INSTANCE.parse (data, offset, length, handler);
	}
	
	private byte[] sneaked = null;
	private byte[] sneakValue (final DiameterAVPDefinition def){
		if (_parsed) throw new IllegalStateException ("called sneakValue while already parsed");
		try{
			DiameterParser.Handler handler = new DiameterParser.Handler (){
					public boolean newAVP (long code, long vendorId, int flags, byte[] data, int off, int len){
						if (code == def.getAVPCode () &&
						    vendorId == def.getVendorId ()){
							sneaked = new byte[len];
							System.arraycopy (data, off, sneaked, 0, len);
							return true;
						}
						return false;
					}
							
				};
			DiameterParser.INSTANCE.parse (_data, 0, _data.length, handler);
			return sneaked;
		}catch(Exception e){
			return null;
		}finally{ sneaked = null; }
	}

	public void getBytes(ByteOutputStream out) {
		int offset = out.size ();
		int flags = getFlags();
		int command = getDiameterCommand();
		int appId = (int) getDiameterApplication();
		int hopIdentifier = getOutgoingClientHopIdentifier();
		int endIdentifier = getEndIdentifier();
		out.write(Utils.getVersion());

		// the length to be filled later
		out.write(EMPTY_LENGTH, 0, 3);
		out.write(flags);
		out.write(command >> 16);
		out.write(command >> 8);
		out.write(command);
		out.write(appId >> 24);
		out.write(appId >> 16);
		out.write(appId >> 8);
		out.write(appId);
		out.write(hopIdentifier >> 24);
		out.write(hopIdentifier >> 16);
		out.write(hopIdentifier >> 8);
		out.write(hopIdentifier);
		out.write(endIdentifier >> 24);
		out.write(endIdentifier >> 16);
		out.write(endIdentifier >> 8);
		out.write(endIdentifier);

		// append the avps
		Enumeration enumer = getDiameterAVPs();
		while (enumer.hasMoreElements()) {
			DiameterAVP avp = (DiameterAVP) enumer.nextElement();
			try {
				avp.getBytes(out);
			}
			catch (IOException e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("cannot getBytes write avp " + avp);
				}
			}
		}

		byte[] res = out.toByteArray(false);

		// set  the length in the buffer
		int len = out.size() - offset;
		res[offset+1] = (byte) ((len >> 16) & 0xFF);
		res[offset+2] = (byte) ((len >> 8) & 0xFF);
		res[offset+3] = (byte) (len & 0xFF);
	}

	public String getSessionId() {
		if (_parsed){
			DiameterAVP sessionIdAVP = getDiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
			if (sessionIdAVP != null) {
				return UTF8StringFormat.getUtf8String(sessionIdAVP.getValue());
			} else
				return null;
		} else {
			byte[] sneaked = sneakValue (DiameterBaseConstants.AVP_SESSION_ID);
			if (sneaked != null) {
				return UTF8StringFormat.getUtf8String(sneaked);
			} else
				return null;
		}
	}

	protected int getClientType() {
		DiameterApplication application = getApplication();
		if (application == null) {
			return 0;
		}
		if (application.isAccounting()) {
			return DiameterClient.TYPE_ACCT;
		}
		return DiameterClient.TYPE_AUTH;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getReceptionAddress()
	 */
	public InetSocketAddress getReceptionAddress() {
		return _receptionAddresses != null ? _receptionAddresses[0] : null;
	}
	public InetSocketAddress[] getReceptionAddresses() {
		return _receptionAddresses;
	}

	public void setReceptionAddresses(InetSocketAddress[] addresses) {
		_receptionAddresses = addresses;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getApplication()
	 */
	public DiameterApplication getApplication() {
		if (_application != null) {
			return _application;
		}

		DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
		if (avp != null && avp.getValueSize() > 0) {
			byte[] value = avp.getValue(0);
			List list = GroupedFormat.getGroupedAVPs(value, false);
			if (list.size() > 1) {
				avp = (DiameterAVP) list.get(0);
				if (avp.getValueSize() > 0 && avp.isInstanceOf(DiameterBaseConstants.AVP_VENDOR_ID)) {
					long vendorId = Unsigned32Format.getUnsigned32(avp.getValue(0), 0);
					avp = (DiameterAVP) list.get(1);
					if (avp.getValueSize() > 0) {
						long applicationId = Unsigned32Format.getUnsigned32(avp.getValue(0), 0);
						boolean isAuthentication = avp.isInstanceOf(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
						_application = new DiameterApplication(applicationId, vendorId, isAuthentication);
						return _application;
					}
				}
			}
		}

		avp = getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
		if (avp != null && avp.getValueSize() > 0) {
			long applicationId = Unsigned32Format.getUnsigned32(avp.getValue(0), 0);
			_application = new DiameterApplication(applicationId, 0, true);
			return _application;
		}

		avp = getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		if (avp != null && avp.getValueSize() > 0) {
			long applicationId = Unsigned32Format.getUnsigned32(avp.getValue(0), 0);
			_application = new DiameterApplication(applicationId, 0, false);
			return _application;
		}

		return null;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#setApplication(DiameterApplication)
	 */
	public void setApplication(DiameterApplication app){
		if(app != null && app.getApplicationId() != 0L){
			//set the new applicationID
			_applicationId = app.getApplicationId();
			//update the existing object with new changes
			_application = app;
			long vendorId = app.getVendorId();
			boolean appType = app.isAuthentication();

			//Remove existing avp's
			removeDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
			removeDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
			removeDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		
			if (vendorId != 0L) {	
				//Add the vendor-spec avp
				DiameterAVP vendorIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
				vendorIdAVP.addValue(Unsigned32Format.toUnsigned32(vendorId), false);
				DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
				List list = new java.util.ArrayList(2);
				list.add(vendorIdAVP);

				if (appType) {
					DiameterAVP authIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
					authIdAVP.addValue(Unsigned32Format.toUnsigned32(_applicationId), false);
					list.add(authIdAVP);
				}else{
					DiameterAVP acctIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
					acctIdAVP.addValue(Unsigned32Format.toUnsigned32(_applicationId), false);
					list.add(acctIdAVP);
				}
				avp.addValue(GroupedFormat.toGroupedAVP(list), false);
				addDiameterAVP(avp);
				return; 
			}

			if (appType) {
				//Add Auth type avp
				DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
				avp.addValue(Unsigned32Format.toUnsigned32(_applicationId), false);
				addDiameterAVP(avp);
			}else {

				//Add Acct type avp
				DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
				avp.addValue(Unsigned32Format.toUnsigned32(_applicationId), false);
				addDiameterAVP(avp);
			}


		} else{
			LOGGER.error("setApplication: Parameter DiameterApplication is null");
		}
	}

	/**
	 * @see com.nextenso.proxylet.impl.ProxyletDataImpl#resume(int)
	 */
	@Override
	public void resume(int status) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resume: status=" + status);
		}
		cancelSuspendListener();
		Utils.removePendingMessage();
		AsyncProxyletManager.resume(this, status);
	}
	
	public static class DiameterParser {
		public static DiameterParser INSTANCE = new DiameterParser ();
		public interface Handler {
			boolean newAVP (long code, long vendorId, int flags, byte[] data, int off, int len);
		}
		public void parse(byte[] data, int off, int len, Handler handler) throws IOException {
			int limit = off + len;
			if (limit > data.length){
				throw new EOFException ();
			}
			while (true) {
				if (len == 0) {
					return;
				}
				if (len < 8) {
					throw new ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_MESSAGE_LENGTH, "Invalid message length");
				}

				long code = Unsigned32Format.getUnsigned32(data, off);
				off += 4;
				int flags = data[off++] & 0xFF;
				int avpLen = data[off++] & 0xFF;
				avpLen <<= 8;
				avpLen |= data[off++] & 0xFF;
				avpLen <<= 8;
				avpLen |= data[off++] & 0xFF;

				int dataLen = avpLen - 8;

				boolean hasVendorId = DiameterAVP.vFlagSet(flags);
				long vendorId = 0L;
				if (hasVendorId) {
					if (len < 12) {
						throw new ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_LENGTH, "Invalid avp length", code, vendorId, flags);
					}
					vendorId = Unsigned32Format.getUnsigned32(data, off);
					off += 4;
					dataLen -= 4;
				}
				if (dataLen < 0) {
					throw new ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_LENGTH, "Invalid avp data length (<0)", code, vendorId, flags);
				}
				if (off + dataLen > limit){
					throw new ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_LENGTH, "Invalid avp data length (too large)", code, vendorId, flags);
				}
				
				len -= avpLen;
				
				// pad
				int neededZeros = (4 - avpLen % 4) % 4;
				if (neededZeros > 0) {
					len -= neededZeros;
					if (len < 0) throw new ParsingException (DiameterBaseConstants.RESULT_CODE_DIAMETER_INVALID_AVP_VALUE, "Invalid avp value (no padding)", code, vendorId, flags);
				}
				if (handler.newAVP (code, vendorId, flags, data, off, dataLen))
					return;
				
				off += dataLen + neededZeros;
			}
		}
	}

	public static class ParsingException extends IOException{
		private DiameterAVP _failedAVP;
		private long _result;
		public ParsingException (long result, String msg, DiameterAVPDefinition def){
			this (result, msg, def.getAVPCode (), def.getVendorId (), def.getRequiredFlags ());
		}
		public ParsingException (long result, String msg, long code, long vid, int flags){
			this (result, msg);
			_failedAVP = new DiameterAVP (DiameterBaseConstants.AVP_FAILED_AVP);
			DiameterAVP badAvp = new DiameterAVP (code, vid, flags);
			java.util.List list = new java.util.ArrayList (1);
			list.add (badAvp);
			byte[] value = GroupedFormat.INSTANCE.toGroupedAVP (list);
			_failedAVP.addValue (value, false);
		}
		public ParsingException (long result, String msg){
			super (msg);
			_result = result;
		}
		public long result (){ return _result;}
		public DiameterAVP failedAVP (){ return _failedAVP;}
		public DiameterAVP errMessageAVP (){
			if (getMessage () == null) return null;
			DiameterAVP avp = new DiameterAVP (DiameterBaseConstants.AVP_ERROR_MESSAGE);
			byte[] value = UTF8StringFormat.toUtf8String (getMessage ());
			avp.addValue (value, false);
			return avp;
		}
		public ParsingException setDefValue (byte[] b){
			_failedAVP.addValue (b, false);
			return this;
		}
	}

	public static void main (String[] s){
		DiameterRequestFacade req1 = new DiameterRequestFacade (null, 0, 0, 0, 0, 0, 0);
		DiameterRequestFacade req2 = new DiameterRequestFacade (null, 0, 0, 0, 0, 0, 0);

		DiameterAVP avp1 = new DiameterAVP (1, 1, 0);avp1.addValue (new byte[]{1, 1, 1, 1}, false);
		DiameterAVP avp2 = new DiameterAVP (2, 1, 0);avp2.addValue (new byte[]{1, 1, 1, 1}, false);
		DiameterAVP avp3 = new DiameterAVP (3, 1, 0);avp3.addValue (new byte[]{1, 1, 1, 1}, false);
				
		req1.addDiameterAVP (avp1);
		req1.addDiameterAVP (avp2);
		System.out.println (req1);

		req2.addDiameterAVP (avp1);
		req2.addDiameterAVP (avp1);
		req2.addDiameterAVP (avp2);
		req2.addDiameterAVP (avp3);
		req1.addDiameterAVPs (req2, false);
		System.out.println ("\n\n\n");
		System.out.println (req1);

		req1.setDiameterAVPs (req2, false);
		System.out.println ("\n\n\n");
		System.out.println (req1);

		req1.removeDiameterAVPs (1, 1);
		System.out.println ("\n\n\n");
		System.out.println (req1);
		
	}
}
