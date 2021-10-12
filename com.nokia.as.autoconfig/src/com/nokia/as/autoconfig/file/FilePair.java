// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.file;

import java.nio.file.Paths;

import com.nokia.as.autoconfig.Utils;

/**
 * A pair of filenames:
 * 	clothed: contains parents and extension
 *  naked: only filename
 */
public class FilePair {
	
	public final String naked;
	public final String clothed;
	
	public FilePair(String p) {
		clothed = p;
		naked = getPid(Paths.get(p).getFileName().toString());
	}
	
	private String getPid(String str) {
		String extension = Utils.extension(str, true);
		if(".cfg".equals(extension)) {
			return str.substring(0, str.lastIndexOf(extension));
		} else {
			return getPid(str.substring(0, str.lastIndexOf(extension)));
		}
	}
}
