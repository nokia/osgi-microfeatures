// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel_lucent.as.agent.web.itest.servlets.AllMethodsServlet;
import com.alcatel_lucent.as.agent.web.itest.servlets.SimpleServlet;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class ServletMethodsTest extends IntegrationTestBase {
  @Test
  public void testGET() throws InterruptedException {
    component(comp -> comp.impl(new SimpleServlet()).provides(Servlet.class, "alias", "/foo"));
    
    String result = Utils.download("http://127.0.0.1:8080/foo", null);
    boolean ok = result.indexOf("Hello, world!") != -1;
    System.out.println("got response: " + result);
    assertTrue(ok);
    return;
  }
  
  @Test
  public void testPOSTandGET() throws InterruptedException {
    component(comp -> comp.impl(new AllMethodsServlet()).provides(Servlet.class, "alias", "/postAndGet"));
    
    Map<String, String> params = new HashMap<>();
    params.put("number", "42");
    String result = Utils.request("http://127.0.0.1:8080/postAndGet", params, "POST");
    
    assertEquals(result.trim(), "ok");
    
    result = Utils.download("http://127.0.0.1:8080/postAndGet", null);
    assertEquals(result.trim(), "42");
  }
  
  @Test
  public void testPUTandDELETEandGET() throws InterruptedException {
    component(comp -> comp.impl(new AllMethodsServlet()).provides(Servlet.class, "alias", "/putAndGet"));
    
    Map<String, String> params = new HashMap<>();
    params.put("number", "25000");
    String result = Utils.request("http://127.0.0.1:8080/putAndGet", params, "PUT");
    assertEquals(result.trim(), "ok");
    
    result = Utils.download("http://127.0.0.1:8080/putAndGet", null);
    assertEquals(result.trim(), "25000");
    
    result = Utils.request("http://127.0.0.1:8080/putAndGet", Collections.emptyMap(), "DELETE");
    
    result = Utils.download("http://127.0.0.1:8080/putAndGet", null);
    assertEquals(result.trim(), "-1");
  }
  
  @Test
  public void testHTTPClient() throws InterruptedException {
    component(comp -> comp.impl(new SimpleServlet()).provides(Servlet.class, "alias", "/foo2"));
    HttpClientImpl c = new HttpClientImpl();
    component(comp -> comp.impl(c).withSvc(org.eclipse.jetty.client.HttpClient.class, "(transport=mux)", true));

    assertTrue(c.awaitCompletion());
  }
  
  class HttpClientImpl {
    volatile HttpClient client;
    private CountDownLatch latch = new CountDownLatch(1);
    
    void start() {
      try {
        ContentResponse res = client.GET("http://localhost:8080/foo2");
        assertTrue(res.getStatus() == 200);
        System.out.println(res);
        latch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    boolean awaitCompletion() throws InterruptedException {
      return latch.await(5, TimeUnit.SECONDS);
    }
  }

  
}