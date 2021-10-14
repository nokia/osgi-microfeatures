package com.nokia.as.diameter.tools.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Start;

@Component
public class Activator {
	
	private Configuration _cnf;
	
	@ConfigurationDependency
	void bindConfig(Configuration cnf) {
		_cnf = cnf;
	}
	
	@Start
	void start() {				
		String from = _cnf.getFrom();
		String secondary = _cnf.getSecondary();
		String secondarySchedule = _cnf.getSecondarySchedule();
		String to = _cnf.getTo();
		String port = _cnf.getPort();
		String peers = _cnf.getPeers();
		String tps = _cnf.getTps();
		String duration = _cnf.getDuration();
		String readTimeout = _cnf.getReadTimeout();
		String sctp = _cnf.getSctp();
		String bulkSize = _cnf.getBulkSize();
		
		List<String> opts = new ArrayList<>();
		if ("true".equals(sctp)) {
			opts.add("-sctp");
		}
		
		opts.add("-from");
		opts.add(from);
		opts.add("-to");
		opts.add(to);
		if (secondary != null) {
			opts.add("-sctpSecondary");
			opts.add(secondary);
			if (secondarySchedule != null) {
				opts.add("-sctpSecondarySchedule");
				opts.add(secondarySchedule);
			}
		}
		opts.add("-port");
		opts.add(port);
		
		opts.add("-peers");
		opts.add(peers);
		
		if (bulkSize != null) {
			opts.add("-bulksize");
			opts.add(bulkSize);
		}
		
		if (duration != null) {
			opts.add("-duration");
			opts.add(duration);
		}
		
		opts.add("-readTimeout");
		opts.add(readTimeout);
		
		if (tps != null && tps.length() >0) {
			opts.add("-tps");
			opts.add(tps);
		}
		
		try {
			System.out.println("args:" + opts);
			DiameterLoader.main(opts.toArray(new String[opts.size()]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
