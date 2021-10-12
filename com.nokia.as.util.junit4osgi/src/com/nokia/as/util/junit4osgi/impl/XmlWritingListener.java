// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.junit4osgi.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Class copied from https://code.google.com/archive/p/reporting-junit-runner/source, and slightly modified.
 */
public class XmlWritingListener extends RunListener {
	private final File reportDirectory;
	private String fileName;
	private Document document;
	private Element root;
	private String name;
	int nError = 0;
	int nFailure = 0;
	private Element currentFailureNode;
	DecimalFormat TIME_FORMAT = new DecimalFormat("######0.000");
	private long t1;
	private volatile int _failed;
	private volatile int _finished;

	ByteArrayOutputStream err = new ByteArrayOutputStream();
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	PrintStream originalErr;
	PrintStream originalOut;

	public XmlWritingListener(File reportDirectory) {
		this.reportDirectory = reportDirectory;
	}

	private String formatTime(long time) {
		return TIME_FORMAT.format(((double) time) / 1000);
	}

	@Override
	public void testRunStarted(Description description) throws Exception {
		originalErr = System.err;
		originalOut = System.out;
		System.setErr(new PrintStream(err));
		System.setOut(new PrintStream(out));

	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		root.addAttribute("tests", "" + result.getRunCount());
		root.addAttribute("name", name);
		root.addAttribute("failures", "" + nFailure);
		root.addAttribute("errors", "" + nError);
		root.addAttribute("time", formatTime(result.getRunTime()));
		root.addAttribute("hostname", getHostName());
		root.addAttribute("timestamp", getIsoTimestamp());
		root.addElement("system-out").addCDATA(out.toString());
		root.addElement("system-err").addCDATA(err.toString());

		System.setErr(originalErr);
		System.setOut(originalOut);

	}

	public static String getIsoTimestamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		final String timestamp = dateFormat.format(new Date());
		return timestamp;
	}

	private String getHostName() {
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
			return address.getHostName().toLowerCase();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void testStarted(Description description) throws Exception {
		this.t1 = System.currentTimeMillis();

	}

	@Override
	public void testFinished(Description description) throws Exception {
		long time = System.currentTimeMillis() - t1;

		String method = description.toString().split("\\(")[0];

		Element currentTestcase = root.addElement("testcase");
		currentTestcase.addAttribute("time", formatTime(time));
		currentTestcase.addAttribute("classname", this.name);
		currentTestcase.addAttribute("name", method);
		if (currentFailureNode != null) {
			currentTestcase.add(currentFailureNode);
			currentFailureNode = null;
		}
		_finished ++;
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		_failed ++;
		if (failure.getException() instanceof java.lang.AssertionError) {
			failure(failure);
		} else {
			error(failure);
		}
	}
	
	public int getFailures() {
		return _failed;
	}
	
	public int getFinished() {
		return _finished;
	}

	private void error(Failure failure) {
		nError++;
		final String elementName = "error";
		currentFailureNode = createFailure(elementName, failure);
	}

	private Element createFailure(String elementName, Failure failure) {
		final Element element = DocumentHelper.createElement(elementName);
		element.addAttribute("message", failure.getMessage());
		element.addAttribute("type", failure.getException().getClass().getName());
		element.addText(failure.getTrace());
		return element;

	}

	private void failure(Failure failure) {
		nFailure++;
		currentFailureNode = createFailure("failure", failure);

	}

	@Override
	public void testIgnored(Description description) throws Exception {
		System.out.println("Ignored");
	}

	public void startFile(Class<?> aClass) {

		document = DocumentHelper.createDocument();
		root = document.addElement("testsuite");

		name = aClass.getName();
		this.fileName = "TEST-" + name + ".xml";
	}

	public void closeFile() {
		try {

			// lets write to a file
			OutputFormat outformat = OutputFormat.createPrettyPrint();
			outformat.setTrimText(false);

			XMLWriter writer = new XMLWriter(new FileWriter(new File(reportDirectory, fileName)), outformat);

			writer.write(document);
			writer.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
