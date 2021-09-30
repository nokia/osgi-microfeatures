package com.nokia.as.service.loghistory;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is the contract of a simple LogBuffer service. It only keep
 * the N last lines of logs and provide them through this service.
 * Log levels and desired loggers are still configured in the log4j properties 
 */
@ProviderType
public interface LogBufferService {
	
	/**
	 * Gives the last logs (maximum: bufferSize)
	 * @param numberOfEntries: number of entries to display, if numberOfEntries > bufferSize,
	 * 							then bufferSize is used
	 * @return Log entries in a StringBuilder
	 */
	StringBuilder getLogs();
	
	/**
	 * Gives the last numberOfEntries logs and filter each logEntries with filter (do a non case sensitive logEntry.contains(filter)).
	 * If the filter is not required, set filter to null or to an empty string
	 * @param numberOfEntries: number of entries to display, if numberOfEntries > bufferSize,
	 * 							then bufferSize is used
	 * @param filter: filter on each logEntry if it contains the appropriate string
	 * @return Log entries in a StringBuilder
	 */
	StringBuilder getLogs(int numberOfEntries, String filter);
	
	/**
	 * Gives the maximum amount of log entries the service can display.
	 * That buffer size is configurable (service property)
	 * @return bufferSize: maximum number of log entries
	 */
	int getBufferSize();
}
