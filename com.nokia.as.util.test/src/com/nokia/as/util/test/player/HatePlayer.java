// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.test.player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class HatePlayer extends TestPlayer {

	protected Map<String, String> _tests = new HashMap<String, String>();
	JunitXmlReporter _junitReporter = new JunitXmlReporter();

	public HatePlayer() throws Exception {
		set("hate.prefix", "ZNORUN ");
	}

	public Boolean declare(String value) throws Exception {
		if (_tests.size() == 0) {
			set("on-ok", get("hate.ok >> $on-ok"));
			set("on-ko", get("hate.ko >> $on-ko"));
		}
		String[] toks = split(value, 2);
		toks[1] = unquote(toks[1]);
		_tests.put(toks[0], toks[1]);
		System.out.println(get("$hate.prefix") + toks[1]);
		return true;
	}

	public Boolean enter(String value) throws Exception {
		String test = get("$hate.test");
		if (test.length() != 0) {
			ok(null);
		}
		String[] toks = split(value, 2);
		if (toks.length == 2) {
			declare(value);
		}
		set("hate.test", toks[0]);
		_junitReporter.testStarted(_tests.get(get("$hate.test")));
		return true;
	}

	public Boolean ok(String value) throws Exception {
		if (get("$hate.test").length() == 0)
			return true;
		String testDesc = _tests.get(get("$hate.test"));
		System.out.println("+++++++++++++++ HATE_OK " + testDesc);
		set("hate.test", null);
		_junitReporter.testFinished(testDesc);

		return true;
	}

	public Boolean ko(String value) throws Exception {
		if (get("$hate.test").length() == 0)
			return true;
		String testDesc = _tests.get(get("$hate.test"));
		System.out.println("--------------- HATE_KO " + testDesc);
		set("hate.test", null);
		_junitReporter.testFailed(new java.lang.AssertionError("test failed"));
		_junitReporter.testFinished(testDesc);
		return true;
	}
	
	public void close() throws Exception {
		_junitReporter.close();
		super.close();
	}
	
}