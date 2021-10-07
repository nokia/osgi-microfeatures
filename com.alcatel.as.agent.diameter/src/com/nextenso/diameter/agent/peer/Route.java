package com.nextenso.diameter.agent.peer;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRoute;
import com.nextenso.proxylet.diameter.client.DiameterClient;

public class Route
		implements DiameterRoute {

	private static final int NO_MATCH = -1;
	private static final int DEFAULT_MATCH = 1;
	private static final int MEDIUM_MATCH = 2;
	private static final int EXACT_MATCH = 3;

	private String _destinationRealm; // null means any
	private String _privateRealm;
	private long _applicationId; // -1 means any
	private int _type;
	private DiameterPeer _peer;
	private long _id = -1;
	private int _metrics = Integer.MAX_VALUE;

	public Route(String destinationRealm, long applicationId, int type, int metrics) {
		_destinationRealm = destinationRealm;
		_privateRealm = Utils.formatRealm(destinationRealm);
		_applicationId = applicationId;
		_type = type;
		setMetrics(metrics);
	}

	public Route(String handlerName, long id, String destinationRealm, long applicationId, long peerId, int type) {
		this(handlerName, id, destinationRealm, applicationId, peerId, type, -1);
	}

	public Route(String handlerName, long id, String destinationRealm, long applicationId, long peerId, int type, int metrics) {
		this(destinationRealm, applicationId, type, metrics);
		_id = id;
		_peer = Utils.getTableManager().getDiameterPeerById(handlerName, peerId);
		if (_peer == null) {
			throw new IllegalArgumentException("Invalid Route : Unknown routing-peer: " + peerId);
		}
	}

	public Route(DiameterPeer routingPeer, String destinationRealm, long applicationId, int type) {
		this(routingPeer, destinationRealm, applicationId, type, -1);
	}

	public Route(DiameterPeer routingPeer, String destinationRealm, long applicationId, int type, int metrics) {
		this(destinationRealm, applicationId, type, metrics);
		_peer = routingPeer;
		if (_peer == null) {
			throw new IllegalArgumentException("Invalid Route: no defined routing peer");
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRoute#getApplicationId()
	 */
	public long getApplicationId() {
		return _applicationId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRoute#getApplicationType()
	 */
	public int getApplicationType() {
		return _type;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRoute#getDestinationRealm()
	 */
	public String getDestinationRealm() {
		return _destinationRealm;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRoute#getRoutingPeer()
	 */
	public DiameterPeer getRoutingPeer() {
		return _peer;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRoute#match(java.lang.String,
	 *      long, int)
	 */
	public boolean match(String destinationRealm, long applicationId, int applicationType) {
		return (match(applicationId) > NO_MATCH &&
			match(applicationType)> NO_MATCH &&
			match(destinationRealm) > NO_MATCH);
	}

	
	public long getId() {
		return _id;
	}

	private int match(String realm) {
		if (realm == null || _destinationRealm == null) return DEFAULT_MATCH;
		realm = Utils.formatRealm(realm);
		if (realm.equals (_privateRealm)) return EXACT_MATCH;
		if (realm.endsWith(_privateRealm)) return MEDIUM_MATCH;
		return NO_MATCH;
	}

	private int match(long appId) {
		if (_applicationId == -1 || appId == -1) return DEFAULT_MATCH;
		if (_applicationId == appId) return EXACT_MATCH;
		return NO_MATCH;
	}

	private int match(int type) {
		if (type == 0) return DEFAULT_MATCH;
		if ((_type & type) == type) return EXACT_MATCH;
		return NO_MATCH;
	}

	public int score(String realm, long appId, int type) {
		int matchApp = match (appId);
		if (matchApp == NO_MATCH) return 0;
		int matchType = match (type);
		if (matchType == NO_MATCH) return 0;
		int matchRealm = match (realm);
		if (matchRealm == NO_MATCH) return 0;
		
		return matchApp + matchType + matchRealm;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("[Route: ");
		buff.append("destination-realm=").append((_destinationRealm != null) ? _destinationRealm : "<any>");
		if (_applicationId == -1L) {
			buff.append(", application-id=<any>");
		} else {
			buff.append(", application-id=" + _applicationId);
		}

		buff.append(", metrics=").append(getMetrics());
		buff.append(", type=");
		if (_type == DiameterClient.TYPE_ACCT) {
			buff.append("acct");
		} else if (_type == DiameterClient.TYPE_AUTH) {
			buff.append("auth");
		} else {
			buff.append("acct/auth");
		}
		buff.append(", peer=").append(_peer);
		buff.append(", connected=").append(_peer.isConnected());
		buff.append(", quarantine=").append(_peer.isQuarantined());
		buff.append(']');
		return buff.toString();
	}

	public static int getMaxScore() {
		return 3 * EXACT_MATCH;
	}

	public void setMetrics(int metrics) {
		if (metrics >= 0) {
			_metrics = metrics;
		} else {
			_metrics = Integer.MAX_VALUE;
		}
	}

	public int getMetrics() {
		return _metrics;
	}
}
