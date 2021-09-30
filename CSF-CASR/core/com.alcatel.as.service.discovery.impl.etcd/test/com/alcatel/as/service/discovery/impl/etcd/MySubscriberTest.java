package com.alcatel.as.service.discovery.impl.etcd;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.discovery.Advertisement;

@Component
public class MySubscriberTest {

	@Inject
	BundleContext _bc;

	@Start
	void start() {
		System.out.println("__Subscriber starting...");
		
	}
	
	@ServiceDependency(required=false, removed="advertRemoved", filter="(provider=etcd)")
	void advertAdded(Advertisement advert, Dictionary<String, Object> serviceProperties){
		System.out.println("__Subscriber : advert Added ...");
	}
	
	void advertRemoved(Advertisement advert, Dictionary<String, Object> serviceProperties){
		System.out.println("__Subscriber : advert Removed ...");
	}
}
