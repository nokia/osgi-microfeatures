// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.engine;

import com.nextenso.diameter.agent.engine.DiameterProxyletEngine.Action;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.proxylet.engine.ProxyletEngineException;

public interface MessageProcessingListener {

	public void messageProcessed(DiameterMessageFacade message, Action action);

	public void messageProcessingError(DiameterMessageFacade message, ProxyletEngineException error);

}
