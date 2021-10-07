package com.nextenso.diameter.agent.ha;

import com.nextenso.diameter.agent.impl.DiameterSessionFacade;

public interface SessionListener {

	public void handleSession(DiameterSessionFacade session);

}
