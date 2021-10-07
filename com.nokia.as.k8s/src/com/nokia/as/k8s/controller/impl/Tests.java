package com.nokia.as.k8s.controller.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;

import com.nokia.as.k8s.controller.CasrResource;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;

//@Component
public class Tests {
	private static Logger LOG = Logger.getLogger(Tests.class);

	@ServiceDependency
	ResourceService rs;
	
	@ServiceDependency(removed = "unsetResource", required = false)
	public void setResource(CustomResource res, Dictionary props) {
		LOG.warn("!!! received resource " + res);
	}
	
	public void unsetResource(CustomResource res, Dictionary props) {
		LOG.warn("!!! unset resources " + res);
	}
	
	@Start
	public void start() {
		/*
		LOG.warn("STARTING TESTS");
		WatchHandle handle = rs.watch(CasrResource.CRD, ResourceService.ACCEPT_ALL);
		
		CasrResource resource2 = new CasrResource("test-zob", 
				Arrays.asList("my.feature"), 
				1,
				Arrays.asList(new CasrResource.Port("http", 8080, "TCP", false, "test")));
		
		resource2.namespace("hadrien2");
		
		
		rs.create(resource2)
			.thenCompose((ok) -> {
				LOG.warn("----- resource created ? " + ok);
				try {
					Thread.sleep(2_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return rs.getAll(CasrResource.CRD);
			})
			.thenCompose((resList) -> {
				LOG.warn("----- resources " + resList);
				try {
					Thread.sleep(2_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return rs.get(CasrResource.CRD, "test-zob");
			})
			.thenCompose((res) -> {
				LOG.warn("------ got resoure " + res);
				try {
					Thread.sleep(2_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return rs.delete(CasrResource.CRD, "test-zob");
			})
			.thenCompose((ok) -> {
				LOG.warn("------- resource deleted ? " + ok);
				try {
					Thread.sleep(2_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return rs.getAll(CasrResource.CRD);
			})
			.handle((res, ex) -> {
				LOG.warn("------- got resource " + res);
				LOG.warn("ex ", ex);
				
				try {
					Thread.sleep(2_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				LOG.warn("stopping watch");
				try {
					handle.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return null;
			});
		*/
		
	}
}
