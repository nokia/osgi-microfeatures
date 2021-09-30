package com.nokia.as.util.junit4osgi.test;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides=Object.class)
@Property(name=OsgiJunitRunner.JUNIT, value="true")
@RunWith(OsgiJunitRunner.class)
public class MyTest {  
  @Before
  public void before() {
  }
  
  @Test
  public void testMe() throws Exception {
    System.out.println("Test ME !");
    if (true) throw new Exception("FAILED");
  }
  
  @After
  public void after() {
  }
}
