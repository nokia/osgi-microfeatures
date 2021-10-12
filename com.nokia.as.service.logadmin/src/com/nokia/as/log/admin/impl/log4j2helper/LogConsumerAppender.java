// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.log.admin.impl.log4j2helper;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import com.nokia.as.log.service.admin.LogHandler;

public class LogConsumerAppender extends AbstractAppender {
	
	private final LogHandler _logHandler;
	private final boolean _format;

	public LogConsumerAppender(LogHandler logHandler, String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions, boolean format) {
        super(name, filter, layout, ignoreExceptions);
        _logHandler = logHandler;
        _format = format;
	}
	
	@Override
	public void append(LogEvent event) {
		try {
			if (_format) {
				_logHandler.handleLog(() -> getLayout().toSerializable(event).toString());
			} else {
				_logHandler.handleLog(() -> format(event));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String format(LogEvent event) {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		pw.println(event.getMessage().getFormattedMessage());
		Throwable t = event.getThrown();
		if (t != null) {
			t.printStackTrace(pw);
		}
		return (buffer.toString());
	}

}
