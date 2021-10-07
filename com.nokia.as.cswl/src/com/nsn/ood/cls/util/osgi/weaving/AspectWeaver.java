package com.nsn.ood.cls.util.osgi.weaving;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

public class AspectWeaver implements WeavingHook {

	protected ConcurrentMap<ClassLoader, ClassLoaderWeavingAdaptor> adaptorMap = new ConcurrentHashMap<>();

	protected ClassLoaderWeavingAdaptor getAdaptor(BundleWiring wiring) {
		ClassLoader wiringCL = wiring.getClassLoader();
		//System.out.println("getAdaptor: wiring=" + wiring + ", wiringCL=" + wiringCL);
		//System.out.println("adaptorMap " + adaptorMap);
		return adaptorMap.computeIfAbsent(wiringCL, loader -> {
			//System.out.println("computeIfAbsent: loader=" + loader);
			ClassLoaderWeavingAdaptor adaptor = new ClassLoaderWeavingAdaptor();
			AspectContext context = new AspectContext(wiring);
			adaptor.initialize(loader, context);
			return adaptor;
		});
	}

	@Override
	public void weave(WovenClass woven) {
		try {
			String name = woven.getClassName();
			//System.out.println("weave class " + name);
			if(name.contains("com.nsn.ood.cls") && !name.contains("osgi.weaving")) {
				//System.out.println("in com.nsn.ood.cls, going to wire " + name);
				BundleWiring wiring = woven.getBundleWiring();
				ClassLoaderWeavingAdaptor adaptor = getAdaptor(wiring);
				final byte[] source = woven.getBytes();
				final byte[] target;
				// aspectj is single-threaded
				synchronized (adaptor) {
					target = adaptor.weaveClass(name, source);
				}
				woven.setBytes(target);
			}
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

}
