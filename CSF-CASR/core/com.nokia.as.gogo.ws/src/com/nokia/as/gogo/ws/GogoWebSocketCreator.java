package com.nokia.as.gogo.ws;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.features.admin.k8s.Deployer;
import com.nokia.as.features.admin.k8s.Pod;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

public class GogoWebSocketCreator implements WebSocketCreator {
	private static final Logger LOG = Logger.getLogger("gogo.ws");

	private ReactorProvider reactors;
	private PlatformExecutors executors;
	private Deployer deployer;
	private Reactor reactor;

	public GogoWebSocketCreator(PlatformExecutors execs, ReactorProvider reactors, Deployer deployer) {
		this.reactors = Objects.requireNonNull(reactors);
		this.executors = Objects.requireNonNull(execs);
		this.deployer = Objects.requireNonNull(deployer);

		reactor = reactors.create("gogo.ws");
		reactor.start();
	}

	@Override
	public Object createWebSocket(ServletUpgradeRequest arg0, ServletUpgradeResponse arg1) {
	  String queryString = arg0.getQueryString();
	  LOG.info("Request argument " + queryString);
		if(queryString == null || queryString.isEmpty()) {
		  sendError(arg1, 404, "No runtime pod name provided");
		  return null;
		}
		Optional<Pod> maybePod = findPod(queryString);
		if(maybePod.isPresent()) {
  		try {
  		  Pod p = maybePod.get();
  		  LOG.debug("pod ip " + p.getIp());
  		  LOG.debug("pod name " + p.name);
  		  
  		  if(p.getIp() == null) {
  		    sendError(arg1, 500, " The pod has no IP yet! Please retry later.");
  		  }
  		  
  			return createWebSocket(p.getIp(), p.port);
  		} catch (IOException e) {
  		  sendError(arg1, 500, "An error occcured when "
  		      + "creating the websocket : " + e.getMessage());
  		  return null;
  		}
		}
		return null;
	}

	protected GogoWebSocket createWebSocket(String ip, int port) throws IOException {
		PlatformExecutor exec = executors.createQueueExecutor(executors.getIOThreadPoolExecutor());
		GogoWebSocket ws = new GogoWebSocket(reactors, exec, reactor, ip, port);
		return ws;
	}
	
	protected Optional<Pod> findPod(String name) {
	  return deployer
	      .deployedRuntimes()
	      .stream()
	      .map(runtime -> runtime.pod(name))
	      .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
	      .findFirst();
	}
	
	private void sendError(ServletUpgradeResponse arg1, int code,  String msg) {
    try {
      arg1.sendError(code, msg);
    } catch (IOException e1) {
      LOG.warn("failed to send error message ", e1);
    }
	}
}
