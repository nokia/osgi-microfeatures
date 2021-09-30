package com.nokia.as.log.admin.impl.log4j1;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.nokia.as.log.service.admin.LogHandler;

public class LogConsumerAppender extends AppenderSkeleton {

	private final LogHandler _logHandler;
	private final boolean _format;

	LogConsumerAppender(Layout layout, LogHandler logHandler, boolean format) {
		super.layout = layout;
		super.name = "logconsumer";
		_logHandler = logHandler;
		_format = format;
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	public void close() {
	}

	@Override
	protected void append(LoggingEvent event) {
		try {
			if (_format) {
				_logHandler.handleLog(() -> getLayout().format(event));
			} else {
				_logHandler.handleLog(() -> format(event));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private String format(LoggingEvent event) {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		pw.println(event.getMessage());
		ThrowableInformation info = event.getThrowableInformation();
		if (info != null) {
			info.getThrowable().printStackTrace(pw);
		}
		return (buffer.toString());
	}

}
