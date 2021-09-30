package com.nokia.casr.sample.diameter.springboot.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.nokia.as.osgi.launcher.OsgiLauncher;

/**
 * Tests if a remote diameter server can be connecter
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DiameterLoaderTest {

	@Autowired
	private OsgiLauncher _launcher;

	@Test
	public void test() throws InterruptedException, ExecutionException, TimeoutException {
		System.out.println("test: launcher=" + _launcher);
		DiameterLoader loader = new DiameterLoader(_launcher);
		loader.start();
		
		for (int i = 0; i < 10; i ++) {
			Thread.sleep(1000);
			if (loader.getMessageReceived() > 0) {
				System.out.println("Received " + loader.getMessageReceived() + " responses");
				return;
			}			
		}
		
		Assert.fail("DiameterLoader failure");
	}

}
