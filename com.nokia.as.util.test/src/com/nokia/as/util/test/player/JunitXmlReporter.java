// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.test.player;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Class copied from
 * https://code.google.com/archive/p/reporting-junit-runner/source, and slightly
 * modified.
 */
public class JunitXmlReporter {
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
	private int runCount;
	private long runTime;
	private final ByteArrayOutputStream _stdoutBytes = new ByteArrayOutputStream();
	private final ByteArrayOutputStream _stderrBytes = new ByteArrayOutputStream();
	private PrintStream originalErr;
	private PrintStream originalOut;
	private final PrintStream _stdoutPrintStream;
	private final PrintStream _stderrPrintStream;

	public JunitXmlReporter() {
		String reportDir = Optional.ofNullable(System.getenv("AS_JUNIT4OSGI_REPORTSDIR"))
				.orElse("system-tests/test-reports/cdlb");
		String testName = System.getProperty("testName", "test");
				
		this.reportDirectory = new File(reportDir);
		this.reportDirectory.mkdirs();
		this.fileName = System.getProperty("report.file", "TEST-CDLB.xml");
		this.name = testName;
		document = DocumentHelper.createDocument();
		root = document.addElement("testsuite");
		originalErr = System.err;
		originalOut = System.out;
		_stdoutPrintStream = new PrintStream(new TeeOutputStream(_stdoutBytes, System.out), true);
		_stderrPrintStream = new PrintStream(new TeeOutputStream(_stderrBytes, System.out), true);
		System.setErr(_stdoutPrintStream);
		System.setOut(_stderrPrintStream);
		runTime = System.currentTimeMillis();
	}

	public void testStarted(String testDescription) throws Exception {
		runCount++;
		this.t1 = System.currentTimeMillis();
	}

	public void testFinished(String testDescription) throws Exception {
		long time = System.currentTimeMillis() - t1;
		String method = testDescription.split("\\(")[0];
		Element currentTestcase = root.addElement("testcase");
		currentTestcase.addAttribute("time", formatTime(time));
		currentTestcase.addAttribute("classname", this.name);
		currentTestcase.addAttribute("name", method);
		if (currentFailureNode != null) {
			currentTestcase.add(currentFailureNode);
			currentFailureNode = null;
		}
		_stdoutPrintStream.flush();
		_stderrPrintStream.flush();
		currentTestcase.addElement("system-out").addCDATA(_stderrBytes.toString());
		currentTestcase.addElement("system-err").addCDATA(_stdoutBytes.toString());
		_stderrBytes.reset();
		_stdoutBytes.reset();
	}

	public void testFailed(Throwable failure) throws Exception {
		if (failure instanceof java.lang.AssertionError) {
			failure(failure);
		} else {
			error(failure);
		}
	}

	public void close() throws Exception {
		try {
			runTime = System.currentTimeMillis() - runCount;
			root.addAttribute("tests", "" + runCount);
			root.addAttribute("name", name);
			root.addAttribute("failures", "" + nFailure);
			root.addAttribute("errors", "" + nError);
			root.addAttribute("time", formatTime(runTime));
			root.addAttribute("hostname", "localhost" /*getHostName()*/);
			root.addAttribute("timestamp", getIsoTimestamp());
			
			_stderrPrintStream.flush();
			_stdoutPrintStream.flush();
			root.addElement("system-out").addCDATA(_stderrBytes.toString());
			root.addElement("system-err").addCDATA(_stdoutBytes.toString());
			System.setErr(originalErr);
			System.setOut(originalOut);

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

	// -------------------------- private methods

	private String formatTime(long time) {
		return TIME_FORMAT.format(((double) time) / 1000);
	}

	private static String getIsoTimestamp() {
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

	private void failure(Throwable failure) {
		nFailure++;
		currentFailureNode = createFailure("failure", failure);
	}

	private void error(Throwable failure) {
		nError++;
		currentFailureNode = createFailure("error", failure);
	}

	private Element createFailure(String elementName, Throwable failure) {
		final Element element = DocumentHelper.createElement(elementName);
		element.addAttribute("message", failure.getMessage());
		element.addAttribute("type", failure.getClass().getName());
		element.addText(getTrace(failure));
		return element;
	}

	public String getTrace(Throwable failure) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		failure.printStackTrace(writer);
		return stringWriter.toString();
	}
}
