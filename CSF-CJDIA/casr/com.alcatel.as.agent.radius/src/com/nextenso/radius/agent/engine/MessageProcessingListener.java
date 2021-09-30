package com.nextenso.radius.agent.engine;


import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.radius.agent.engine.RadiusProxyletEngine.Action;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;

public interface MessageProcessingListener {

	public void messageProcessed(RadiusMessageFacade message, Action action);

	public void messageProcessingError(RadiusMessageFacade message, ProxyletEngineException error);

}
