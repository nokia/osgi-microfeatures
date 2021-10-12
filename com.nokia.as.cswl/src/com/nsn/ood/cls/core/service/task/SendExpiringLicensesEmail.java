// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.core.service.task;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.operation.EmailSendOperation;
import com.nsn.ood.cls.core.operation.EmailSendOperation.SendException;
import com.nsn.ood.cls.util.log.Loggable;

@Loggable
public class SendExpiringLicensesEmail implements EventHandler {
	private static final Logger LOG = LoggerFactory.getLogger(TaskEventsHandler.class);
	private EmailSendOperation emailSendOperation;

	@Override
	public void handleEvent(Event event) {
		try {
			sendEmail();
		} catch (Exception e) {
			LOG.error("Cannot send email with expiring licenses info.", e);
		}
	}
	
	private void sendEmail() throws SendException {
		this.emailSendOperation.sendExpiringLicensesEmail();
	}

}
