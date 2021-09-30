package com.nokia.casr.samples.radius.springboot.server;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.radius.auth.AccessRequest;
import com.nextenso.proxylet.radius.auth.AccessRequestProxylet;
import com.nextenso.proxylet.radius.auth.AccessResponse;
import com.nextenso.proxylet.radius.auth.AccessResponseProxylet;
import com.nextenso.proxylet.radius.auth.AuthUtils;
import com.nokia.as.osgi.launcher.OsgiLauncher;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.apache.log4j.Logger;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This proxylet handles Auth Requests. 
 */
@Component
public class TestAccessRequestProxylet implements AccessRequestProxylet {
	
        Logger log = Logger.getLogger(TestAccessRequestProxylet.class);

	/**
	 * This service is the bridge between springboot world and CASR osgi world.
	 * Using this service, you can then obtain CASR services, or register your
	 * springboot classes as osgi services.
	 */
	@Autowired
	private OsgiLauncher launcher;

	/**
	 * Register our Diameter Proxylet into the CASR OSGi container
	 */
	@PostConstruct
	public void start() {
	        log.warn("Registering radius proxylet into CASR radius container via OSGi bridge");
		ServiceRegistration<?> reg = launcher.registerService(AccessRequestProxylet.class, this);
	}

	public void init(ProxyletConfig conf) {
		log.warn("Initializing TestAccessRequestProxylet proxylet");
	}

	public String getProxyletInfo() {
		return "My TestAccessRequestProxylet";
	}

	public void destroy() {
	}

	/***************** Auth requests *************/

	public int accept(AccessRequest request) {
		// do i want to handle this request ? lets say yes
		// this is more relevant when many AccessRequestProxylets are chained
		return AccessRequestProxylet.ACCEPT;
	}

	public int doRequest(AccessRequest request) {
		// i can read all the attributes in the request

		AccessResponse response = request.getResponse();
		response.setCode(AuthUtils.CODE_ACCESS_ACCEPT);
		// can set attributes in the response

		// return the response (do not proxy the request further)
		return AccessResponseProxylet.RESPOND_FIRST_PROXYLET;
	}

}
