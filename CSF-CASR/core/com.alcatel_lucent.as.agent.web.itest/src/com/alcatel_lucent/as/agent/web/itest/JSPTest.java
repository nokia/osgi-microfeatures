package com.alcatel_lucent.as.agent.web.itest;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class JSPTest extends IntegrationTestBase {

  @Test
  public void testJSPQuery() throws InterruptedException, IOException {
    String result = Utils.download("http://localhost:8080/test", null);
    Assert.assertTrue(result.contains("Hello JSP World"));
  }
}
