// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;

public class TestJndiConfig {
  public static void main(String args[]) throws Exception {
    InitialContext ctxt = new InitialContext();
    System.out.println(ctxt.lookup("application.foo"));
    ctxt.bind("application.zoo", new Integer(10));
    System.out.println("application.zoo=" + ctxt.lookup("application.zoo"));
    System.out.println("application.zoo class=" + ctxt.lookup("application.zoo").getClass());
    
    EventContext eventCtx = (EventContext) ctxt.lookup("");
    eventCtx.addNamingListener("application.foo", EventContext.OBJECT_SCOPE, new ObjectChangeListener() {
      public void namingExceptionThrown(NamingExceptionEvent evt) {
        evt.getException().printStackTrace();
      }
      
      public void objectChanged(NamingEvent evt) {
        Binding newBinding = evt.getNewBinding();
        System.out.println("objectChanged:" + newBinding.getName() + "=" + newBinding.getObject());
      }
    });
    
    //c.setPrivateProperty("application.foo", "bar2");
    //c.notifyListeners();
    
    System.out.println("list ...");
    NamingEnumeration<NameClassPair> ec = ctxt.list(args[0]);
    while (ec.hasMore()) {
      NameClassPair ncp = ec.next();
      System.out.println("\t " + ncp.getName() + "=" + ncp.getClassName());
    }
    
    System.out.println("listBinding ...");
    NamingEnumeration<Binding> eb = ctxt.listBindings(args[0]);
    while (eb.hasMore()) {
      Binding bd = eb.next();
      System.out.println("\t " + bd.getName() + "=" + bd.getObject());
    }
    
    eventCtx.close();
  }
}
