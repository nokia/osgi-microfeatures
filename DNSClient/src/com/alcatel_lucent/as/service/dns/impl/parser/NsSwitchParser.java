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
import java.util.List;

import org.apache.log4j.Logger;

import com.alcatel_lucent.as.service.dns.impl.DNSProperties.NsSwitchOption;


public class NsSwitchParser {

	private final static Logger LOGGER = Logger.getLogger("dns.impl.parser.nsswitch");
	private static final char COMMENT_CHAR = '#';

	public static void parse(String filename, List<NsSwitchOption> options) {
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
				int ix = line.indexOf("hosts:");
				if (ix >= 0) {
					String[] elts = line.split("\\s+");
					for (int i = 1; i < elts.length; i++) {
						String option = elts[i];
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("parse: processing option=" + option);
						}
						if ("files".equalsIgnoreCase(option)) {
							options.add(NsSwitchOption.FILES);
						} else if ("dns".equalsIgnoreCase(option)) {
							options.add(NsSwitchOption.DNS);
						} else {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("parse: not processed (ignored) option=" + option);
							}
						}
					}
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
			for (NsSwitchOption option : options) {
				LOGGER.debug("parse:res=" + option);
			}
		}

	}
}
