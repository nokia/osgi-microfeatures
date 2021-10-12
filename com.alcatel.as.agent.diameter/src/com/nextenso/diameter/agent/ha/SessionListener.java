// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.ha;

import com.nextenso.diameter.agent.impl.DiameterSessionFacade;

public interface SessionListener {

	public void handleSession(DiameterSessionFacade session);

}
