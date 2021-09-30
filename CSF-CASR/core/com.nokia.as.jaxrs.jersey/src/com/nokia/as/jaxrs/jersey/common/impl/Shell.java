package com.nokia.as.jaxrs.jersey.common.impl;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.glassfish.jersey.server.model.Resource;

import com.nokia.as.jaxrs.jersey.common.JaxRsResourceRegistry;

/**
 * Debug JAX-RS Server using Gogo shell commands
 */

@Component(provides = Shell.class)
@Property(name = CommandProcessor.COMMAND_SCOPE, value = "casr.app.jaxrs")
@Property(name = "casr." + CommandProcessor.COMMAND_SCOPE + ".alias", value = "asr.jaxrs")
@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "classes", "singletons", "resources", "props" })
@Descriptor("JAX-RS Server")
public class Shell {

	@ServiceDependency
	volatile JaxRsResourceRegistry _registration;

	@Descriptor("List loaded classes")
	public void classes() {
		_registration	.getLoadedClasses()
						.forEach((BiConsumer<? super InetSocketAddress, ? super Set<Class<?>>>) print());
	}

	@Descriptor("List loaded classes by server port")
	public void classes(@Descriptor("port") String port) {
		_registration	.getLoadedClasses(Integer.valueOf(port))
						.forEach((BiConsumer<? super InetSocketAddress, ? super Set<Class<?>>>) print());
	}

	@Descriptor("List loaded singletons")
	public void singletons() {
		_registration.getLoadedSingletons().forEach(print());
	}

	@Descriptor("List loaded singletons by server port")
	public void singletons(@Descriptor("port") String port) {
		_registration.getLoadedSingletons(Integer.valueOf(port)).forEach(print());
	}

	@Descriptor("List loaded resources")
	public void resources() {
		_registration	.getLoadedResources()
						.forEach((BiConsumer<? super InetSocketAddress, ? super Set<Resource>>) print());
	}

	@Descriptor("List loaded resources by server port")
	public void resources(@Descriptor("port") String port) {
		_registration	.getLoadedResources(Integer.valueOf(port))
						.forEach((BiConsumer<? super InetSocketAddress, ? super Set<Resource>>) print());
	}

	@Descriptor("List loaded properties")
	public void props() {
		_registration.getLoadedProperties().forEach(printProps());
	}

	@Descriptor("List loaded properties by server port")
	public void props(@Descriptor("port") String port) {
		_registration.getLoadedProperties(Integer.valueOf(port)).forEach(printProps());
	}

	private BiConsumer<? super InetSocketAddress, ? super Set<Object>> print() {
		return (a, r) -> {
			System.out.println(a);
			r.forEach(x -> System.out.println("  " + x));
		};
	}

	private BiConsumer<? super InetSocketAddress, ? super Map<String, Object>> printProps() {
		return (a, r) -> {
			System.out.println(a);
			r.forEach((k, v) -> System.out.println("  " + k + "=" + v));
		};
	}
}
