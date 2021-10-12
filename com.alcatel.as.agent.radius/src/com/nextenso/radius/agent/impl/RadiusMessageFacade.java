// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.AsyncProxyletManager;
import com.nextenso.proxylet.engine.AsyncProxyletManager.ProxyletResumer;
import com.nextenso.proxylet.impl.ProxyletDataImpl;
import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusMessage;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.VendorSpecificAttribute;
import com.nextenso.proxylet.radius.ExtendedTypeAttribute;
import com.nextenso.proxylet.radius.ExtendedVendorSpecificAttribute;
import com.nextenso.proxylet.radius.auth.CHAPPasswordAttribute;
import com.nextenso.proxylet.radius.event.AbortEvent;
import com.nextenso.radius.agent.Utils;
import com.nextenso.radius.agent.engine.RadiusProxyletContext;

/**
 * The generic Radius message.
 */
public abstract class RadiusMessageFacade
		extends ProxyletDataImpl
		implements RadiusMessage, ProxyletResumer {

	private static final long VENDOR_SPECIFIC_TYPE = RadiusUtils.VENDOR_SPECIFIC & 0XFFL; // stored as long
	private static final long VENDOR_SPECIFIC_TYPE_SHIFTED = VENDOR_SPECIFIC_TYPE << 8; // shift left
	protected static final byte[] VOID_16 = new byte[16];

	private int _code = -1;
	/**
	 * in network byte order (byte 0 at the left)
	 * - bytes 0, 1, 2, 3: vendor Id
	 * - byte 4 : flag : 0x11 ExtendedVendorSpecificAttribute / 0x01 ExtendedTypeAttribute / 0x10 VendorSpecificAttribute / 0x00 regular
	 * - byte 5 : ExtendedVendorSpecific type
	 * - byte 6 : extended type
	 * - byte 7 : type
	 */
	private Map<Long, RadiusAttribute> _radiusAttributes = new HashMap<Long, RadiusAttribute>();
	private int _clientIP, _clientPort=-1;
	private String _clientAddr, _clientHost;
	private RadiusServer _server;
	private byte[] _proxySecret;
	private MessageDigest _digest;
	private int _msgAuthenticatorOffset = -1;

	public RadiusMessageFacade() {
		super();
		RadiusProxyletContext context = (RadiusProxyletContext)Utils.getContainer().getContext();
		if (context != null) {
			setProxyletContext(context);
			setStaticListeners(context.getAccessRequestListeners());
		}

	}

	private long getKey (RadiusAttribute attr){
		long key = attr.getType () & 0xFFL;
		if (attr instanceof ExtendedTypeAttribute){
			ExtendedTypeAttribute extAttr = (ExtendedTypeAttribute) attr;
			key |= (extAttr.getExtendedType () & 0xFFL) << 8;
			if (extAttr instanceof ExtendedVendorSpecificAttribute){
				ExtendedVendorSpecificAttribute evs = (ExtendedVendorSpecificAttribute) extAttr;
				key |= (evs.getEVSType () & 0xFFL) << 16;
				key |= (evs.getVendorId () & 0xFFFFFFFFL) << 32;
				key |= 0x11000000L;
			} else {
				key |= 0x01000000L;
			}
		} else if (attr instanceof VendorSpecificAttribute){
			VendorSpecificAttribute vsa = (VendorSpecificAttribute) attr;
			key |= (vsa.getVendorId () & 0xFFFFFFFFL) << 32;
			key |= 0x10000000L;
		}
		return key;
	}
	
	protected MessageDigest getDigest() {
		if (_digest == null) {
			try {
				_digest = MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException("MD5 Algorithm is not supported");
			}

		}
		return _digest;
	}

	public abstract int getDefaultPort();

	protected abstract String getMessageType();

	public abstract String getSpecificAttributesToPrint();

	public abstract RadiusServer getRadiusServer(String host, int port, byte[] secret, int clientIP);

	public abstract String isValid();

	public abstract void writeTo(OutputStream out)
		throws IOException;

	public void readAttributes(byte[] in, int offset, int length)
		throws IOException {
		RadiusInputStream.readAttributes(this, in, offset, length);
	}

	public void setProxyStateAttribute(long ident) {
		RadiusAttribute ps = getRadiusAttribute(RadiusUtils.PROXY_STATE);
		if (ps == null) {
			ps = new RadiusAttribute(RadiusUtils.PROXY_STATE);
			addRadiusAttribute(ps);
		}
		byte[] b = new byte[8];
		Utils.setRequestId(ident, b, 0);
		ps.addValue(b, false);
	}

	public void setServer(String server, byte[] secret) {
		if (server == null) {
			setServer(null);
			return;
		}

		String theServer = server;
		int index = theServer.indexOf(':');
		int port = getDefaultPort();
		if (index != -1) {
			try {
				port = Integer.parseInt(theServer.substring(index + 1));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Invalid server name: " + theServer + " (cannot parse port number)");
			}
			theServer = server.substring(0, index);
		}
		setServer(new RadiusServer(theServer, port, secret, getClientIP(), true));
	}

	public void setCode(int code) {
		_code = code;
	}

	public void setIdentifier(int value) {
		setId(value);
	}

	public void setProxySecret(byte[] secret) {
		_proxySecret = Utils.copyArray(secret);
	}

	public void setClient(String ip_string, int ip_int, int port) {
		_clientAddr = ip_string;
		_clientIP = ip_int;
		_clientPort = port;
	}

	public void setServer(RadiusServer server) {
		_server = server;
	}

	public RadiusServer getServer() {
		return _server;
	}

	public int getCode() {
		return _code;
	}

	public int getIdentifier() {
		return getId();
	}

	public int getMessageAuthenticatorOffset (){
		return _msgAuthenticatorOffset;
	}

	public void setMessageAuthenticatorOffset (int i){
		_msgAuthenticatorOffset = i;
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getRadiusAttributes()
	 */
	public Enumeration getRadiusAttributes() {
		Vector<RadiusAttribute> v = new Vector<RadiusAttribute>();
		v.addAll(_radiusAttributes.values());
		return v.elements();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getRadiusAttributesSize()
	 */
	public int getRadiusAttributesSize() {
		return _radiusAttributes.size();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getRadiusAttribute(int)
	 */
	public RadiusAttribute getRadiusAttribute(int type) {
		switch (type){
		case RadiusUtils.VENDOR_SPECIFIC: throw new IllegalArgumentException("Use getVendorSpecificAttribute(int vendorId)");
		case ExtendedTypeAttribute.Extended_Type_1:
		case ExtendedTypeAttribute.Extended_Type_2:
		case ExtendedTypeAttribute.Extended_Type_3:
		case ExtendedTypeAttribute.Extended_Type_4:
		case ExtendedTypeAttribute.Long_Extended_Type_1:
		case ExtendedTypeAttribute.Long_Extended_Type_2:
			throw new IllegalArgumentException("Use getExtendedTypeAttribute(int type, int extendedType)");
		}
		return _radiusAttributes.get(type & 0xFFL);
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#removeRadiusAttribute(int)
	 */
	public RadiusAttribute removeRadiusAttribute(int type) {
		switch (type){
		case RadiusUtils.VENDOR_SPECIFIC: throw new IllegalArgumentException("Use removeVendorSpecificAttribute(int vendorId)");
		case ExtendedTypeAttribute.Extended_Type_1:
		case ExtendedTypeAttribute.Extended_Type_2:
		case ExtendedTypeAttribute.Extended_Type_3:
		case ExtendedTypeAttribute.Extended_Type_4:
		case ExtendedTypeAttribute.Long_Extended_Type_1:
		case ExtendedTypeAttribute.Long_Extended_Type_2:
			throw new IllegalArgumentException("Use removeExtendedTypeAttribute(int type, int extendedType)");
		}
		return _radiusAttributes.remove(type & 0xFFL);
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#addRadiusAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	public void addRadiusAttribute(RadiusAttribute attribute) {
		_radiusAttributes.put(getKey (attribute), attribute);
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getVendorSpecificAttribute(int)
	 */
	public VendorSpecificAttribute getVendorSpecificAttribute(int vendorId) {
		long key = 0x10000000L | VENDOR_SPECIFIC_TYPE;
		key |= (vendorId & 0xFFFFFFFFL) << 32;
		return (VendorSpecificAttribute) _radiusAttributes.get(key);
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#removeVendorSpecificAttribute(int)
	 */
	public VendorSpecificAttribute removeVendorSpecificAttribute(int vendorId) {
		long key = 0x10000000L | VENDOR_SPECIFIC_TYPE;
		key |= (vendorId & 0xFFFFFFFFL) << 32;
		return (VendorSpecificAttribute) _radiusAttributes.remove(key);
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#addVendorSpecificAttribute(com.nextenso.proxylet.radius.VendorSpecificAttribute)
	 */
	public void addVendorSpecificAttribute(VendorSpecificAttribute attribute) {
		addRadiusAttribute (attribute);
	}
	
	public ExtendedTypeAttribute getExtendedTypeAttribute(int type, int extendedType){
		if (extendedType == RadiusUtils.VENDOR_SPECIFIC)
			throw new IllegalArgumentException ("Use getExtendedVendorSpecificAttribute(int type, int vendorId, int evsType)");
		long key = type & 0xFFL;
		key |= (extendedType & 0xFFL) << 8;
		key |= 0x01000000L;
		return (ExtendedTypeAttribute) _radiusAttributes.get (key);
	}
	public ExtendedTypeAttribute removeExtendedTypeAttribute(int type, int extendedType){
		if (extendedType == RadiusUtils.VENDOR_SPECIFIC)
			throw new IllegalArgumentException ("Use removeExtendedVendorSpecificAttribute(int type, int vendorId, int evsType)");
		long key = type & 0xFFL;
		key |= (extendedType & 0xFFL) << 8;
		key |= 0x01000000L;
		return (ExtendedTypeAttribute) _radiusAttributes.remove (key);
	}
	
	public ExtendedVendorSpecificAttribute getExtendedVendorSpecificAttribute(int type, int vendorId, int evsType){
		long key = type & 0xFFL;
		key |= VENDOR_SPECIFIC_TYPE_SHIFTED;
		key |= (evsType & 0xFFL) << 16;
		key |= 0x11000000L;
		key |= (vendorId & 0xFFFFFFFFL) << 32;
		return (ExtendedVendorSpecificAttribute) _radiusAttributes.get (key);
	}
	public ExtendedVendorSpecificAttribute removeExtendedVendorSpecificAttribute(int type, int vendorId, int evsType){
		long key = type & 0xFFL;
		key |= VENDOR_SPECIFIC_TYPE_SHIFTED;
		key |= (evsType & 0xFFL) << 16;
		key |= 0x11000000L;
		key |= (vendorId & 0xFFFFFFFFL) << 32;
		return (ExtendedVendorSpecificAttribute) _radiusAttributes.remove (key);
	}
	

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#removeRadiusAttributes()
	 */
	public void removeRadiusAttributes() {
		_radiusAttributes.clear();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getClientAddr()
	 */
	public String getClientAddr() {
		return _clientAddr;
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getClientHost()
	 */
	public String getClientHost() {
		return (_clientHost != null) ? _clientHost : _clientAddr;
	}

	public int getClientIP() {
		return _clientIP;
	}

	public int getClientPort(){
		return _clientPort;
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getProxySecret()
	 */
	public byte[] getProxySecret() {
	    // TODO check if copy is really needed
		return Utils.copyArray(_proxySecret);
	}
	public String getProxySecretAsString() {
	    try{
		byte[] secret = getProxySecret ();
		return secret != null ? new String (secret, "ascii") : null;
	    }catch(Exception e){return null;}
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getServerAddr()
	 */
	public String getServerAddr() {
		if (_server == null)
			return null;
		String addr = _server.getHostAddress();
		return (addr != null) ? addr : _server.getName();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getServerHost()
	 */
	public String getServerHost() {
		if (_server == null)
			return null;
		String host = _server.getName();
		return (host != null) ? host : _server.getHostAddress();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getServerPort()
	 */
	public int getServerPort() {
		if (_server == null)
			return -1;
		return _server.getPort();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#getServerSecret()
	 */
	public byte[] getServerSecret() {
		if (_server == null) {
			return null;
		}

		return _server.getSecret();
	}
	public String getServerSecretAsString() {
	    try{
		byte[] secret = getServerSecret ();
		return secret != null ? new String (secret, "ascii") : null;
	    }catch(Exception e){return null;}
	}

	public void writeResultTo(ByteBuffer buffer, int len, byte[] authenticator, OutputStream out)
		throws IOException {
		out.write(buffer.toByteArray(false), 0, 4);
		out.write(authenticator);
		if (len > 20) {
			// skip authenticator
			out.write(buffer.toByteArray(false), 20, len - 20);
		}
	}

	public void writePrologTo(OutputStream out)
		throws IOException {
		writePrologTo(out, getLength());
	}

	public void writePrologTo(OutputStream out, int len)
		throws IOException {
		out.write(getCode());
		out.write(getId());
		out.write(len >> 8);
		out.write(len);
	}

	public void writeAttributesTo(OutputStream out)
		throws IOException {
		byte[] value;
		int off = 0;
		_msgAuthenticatorOffset = -1; // reset it
		for (RadiusAttribute att : _radiusAttributes.values ()){
			int type = att.getType();
			// the attribute may be multivalued
			switch (type) {
				case RadiusUtils.CHAP_PASSWORD:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						out.write(RadiusUtils.CHAP_PASSWORD);
						out.write(3 + value.length);
						out.write(((CHAPPasswordAttribute) att).getCHAPIdentifier());
						out.write(value, 0, value.length);
						off += 3 + value.length;
					}
					break;
				case RadiusUtils.VENDOR_SPECIFIC:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						int vendorId = ((VendorSpecificAttribute) att).getVendorId();
						// value cannot be null
						out.write(RadiusUtils.VENDOR_SPECIFIC);
						out.write(6 + value.length);
						out.write(vendorId >> 24);
						out.write(vendorId >> 16);
						out.write(vendorId >> 8);
						out.write(vendorId);
						out.write(value, 0, value.length);
						off += 6 + value.length;
					}
					break;
				case RadiusUtils.MESSAGE_AUTHENTICATOR:
					_msgAuthenticatorOffset = off + 2;
					out.write(type);
					out.write(2 + 16);
					out.write(VOID_16);
					off += 2 + 16;
					break;
				case ExtendedTypeAttribute.Extended_Type_1:
				case ExtendedTypeAttribute.Extended_Type_2:
				case ExtendedTypeAttribute.Extended_Type_3:
				case ExtendedTypeAttribute.Extended_Type_4:
					boolean isEVS = (att instanceof ExtendedVendorSpecificAttribute);
					ExtendedTypeAttribute ext = (ExtendedTypeAttribute) att;
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						int valueLen = value.length;
						if (isEVS) valueLen += 5;
						out.write(type);
						out.write(3 + valueLen);
						out.write (ext.getExtendedType ());
						if (isEVS){
							ExtendedVendorSpecificAttribute evs = (ExtendedVendorSpecificAttribute) att;
							int vendorId = evs.getVendorId();
							out.write(vendorId >> 24);
							out.write(vendorId >> 16);
							out.write(vendorId >> 8);
							out.write(vendorId);
							out.write(evs.getEVSType ());
						}
						out.write(value, 0, value.length);
						off += 3 + valueLen;
					}
					break;
				case ExtendedTypeAttribute.Long_Extended_Type_1:
				case ExtendedTypeAttribute.Long_Extended_Type_2:
					isEVS = (att instanceof ExtendedVendorSpecificAttribute);
					ext = (ExtendedTypeAttribute) att;
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						int valueLen = value.length;
						if (isEVS) valueLen += 5;
						int fullblocks = valueLen / 251;
						int remaining = valueLen % 251;
						boolean exact = remaining == 0;
						int valueOff = 0;
						for (int k=1; k<=fullblocks; k++){
							out.write(type);
							out.write(255);
							out.write (ext.getExtendedType ());
							if (exact && k == fullblocks) // lastFullBlock, no remaining
								out.write (0x0);
							else
								out.write (0x80);
							int valueLenToWrite = 251;
							if (isEVS && k == 1){
								ExtendedVendorSpecificAttribute evs = (ExtendedVendorSpecificAttribute) att;
								int vendorId = evs.getVendorId();
								out.write(vendorId >> 24);
								out.write(vendorId >> 16);
								out.write(vendorId >> 8);
								out.write(vendorId);
								out.write(evs.getEVSType ());
								valueLenToWrite = 246;
							}
							out.write(value, valueOff, valueLenToWrite);
							valueOff += valueLenToWrite;
							off += 255;
						}
						if (!exact){
							boolean needToWriteEVSInfo = (isEVS && fullblocks == 0);
							out.write(type);
							out.write(4 + remaining);
							out.write (ext.getExtendedType ());
							out.write (0x0);
							if (needToWriteEVSInfo){
								ExtendedVendorSpecificAttribute evs = (ExtendedVendorSpecificAttribute) att;
								int vendorId = evs.getVendorId();
								out.write(vendorId >> 24);
								out.write(vendorId >> 16);
								out.write(vendorId >> 8);
								out.write(vendorId);
								out.write(evs.getEVSType ());
							}
							out.write(value, valueOff, value.length - valueOff);
							off += 4 + remaining;
						}
					}
					break;
				default:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						out.write(type);
						out.write(2 + value.length);
						out.write(value, 0, value.length);
						off += 2 + value.length;
					}
					break;
			}
		}
	}

	public int getLength() {
		return (20 + getAttributeLength());
	}

	public int getAttributeLength() {
		int len = 0;
		byte[] value;
		for (RadiusAttribute att : _radiusAttributes.values ()){
			int type = att.getType();
			switch (type) {
				case RadiusUtils.CHAP_PASSWORD:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						len += 3 + value.length;
					}
					break;
				case RadiusUtils.VENDOR_SPECIFIC:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						len += 6 + value.length;
					}
					break;
				case ExtendedTypeAttribute.Extended_Type_1:
				case ExtendedTypeAttribute.Extended_Type_2:
				case ExtendedTypeAttribute.Extended_Type_3:
				case ExtendedTypeAttribute.Extended_Type_4:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						if (att instanceof ExtendedVendorSpecificAttribute){
							len += 8 + value.length;
						} else {
							len += 3 + value.length;
						}
					}
					break;
				case ExtendedTypeAttribute.Long_Extended_Type_1:
				case ExtendedTypeAttribute.Long_Extended_Type_2:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						int valueLen = value.length;
						if (att instanceof ExtendedVendorSpecificAttribute){
							valueLen += 5;
						}
						int fullblocks = valueLen / 251;
						int remaining = valueLen % 251;
						len += fullblocks*255;
						if (remaining > 0) len += 4 + remaining;
					}
					break;
				default:
					for (int i = 0; i < att.getValueSize(); i++) {
						value = att.getValue(i);
						// value cannot be null
						len += 2 + value.length;
					}
					break;
			}
		}
		return len;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("Code:").append(String.valueOf(getCode())).append("\t").append(getMessageType());
		buff.append("\nIdentifier:").append(String.valueOf(getIdentifier())).append('\n');
		buff.append(getSpecificAttributesToPrint());

		for (RadiusAttribute att : _radiusAttributes.values ()){
			buff.append(att.toString());
			buff.append('\n');
		}
		return buff.toString();
	}

	public void abort() {
		fireProxyletEvent(new AbortEvent(getProxyletContext(), this), true);
	}
	
	/**
	 * @see com.nextenso.proxylet.engine.AsyncProxyletManager.ProxyletResumer#resumeProxylet(com.nextenso.proxylet.ProxyletData,
	 *      int)
	 */
	@Override
	public void resumeProxylet(ProxyletData message, int status) {
		Utils.getEngine().resume(this, status);
	}

	/**
	 * @see com.nextenso.proxylet.impl.ProxyletDataImpl#resume(int)
	 */
	@Override
	public void resume(int status) {
		cancelSuspendListener();
		AsyncProxyletManager.resume(this, status);
	}

}
