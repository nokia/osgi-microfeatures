package com.nokia.as.csdc2.stest.test;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.*;

import com.nokia.csdc.SdcClient;
import com.nokia.csdc.SdcResponse;
import com.nokia.csdc.SdcScope;

import com.google.common.base.Charsets;

/**
 * Component declared with Dependency Manager, and used to do a quick validatation for usage of Nokia/CSDC library under OSGI.
 * Since the start method may block the current thread, we schedule the initialization of this component in the ASR blocking threadpool.
 */
@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class CsdcTest {
	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService _log;

	@Before
	public void initLog() {
	    _log = logFactory.getLogger(CsdcTest.class);
	}

	@Test
	public void testCoreosAPI() throws Exception {
	    _log.warn("Testing CoreOS API");
	    ClassLoader old = Thread.currentThread().getContextClassLoader();
	    Thread.currentThread().setContextClassLoader(Client.class.getClassLoader());

	    // ------------------------------------------------------------------------------------------
	    // The following line must be used when using latest coreos api
	    Client client = Client.builder().endpoints("http://localhost:2379").build();

	    // The following line must be used when using older coreos api
	    // Client client = ClientBuilder.newBuilder().setEndpoints("http://localhost:2379").build();
	    // ------------------------------------------------------------------------------------------
			
	    Thread.currentThread().setContextClassLoader(old);
	    KV kvClient = client.getKVClient();

	    ByteSequence key = ByteSequence.from("mykey", Charsets.UTF_8);
	    ByteSequence value = ByteSequence.from("test_value", Charsets.UTF_8);

	    // put the key-value
	    _log.warn("putting key");
	    kvClient.put(key, value).get();
			
	    // get the CompletableFuture
	    CompletableFuture<GetResponse> getFuture = kvClient.get(key);
	    // get the value from CompletableFuture
	    _log.warn("getting key");
	    GetResponse response = getFuture.get();

	    // System.out.println(response.getKvsCount());
	    boolean valueFound = false;
	    List<KeyValue> l = response.getKvs();
	    for (int i = 0; l != null && i < l.size(); i ++) {
		String val = l.get(i).getValue().toString(Charsets.UTF_8);
		_log.warn("read value using coreos api: " + val);
		if ("test_value".equals(val)) {
		    valueFound = true;
		}
	    }
	    Assert.assertEquals(true, valueFound);
	    _log.warn("Tested CoreOS API");
	}

    @Test
    public void testCsdcApi() throws Exception {
	_log.warn("Testing CSDC API");

	// assume the config file is in ./instance/ directory
	SdcClient sdcClient = new SdcClient("instance/csdc.properties");
	_log.warn("putting Key");
	SdcResponse sdcResponse = sdcClient.setKeyValue(SdcScope.SDC_LOCAL, "sdc_test/ttl", "testKeyWithTTL", 10);
	if (null != sdcResponse.getError()) {
	    _log.warn("CSDC API: error:" + sdcResponse.getError().getMessage());
	    Assert.fail("CSDC API: error:" + sdcResponse.getError().getMessage());
	}
	        
	_log.warn("getting Key");
	sdcResponse = sdcClient.getKeyValue(SdcScope.SDC_LOCAL, "sdc_test/ttl");
	if (null != sdcResponse.getError()) {
	    _log.warn("CSDC API: error:" + sdcResponse.getError().getMessage());
	    Assert.fail("CSDC API: error:" + sdcResponse.getError().getMessage());
	}
	_log.warn(sdcResponse.getValue());
	Assert.assertEquals("testKeyWithTTL", sdcResponse.getValue());
	_log.warn("Tested CSDC API: have read testKeyWithTTL value from key sdc_test/ttl");
    }
    
}
