// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns.impl.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.IPAddr;

import com.alcatel_lucent.as.service.dns.Record;
import com.alcatel_lucent.as.service.dns.RecordA;
import com.alcatel_lucent.as.service.dns.RecordAAAA;
import com.alcatel_lucent.as.service.dns.RecordDClass;
import com.alcatel_lucent.as.service.dns.impl.RecordCache;

/**
 * The parser for /etc/hosts file
 */
public class HostParser {

	private final static Logger LOGGER = Logger.getLogger("dns.impl.parser.hosts");
	private static final char COMMENT_CHAR = '#';

	public static void parse(RecordCache cache, String filename) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("parse: filename=" + filename);
		}

		File file = new File(filename);
		if (!file.exists()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("parse: " + filename + " does not exist");
			}
			return;
		}

		if (!file.isFile()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("parse: " + filename + " is not a file");
			}
			return;
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				int iSharp = line.indexOf(COMMENT_CHAR);
				if (iSharp >= 0) {
					line = line.substring(0, iSharp);
				}
				line = line.trim();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("parse: processing line=" + line);
				}

				String[] elts = line.split("\\s+");
				if (elts.length > 1) {
					IPAddr ip = IPAddr.getIPAddress(elts[0]);
					if (ip == null) {
						continue;
					}
					addRecords(ip.isIPv4(), elts, cache);
				}
			}
		}
		catch (IOException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("parse: cannot read the buffered reader");
			}
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("parse: cannot close the buffered reader");
					}
				}
			}
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("parse: res=" + cache);
		}

	}

	/**
	 * Builds and adds records in the cache.
	 * 
	 * @param isV4 true if the address is an IPV4 address.
	 * @param elts The parsed elements.
	 * @param res The cache to fill.
	 */
	private static void addRecords(boolean isV4, String[] elts, RecordCache res) {
		Record record = null;
		long ttl = -1;
		String ip = elts[0];
		for (int i = 1; i < elts.length; i++) {
			String name = elts[i];
			if (isV4) {
				record = new RecordA(name, RecordDClass.IN, ttl, ip);
			} else {
				record = new RecordAAAA(name, RecordDClass.IN, ttl, ip);
			}

//			if (LOGGER.isDebugEnabled()) {
//				LOGGER.debug("parse: add record=" + record);
//			}

			List<Record> records = new ArrayList<Record>();

			records.add(record);
			res.put(records, record.getType(), name, ttl);
		}

	}

}
