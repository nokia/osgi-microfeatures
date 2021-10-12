// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Vector;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.ConfigListener;

public class TestConfig implements ConfigListener {
  
  String name;
  
  private static void p(String s) {
    System.out.println(s);
  }
  
  public static void main(String args[]) throws Exception {
    Config cnf = new Config();
    cnf.setProperty("bar.gabuzo3", "bar.value3 (default)");
    cnf.writeTo(new FileOutputStream("/tmp/foo.properties"), "bar.");
    
    cnf.clearChangeHistory();
    Vector table = new Vector();
    table.add("1");
    table.add("2");
    cnf.setPublicTable("foo.table", table);
    System.out.println("foo.table=" + cnf.getTable("foo.table"));
    System.out.println("foo.table=" + cnf.getObject("foo.table"));
    cnf.setPublicProperty("yaya", "yuyu");
    
    System.out.println("properties starting with foo.*:\n");
    Enumeration e = cnf.getKeys("foo.*");
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (name.indexOf("table") != -1) {
        System.out.println(name + "=" + cnf.getTable(name));
      } else {
        System.out.println(name + "=" + cnf.getString(name));
      }
    }
    System.out.println();
    
    TestConfig test1 = new TestConfig("test1", "foo.*", cnf);
    TestConfig test2 = new TestConfig("test2", "bar.*", cnf);
    TestConfig test3 = new TestConfig("test3", "foo.gabuzo1", cnf);
    TestConfig test4 = new TestConfig("test4", "*", cnf);
    
    // make some tests
    
    System.out.println("--");
    p("NOTIFY1");
    cnf.notifyListeners();
    cnf.setPublicProperty("foo.p1", "foo.p1(pub)");
    p("NOTIFY2");
    cnf.notifyListeners();
    cnf.setPrivateProperty("foo.p1", "foo.p1(priv)");
    p("NOTIFY3");
    cnf.notifyListeners();
    cnf.removePrivateProperty("foo.p1");
    p("NOTIFY4");
    cnf.notifyListeners();
    cnf.removePublicProperty("foo.p1");
    p("NOTIFY5");
    cnf.notifyListeners();
    
    System.out.println("--");
    cnf.setProperty("foo.gabuzo1", "foo.value1 (modified)");
    cnf.setProperty("foo.gabuzo2", "foo.value2 (modified)");
    cnf.setProperty("bar.gabuzo3", "bar.value3 (modified)");
    cnf.notifyListeners();
    
    System.out.println("--");
    cnf.setProperty("foo.gabuzo1", "foo.value1 (modified 2)");
    cnf.notifyListeners();
    
    System.out.println("--");
    cnf.removeProperty("bar.gabuzo3");
    cnf.notifyListeners();
    
    System.out.println("--");
    cnf.unregisterListener(test3, "foo.gabuzo1");
    cnf.setProperty("foo.gabuzo1", "foo.value1 (modified 3)");
    cnf.notifyListeners();
  }
  
  public TestConfig(String name, String pattern, Config cnf) throws ConfigException {
    this.name = name;
    cnf.registerListener(this, pattern);
  }
  
  public String toString() {
    return name;
  }
  
  public void propertyChanged(Config cnf, String propertyNames[]) throws ConfigException {
    try {
      for (int i = 0; i < propertyNames.length; i++) {
        System.out.println(name + ": " + propertyNames[i] + "=" + cnf.getObject(propertyNames[i], null));
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
