package com.alcatel.as.service.log.impl.asrlog;

import java.io.PrintStream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

@Component
public class StdoutRedirector {

	private Thread.UncaughtExceptionHandler _xh;
	private PrintStream _out = System.out;
	private PrintStream _err = System.err;
    private Thread _stdoutThread;
    private BundleContext _bctx;

	@Start
	void start() {
		if ("false".equals(System.getProperty("com.nokia.as.stdout2log4j", "true"))) {
			return;
		}
		if (System.getenv("CASR_LOGSTDOUT") != null) {
		    return;
		}
		
		Logger stdout = Logger.getLogger("stdout");
		Logger stderr = Logger.getLogger("stderr");
		_out = System.out;
		_err = System.err;
		System.setOut(new PrintStream(new Stdout2Log4jOutputStream(stdout, Level.WARN), true));
		System.setErr(new PrintStream(new Stdout2Log4jOutputStream(stderr, Level.ERROR), true));
		
        _stdoutThread = new Stdout2Log4jOutputStream.StdoutRedirectorThread();
        _stdoutThread.start();

		_xh = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(stderr));
	}
	
	@Stop
	void stop() {
		if ("false".equals(System.getProperty("com.nokia.as.stdout2log4j", "true"))) {
			return;
		}
		if (System.getenv("CASR_LOGSTDOUT") != null) {
		    return;
		}

		Thread.setDefaultUncaughtExceptionHandler(_xh);
		System.setOut(_out);
		System.setErr(_err);	
		if (_stdoutThread != null) {
			_stdoutThread.interrupt();
		}
	}
}
