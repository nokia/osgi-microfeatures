// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.charging.ro;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.ImsClient;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingUtils;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientFactory;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The abstract Rf client.
 */
public abstract class AbstractRoClient
		implements ImsClient, Externalizable {

	private final static Logger LOGGER = Logger.getLogger("3gpp.interfaces.ro");
	private final static long VENDOR_ID = 0;
	private final static long APPLICATION_ID = 4;
	static protected final long serialVersionUID = 1L;

	protected static enum State {
		INIT,
		START,
		STOP
	}

	private State _state = State.INIT;

	private Version _version;
	private DiameterClient _client;
	private final AtomicLong _nextRecordNumber = new AtomicLong(0L);
	private String _serviceContextId;
	private String _realm;
	private final List<String> _servers = new ArrayList<String>();

	protected AbstractRoClient() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param servers
	 * @param realm
	 * @param serviceContextId
	 * @param version The 32.299 version.
	 * @throws NoRouteToHostException
	 */
	public AbstractRoClient(Iterable<String> servers, String realm, String serviceContextId, Version version)
			throws NoRouteToHostException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new AbstractRoClient: servers=" + servers + ", realm=" + realm + ", id=" + serviceContextId + ", version=" + version);
		}

		setVersion(version);
		setServiceContextId(serviceContextId);
		_realm = realm;
		if (servers != null) {
			for (String s : servers) {
				_servers.add(s);
			}
		}
		createClient(null);
	}

	private void createClient(String sessionId)
		throws NoRouteToHostException {
		// TODO create a new client with the sessionId
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("createClient: session id=" + sessionId);
		}

		DiameterClientFactory factory = DiameterClientFactory.getDiameterClientFactory();
		if (factory == null) {
			throw new NoRouteToHostException("Cannot get the Diameter Client Factory");
		}

		if (_servers.isEmpty()) {
			try {
				setDiameterClient(factory, null, sessionId);
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("cannot access to the realm=" + _realm, e);
				}
			}
			if (getDiameterClient() == null) {
				throw new NoRouteToHostException("Cannot access any Ro server");
			}
			return;
		}

		Iterator<String> iter = _servers.iterator();
		while (getDiameterClient() == null && iter.hasNext()) {
			String server = iter.next();
			try {
				setDiameterClient(factory, server, sessionId);
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("cannot access to the server=" + server, e);
				}
			}
		}
		if (getDiameterClient() == null) {
			throw new NoRouteToHostException("Cannot access any Ro server");
		}
	}

	private void setDiameterClient(DiameterClientFactory factory, String server, String sessionId)
		throws NoRouteToHostException {
		DiameterClient client = null;
		if (sessionId != null) {
			client = factory.newDiameterClient(server, _realm, VENDOR_ID, APPLICATION_ID, DiameterClient.TYPE_AUTH, sessionId, 0);
		} else {
			client = factory.newDiameterClient(server, _realm, VENDOR_ID, APPLICATION_ID, DiameterClient.TYPE_AUTH, true, 0);
		}
		setDiameterClient(client);
	}

	/**
	 * Gets the Service-Context-Id.
	 * 
	 * @return The Service-Context-Id.
	 */
	public String getServiceContextId() {
		return _serviceContextId;
	}

	/**
	 * Sets the Service-Context-Id.
	 * 
	 * @param serviceContextId The Service-Context-Id.
	 */
	protected void setServiceContextId(String serviceContextId) {
		if (serviceContextId == null) {
			throw new IllegalArgumentException("serviceContextId cannot be null");
		}
		_serviceContextId = serviceContextId;
	}

	/**
	 * Gets the state.
	 * 
	 * @return The state.
	 */
	protected State getState() {
		return _state;
	}

	protected void setState(State state) {
		_state = state;
	}

	/**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		out.writeUTF(getState().name());
		out.writeObject(getVersion());
		out.writeLong(_nextRecordNumber.get());
		if (getServiceContextId() != null) {
			out.writeBoolean(true);
			out.writeUTF(getServiceContextId());
		} else {
			out.writeBoolean(false);
		}
		if (_realm != null) {
			out.writeBoolean(true);
			out.writeUTF(_realm);
		} else {
			out.writeBoolean(false);
		}
		out.writeInt(_servers.size());
		for (String s : _servers) {
			out.writeUTF(s);
		}
		if (getDiameterClient() != null && getDiameterClient().getDiameterSession() != null) {
			out.writeBoolean(true);
			out.writeUTF(getDiameterClient().getDiameterSession().getSessionId());
		} else {
			out.writeBoolean(false);
		}

	}

	/**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		setState(State.valueOf(in.readUTF()));
		setVersion((Version) in.readObject());
		long l = in.readLong();
		_nextRecordNumber.set(l);
		boolean hasValue = in.readBoolean();
		if (hasValue) {
			setServiceContextId(in.readUTF());
		}
		hasValue = in.readBoolean();
		if (hasValue) {
			_realm = in.readUTF();
		}
		int nb = in.readInt();
		for (int i = 0; i < nb; i++) {
			_servers.add(in.readUTF());
		}
		hasValue = in.readBoolean();
		String sessionId = null;
		if (hasValue) {
			sessionId = in.readUTF();
		}

		createClient(sessionId);
	}

	/**
	 * Creates a new CCR.
	 * 
	 * @param type The type of the CCR.
	 * @return The new CCR.
	 */
	protected Ccr createCcr(CcRecordType type) {
		if (type == null) {
			throw new NullPointerException("cannot create a new CCR without a type");
		}

		long accountingRecordNumber = getRecordNumber();
		DiameterClientRequest request = _client.newAuthRequest(ChargingConstants.COMMAND_CC, true);

		DiameterAVP avp = new DiameterAVP(ChargingUtils.getServiceContextIdAVP());
		avp.setValue(UTF8StringFormat.toUtf8String(getServiceContextId()), false);
		request.addDiameterAVP(avp);

		avp = new DiameterAVP(ChargingUtils.getCcRequestTypeAVP());
		avp.setValue(EnumeratedFormat.toEnumerated(type.getValue()), false);
		request.addDiameterAVP(avp);

		Ccr res = new Ccr(request, getVersion());
		res.setRequestNumber(accountingRecordNumber);
		
		return res;
	}

	/**
	 * Creates a new request.
	 * 
	 * @param type The type of the request to be created.
	 * @return
	 */
	protected DiameterClientRequest newRequest(ChargingConstants.CcRecordType type) {
		long accountingRecordNumber = getRecordNumber();
		DiameterClientRequest request = _client.newAuthRequest(ChargingConstants.COMMAND_CC, true);

		DiameterAVP avp = new DiameterAVP(ChargingUtils.getCcRequestTypeAVP());
		avp.setValue(EnumeratedFormat.toEnumerated(type.getValue()), false);
		request.addDiameterAVP(avp);

		avp = new DiameterAVP(ChargingUtils.getCcRequestNumberAVP());
		avp.setValue(Unsigned32Format.toUnsigned32(accountingRecordNumber), false);
		request.addDiameterAVP(avp);

		return request;
	}

	/**
	 * Gets the record number.
	 * 
	 * @return The record number.
	 */
	protected long getRecordNumber() {
		return _nextRecordNumber.getAndIncrement();
	}

	/**
	 * Sets the version.
	 * 
	 * @param version The version.
	 */
	public void setVersion(Version version) {
		_version = version;
	}

	/**
	 * Gets the version
	 * 
	 * @return The version
	 */
	public Version getVersion() {
		return _version;
	}

	/**
	 * Sets the client.
	 * 
	 * @param client The client.
	 */
	private  void setDiameterClient(DiameterClient client) {
		_client = client;
	}

	/**
	 * Gets the client.
	 * 
	 * @return The client.
	 */
	public DiameterClient getDiameterClient() {
		return _client;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.ImsClient#close()
	 */
	public void close() {
		getDiameterClient().close();
	}
	
	/**
	 * @see com.alcatel_lucent.as.ims.diameter.ImsClient#getDiameterSession()
	 */
	public DiameterSession getDiameterSession() {
		if (_client != null) {
			return _client.getDiameterSession();
		}
		return null;
	}

}
