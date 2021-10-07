package com.nokia.as.gogo.ws;

import javax.servlet.Servlet;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.features.admin.k8s.Deployer;

import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

@Component(provides=Servlet.class)
@Property(name = "alias", value = "/gogo")
public class GogoWebSocketServlet extends WebSocketServlet {
	private static final Logger LOG = Logger.getLogger(GogoWebSocketServlet.class);
	
	@ServiceDependency
	private Deployer deployer;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@ServiceDependency
	private ReactorProvider reactors;
	
	@ServiceDependency
	private PlatformExecutors executors;

	@Start
	void start() {
		LOG.warn("GOGO WEB SOCKET START!");
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.setCreator(new GogoWebSocketCreator(executors, reactors, deployer));
		factory.getPolicy().setIdleTimeout(60000);
	}


}
