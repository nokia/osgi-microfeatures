// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.impl;

import com.nextenso.proxylet.diameter.DiameterResponse;

public class CannotProxyException
		extends Exception {

	private static final long serialVersionUID = 1L;

	private final long _reason;
	private DiameterResponse.UNABLE_TO_DELIVER_CAUSE _cause;

	public CannotProxyException(long reason) {
		_reason = reason;
	}

	public CannotProxyException unableToDeliverCause (DiameterResponse.UNABLE_TO_DELIVER_CAUSE cause){ _cause = cause; return this;}
	public DiameterResponse.UNABLE_TO_DELIVER_CAUSE unableToDeliverCause (){ return _cause;}

	public final long getReason() {
		return _reason;
	}

}
