package com.nokia.casr.samples.diameter.springboot.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Before;
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
public class DiameterTest {

	private OsgiLauncher launcher;

	@Before
	public void setup() throws InterruptedException, ExecutionException, TimeoutException {
	}

	@Test
	public void test() throws InterruptedException {
	}

}
