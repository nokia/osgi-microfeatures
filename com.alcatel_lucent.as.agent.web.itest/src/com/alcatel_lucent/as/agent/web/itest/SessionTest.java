// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.itest;

import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.CookieManager;
import java.util.stream.IntStream;

import javax.servlet.Servlet;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel_lucent.as.agent.web.itest.servlets.SessionServlet;
import com.alcatel_lucent.as.agent.web.itest.servlets.TimeoutSessionServlet;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class SessionTest extends IntegrationTestBase {
	private final static Logger _log = Logger.getLogger(SessionTest.class);

  @Before
  public void init() {
	  Utils.initLoggers();
  }
	
  @Test
  public void testSession() throws InterruptedException, IOException {
    component(comp -> comp.impl(new SessionServlet()).provides(Servlet.class, "alias", "/session"));
    CookieManager cookieMgr = new CookieManager();
    IntStream.range(1, 3).forEach(index -> {
      String result = Utils.download("http://127.0.0.1:8080/session", cookieMgr);
      boolean ok = result.startsWith(String.valueOf(index));
      _log.warn("got response: " + result + ", index=" + index);
      assertTrue(ok);
    });
  }

  @Test
  public void testSessionTimeout() throws InterruptedException, IOException {
    component(comp -> comp.impl(new TimeoutSessionServlet()).provides(Servlet.class, "alias", "/session"));
    CookieManager cookieMgr = new CookieManager();
    
    IntStream.range(1, 3).forEach(index -> {
      String result = Utils.download("http://127.0.0.1:8080/session", cookieMgr);
      boolean ok = result.startsWith(String.valueOf(index));
      _log.warn("got response: " + result + ", index=" + index);
      assertTrue(ok);
    });
    
    Thread.sleep(2001);
    
    IntStream.range(1, 3).forEach(index -> {
      String result = Utils.download("http://127.0.0.1:8080/session", cookieMgr);
      boolean ok = result.startsWith("1");
      _log.warn("got response: " + result + ", index=" + index);
      assertTrue(ok);
      try {
        Thread.sleep(2001);
      } catch (InterruptedException e) {
        fail();
      }
    });
  }
  
  @Test
  public void testSessionTimeout_WARServlet() throws InterruptedException, IOException {
    CookieManager cookieMgr = new CookieManager();
    
    IntStream.range(1, 3).forEach(index -> {
      String result = Utils.download("http://127.0.0.1:8080/test/timeout", cookieMgr);
      boolean ok = result.startsWith(String.valueOf(index));
      _log.warn("got response: " + result + ", index=" + index);
      assertTrue(ok);
    });
    
    Thread.sleep(4050);
    
    String result = Utils.download("http://127.0.0.1:8080/test/timeout", cookieMgr);
    boolean ok = result.startsWith("1");
    _log.warn("got response: " + result + ", expect 1");
    assertTrue(ok);
  }



}
