package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.xml.RoutesParser;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRoute;

public class RouteTable {

	private static String DTD_CONTEXT = "diameterAgent/diameterRoutes.dtd";

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.routetable");
	private final Map<String, ScoredRouteSet> _scoreRoutes = Utils.newConcurrentHashMap();
	private static final String DELIMITER = "%%";

	private List<Route> _routes = new ArrayList<Route>();

	private String _handlerName = null;

	public static class ScoredRouteSet {

		private String _realm;
		private long _appId;
		private int _type;

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder res = new StringBuilder("[ScoredRouteSet: roundRobinKey=");
			res.append(_roundRobinKey).append(", roundRobinIndex=").append(_roundRobinIndex);
			res.append(", routes=");
			for (Integer i : _sortedRoutes.keySet()) {
				res.append("\nscore=").append(i);
				for (Route route : _sortedRoutes.get(i)) {
					res.append("\n\troute=").append(route);
				}
			}
			res.append("]");
			return res.toString();
		}

		private final Map<Integer, List<Route>> _sortedRoutes = Utils.newConcurrentHashMap();
		private int _roundRobinKey = 0;
		private int _roundRobinIndex = 0;

		public ScoredRouteSet(String realm, long appId, int type, List<Route> routes) {
			_realm = realm;
			_appId = appId;
			_type = type;
			for (Route route : routes) {
				addRoute(route);
			}
		}

		public void addRoute(Route route) {
			int score = route.score(_realm, _appId, _type);
			if (score > 0) {
				addRoute(score, route);
			}
		}

		public void removeRoute(Route route) {
			for (int i = 3 * Route.getMaxScore(); i > 0; i--) {
				Integer key = Integer.valueOf(i);
				List<Route> list = _sortedRoutes.get(key);
				if (list != null) {
					list.remove(route);
				}
			}

		}

		private void addRoute(int score, Route route) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("message: score=" + score + ", route=" + route);
			}
			Integer key = Integer.valueOf(score);
			List<Route> list = _sortedRoutes.get(key);
			if (list == null) {
				list = new ArrayList<Route>();
				_sortedRoutes.put(key, list);
			}

			int i = 0;
			boolean isAdded = false;
			while (i < list.size() && !isAdded) {
				if (list.get(i).getMetrics() > route.getMetrics()) {
					list.add(i, route);
					isAdded = true;
				} else {
					i++;
				}
			}
			if (!isAdded) {
				list.add(route);
			}
		}

		public Route getRoute() {
			for (int i = 3 * Route.getMaxScore(); i > 0; i--) {
				Integer key = Integer.valueOf(i);
				List<Route> list = _sortedRoutes.get(key);
				if (list != null && !list.isEmpty()) {
					int size = list.size();
					int nextIndex = 0;

					int currentMetrics = Integer.MAX_VALUE;
					if (_roundRobinKey == i) {
						if (_roundRobinIndex >= size) {
							_roundRobinIndex = 0;
						}
						currentMetrics = list.get(_roundRobinIndex).getMetrics();

						// search for a route with better metrics (less than current)
						int j = 0;
						for (; j < _roundRobinIndex && list.get(j).getMetrics() < currentMetrics; j++) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("getRoute: trying 1 (high priority) index=" + j);
							}
							Route route = list.get(j);
							if (route.getRoutingPeer().isConnected() && ! route.getRoutingPeer().isQuarantined()) {
								_roundRobinIndex = j;
								if (LOGGER.isDebugEnabled()) {
									LOGGER.debug("getRoute: 1 _roundRobinIndex becomes " + _roundRobinIndex);
								}
								return route;
							}
						}

						int firstWithMetrics = j;
						// find the last with this metrics
						int lastWithMetrics = firstWithMetrics;
						while (lastWithMetrics + 1 < size) {
							if (list.get(lastWithMetrics + 1).getMetrics() == currentMetrics) {
								lastWithMetrics++;
							} else {
								break;
							}
						}
						int nbWithTheMetrics = lastWithMetrics - firstWithMetrics + 1;
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("getRoute: firstWithMetrics=" + firstWithMetrics + ", lastWithMetrics=" + lastWithMetrics + ", nbWithTheMetrics="
									+ nbWithTheMetrics);
						}
						//last with these metrics; must try others with same metrics
						for (int k = 0; k < nbWithTheMetrics; k++) {
							int index = firstWithMetrics + (_roundRobinIndex - firstWithMetrics + 1 + k) % nbWithTheMetrics;
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("getRoute: trying 2 (same priority) index=" + index);
							}
							Route route = list.get(index);
							if (route.getRoutingPeer().isConnected() && ! route.getRoutingPeer().isQuarantined()) {
								_roundRobinIndex = index;
								if (LOGGER.isDebugEnabled()) {
									LOGGER.debug("getRoute: 2 _roundRobinIndex becomes " + _roundRobinIndex);
								}
								return route;
							}
						}

						nextIndex = lastWithMetrics + 1;
					}

					//search for next in round robin
					while (nextIndex < size) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("getRoute: trying 3 (low priority) index=" + nextIndex);
						}
						Route route = list.get(nextIndex);
						if (route.getRoutingPeer().isConnected() && ! route.getRoutingPeer().isQuarantined()) {
							_roundRobinKey = i;
							_roundRobinIndex = nextIndex;
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("getRoute: 3 _roundRobinIndex becomes " + _roundRobinIndex);
							}
							return route;
						}
						nextIndex++;
					}
				}
			}
			return null;
		}

		public List<Route> getRoutes() {
			List<Route> res = new ArrayList<Route>();
			for (int i = 3 * Route.getMaxScore(); i > 0; i--) {
				Integer key = Integer.valueOf(i);
				List<Route> list = _sortedRoutes.get(key);
				if (list != null) {
					res.addAll(list);
				}
			}
			return res;
		}

	}

	public RouteTable(String handlerName) {
		_handlerName = handlerName;
	}

	public void init()
		throws ConfigException {
		String xml = DiameterProperties.getRoutesXml();
		init(xml);
	}

	public void init(String xml)
		throws ConfigException {
		RoutesParser parser = new RoutesParser(xml, DTD_CONTEXT);
		List<Route> routes = parser.parseRoutes(_handlerName);

		_routes.clear();
		_routes.addAll(routes);

		if (LOGGER.isInfoEnabled()) {
			for (Route route : _routes) {
				LOGGER.info("Defined: " + route);
			}
		}
		_scoreRoutes.clear();
	}

	public void clear() {
		_routes.clear();
		_scoreRoutes.clear();
	}

	/**
	 * Gets the peer to send a message to according to parameter values.
	 * 
	 * @param realm The realm.
	 * @param appId The application identifier.
	 * @param type The type of the message to be sent.
	 * @return The route to use
	 */
	public Route getRoute(String realm, long appId, int type) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getRoute: realm=" + realm + ", appId=" + appId + ", type=" + type);
		}
		String key = getKey(realm, appId, type);
		ScoredRouteSet set = _scoreRoutes.get(key);
		if (set == null) {
			set = new ScoredRouteSet(realm, appId, type, _routes);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("getRoute: create a new scored route set: " + set);
			}

			_scoreRoutes.put(key, set);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getRoute: using the scored route=" + set);
		}
		Route route = set.getRoute();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getRoute: res=" + route);
		}
		return route;
	}

	private ScoredRouteSet getScoredRouteSet(String realm, long appId, int type) {
		String key = getKey(realm, appId, type);
		ScoredRouteSet res = _scoreRoutes.get(key);
		if (res == null) {
			res = new ScoredRouteSet(realm, appId, type, _routes);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("getScoredRouteSet: create a new scored route for realm=" + realm + ", app id=" + appId + ", type=" + type);
			}

			_scoreRoutes.put(key, res);
		}

		return res;
	}

	public List<DiameterRoute> getRoutes(String realm, long appId, int type) {
		List<DiameterRoute> res = new ArrayList<DiameterRoute>();
		ScoredRouteSet set = getScoredRouteSet(realm, appId, type);
		if (set != null) {
			res.addAll(set.getRoutes());
		}
		return res;
	}

	/**
	 * Adds a route.
	 * 
	 * @param route The route to be added.
	 */
	public void addRoute(Route route) {
		_routes.add(route);
		for (ScoredRouteSet set : _scoreRoutes.values()) {
			set.addRoute(route);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addRoute: a route has been added: " + route);
		}

	}

	/**
	 * Removes the route from the table.
	 * 
	 * @param route The route.
	 */
	public void removeRoute(DiameterRoute route) {
		_routes.remove(route);
		for (ScoredRouteSet set : _scoreRoutes.values()) {
			set.removeRoute((Route) route);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeRoute: a route has been removed: " + route);
		}
	}

	/**
	 * Gets the key of the scored table.
	 * 
	 * @param realm The realm.
	 * @param appId The application identifier.
	 * @param type The type.
	 * @return The key value.
	 */
	private String getKey(String realm, long appId, int type) {
		StringBuilder res = new StringBuilder();
		res.append(realm).append(DELIMITER).append(appId).append(DELIMITER).append(type);
		return res.toString();
	}

	public void removePeer(RemotePeer sPeer) {
		List<Route> routesToRemove = new ArrayList<Route>();
		for (Route route : _routes) {
			if (route.getRoutingPeer() == sPeer) {
				routesToRemove.add(route);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removePeer: route to be removed=" + routesToRemove);
		}
		for (Route route : routesToRemove) {
			removeRoute(route);
		}

	}

}
