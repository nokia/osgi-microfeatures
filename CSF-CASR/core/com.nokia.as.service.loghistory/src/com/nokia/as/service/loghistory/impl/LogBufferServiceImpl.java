package com.nokia.as.service.loghistory.impl;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;
import com.nokia.as.log.service.admin.LogHandler;
import com.nokia.as.log.service.admin.LogMessage;

import com.nokia.as.service.loghistory.LogBufferService;
import com.nokia.as.service.loghistory.common.Configuration;
import com.nokia.as.service.loghistory.common.LogUtils;
import com.nokia.as.service.loghistory.common.RingBuffer;


@Component
public class LogBufferServiceImpl implements LogBufferService, LogHandler{

	private final static Logger log = Logger.getLogger(LogBufferServiceImpl.class);

	private RingBuffer<String> buffer;
	private Configuration configuration;
	private int maxChars = 10000;
	private int bufferSize = 100;
	boolean stopped = false;

	@ConfigurationDependency()
	void bindConfiguration(Configuration conf) {
		configuration = conf;
	}
	
	@Start
	void start() {
		log.debug("LogBufferService STARTED");
		stopped = false;
		if (configuration != null) {
			maxChars = configuration.getMaxLineSize();
			bufferSize = configuration.getBufferSize();
			log.debug(String.format("Configuration has been loaded as follows: "
					+ "buffer.size=%d, max.line.size=%d", bufferSize, maxChars));
		} else {
			log.warn("Configuration has not been loaded");
		}

		buffer = new RingBuffer<>(bufferSize);
	}


	@Override
	public void handleLog(LogMessage message) {
		String msg = message.getMessage();
		buffer.put(msg);
	}
	
	@Stop
	void stop() {
		stopped = true;
		log.debug("LogBufferService STOPPING... cleaning buffer...");
		buffer.clear();
		buffer = null;
	}
		
	@Override
	public StringBuilder getLogs() {
		if (stopped) return new StringBuilder("");
		StringBuilder logs = new StringBuilder();
		buffer.stream().map(e -> {
					String fullMessage = e;
					String more = fullMessage.length() > maxChars? "[...]": "";
					String entry = fullMessage.substring(0, Math.min(fullMessage.length(), maxChars));
					return (entry+more);
	            }).collect(Collectors.<String> toList()).iterator().forEachRemaining(logs::append);
		return logs;
	}

	@Override
	public StringBuilder getLogs(int numberOfEntries, String filter) {
		if (stopped) return new StringBuilder("");
		StringBuilder logs = new StringBuilder();
		Stream<String> bufferList = buffer.stream().map(e -> {
					String fullMessage = e;
					String more = fullMessage.length() > maxChars? "[...]": "";
					String entry = fullMessage.substring(0, Math.min(fullMessage.length(), maxChars));
					return (entry+more);
	            });
		
		ifFilter(bufferList, filter)
				  .collect(LogUtils.lastN(numberOfEntries))
				  .forEach(logs::append);
		return logs;
	}

	
	Stream<String> ifFilter(Stream<String> stream, String filter){
		if(filter == null || filter.isEmpty())
			return stream;
		else
			return stream.filter(s -> s.toLowerCase().contains(filter.toLowerCase()));
	}
	
	@Override
	public int getBufferSize() {
		return bufferSize;
	}

}
