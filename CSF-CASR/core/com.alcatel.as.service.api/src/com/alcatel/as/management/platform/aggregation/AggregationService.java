package com.alcatel.as.management.platform.aggregation;

import java.io.Serializable;
import java.util.Map;

/**
 * This interface represents an aggregator service
 * on which listeners can register to get notified
 */
public interface AggregationService {
	/**
	 * Constant used by services to specify their implementation in activator
	 */
	public static final String TYPE = "type";

	/**
	 * Add listener
	 * @param listener The listener to add
	 * @return The listener id
	 */
	public int addListener(AggregationService.Listener listener);
	
	/**
	 * Remove a listener
	 * @param id The listener id
	 */
	public void removeListener(int id);
	
	/**
	 * This interface represents a listener for events
	 */
	public interface Listener{
		public void notify(Map<String,Serializable> datas);
	}
}
