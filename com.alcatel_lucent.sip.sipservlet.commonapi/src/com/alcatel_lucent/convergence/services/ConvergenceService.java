// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.convergence.services;

import java.util.Enumeration;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.sip.SipApplicationSession;

public interface ConvergenceService {

	// Http Session management
	public interface SessionManagement {
		public void addHttpSession(SipApplicationSession sas, HttpSession session);
		public void removeHttpSession(SipApplicationSession sas, String id);
		public void removeAllHttpSessions(SipApplicationSession sas);
		public byte getSessionCounter(SipApplicationSession sas);
	}

	// ServletContext event propagation API
	public interface EventPropagation {
		public void propagateServletCtxEvent(ServletContextEvent event, String method);
		public void propagateServletCtxAttributeEvent(ServletContextAttributeEvent event, String method);
	}

	public interface AttributesViewer {
		public String getInternalInitParameter(String name);
		@SuppressWarnings("rawtypes")
		public Enumeration getInternalInitParameterNames();
		public Object getInternalAttribute(String name);
		@SuppressWarnings("rawtypes")
		public  Enumeration getInternalAttributeNames();
	}

	public interface SipApplicationSessionCreator {
		public SipApplicationSession createSipApplicationSessionById(String id);
	}
}