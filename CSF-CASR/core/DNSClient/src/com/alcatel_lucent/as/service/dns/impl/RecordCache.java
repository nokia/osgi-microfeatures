package com.alcatel_lucent.as.service.dns.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.alcatel_lucent.as.service.dns.Record;
import com.alcatel_lucent.as.service.dns.RecordType;

/**
 * A container to cache the records returned by the DNS.
 */
public class RecordCache {

	private final static Logger LOGGER = Logger.getLogger("dns.impl.cache");

	private Map<String, Element>
		_mapA = new ConcurrentHashMap<>(),
		_mapAAAA = new ConcurrentHashMap<>(),
		_mapCName = new ConcurrentHashMap<>(),
		_mapNAPTR = new ConcurrentHashMap<>(),
		_mapSRV = new ConcurrentHashMap<>();

	private static class Element {

		// _deathDate < 0 --> infinite
		// _deathDate = 0 --> no cache
		// _deathDate > 0 --> expire
		
		private List<Record> _records = null;
		private long _deathDate = -1L;

		public Element(List<Record> records, long ttl) {
			_records = records;
			if (ttl <= 0) {
				_deathDate = ttl;
			} else {
				_deathDate = System.currentTimeMillis() + ttl * 1000;
			}
		}

		/**
		 * Gets the records.
		 * 
		 * @return The records.
		 */
		public final List<Record> getRecords() {
			return _records;
		}

		/**
		 * Gets the ttl.
		 * 
		 * @return The ttl.
		 */
		public final long getDeathDate() {
			return _deathDate;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder res = new StringBuilder();
			res.append("Death date=").append(getDeathDate());
			res.append(", records=").append(getRecords().toString());
			return res.toString();
		}
	}
    
	private Meters _meters;
	public RecordCache(){};
	public void setMeters (Meters meters) { _meters = meters;}

	private Map<String, Element> getMap (RecordType type){
		switch (type){
		case A : return _mapA;
		case AAAA: return _mapAAAA;
		case CNAME: return _mapCName;
		case NAPTR: return _mapNAPTR;
		case SRV : return _mapSRV;
		}
		throw new RuntimeException ("Cannot happen");
	}

	/**
	 * 
	 * @param records
	 * @param type
	 * @param query
	 * @param ttl
	 */
	public void put(List<Record> records, RecordType type, String query, long ttl) {
		if (ttl == 0) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("put: TTL=0 -> no cache");
			}
			return;
		}

		if (records == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("put: no records -> do nothing");
			}
			return;
		}

		if (type == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("put: no type -> do nothing");
			}
			return;
		}

		if (query == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("put: no query -> do nothing");
			}
			return;
		}

		Element elt = new Element(records, ttl);
		Map<String, Element> map = getMap (type);
		map.put(query, elt);
		// we cannot increment the cache size since it may be accessed in // threads -> need absolute size - also the put may be a duplicate
		if (_meters != null) _meters.getSet (type)._cacheEntriesMeter.set (map.size ()); // meters is null for hosts cache

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("put: query=" + query + ", type=" + type + ", elt=" + elt);
		}

	}

	/**
	 * Gets records according to
	 * 
	 * @param type
	 * @param query
	 * @return The list of records.
	 */
	public List<Record> get(RecordType type, String query) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("get: query=" + query + ", type=" + type);
		}

		if (query == null || type == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("get: null argument -> return null");
			}
			return null;
		}

		Map<String, Element> mapOfRecords = getMap(type);
		
		Element res = mapOfRecords.get(query);
		if (res == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("get: no records -> return null");
			}
			return null;
		}

		if (res.getDeathDate() >= 0 && res.getDeathDate() <= System.currentTimeMillis()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("get: found a dead record (TTL is in the past) -> remove it from the cache");
			}
			mapOfRecords.remove (query);
			// we cannot decrement the cache size since it may be accessed in // threads -> need absolute size
			if (_meters != null) _meters.getSet (type)._cacheEntriesMeter.set (mapOfRecords.size ()); // meters is null for hosts cache
			return null;

		}
		return res.getRecords();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("[RecordCache ");		
		for (Map<String, Element> map: new Map[]{_mapA, _mapAAAA, _mapCName, _mapNAPTR, _mapSRV}){
			for (Entry<String, Element> entry : map.entrySet ()) {
				res.append ("\n\t").append(entry.getKey()).append(" -> ").append(entry.getValue());				
			}
		}
		res.append("\n]");
		return res.toString();
	}

}
