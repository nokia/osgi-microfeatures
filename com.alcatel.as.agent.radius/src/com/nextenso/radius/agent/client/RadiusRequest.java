// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.client;

import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

import com.nextenso.radius.agent.impl.RadiusMessageFacade;

public abstract class RadiusRequest {

	private static AtomicLong L_SEED = new AtomicLong(1L);

	private boolean _isValid = false;
	private long _identifier;
	private int _identifierRadius;

	protected RadiusRequest() {
		setIdentifiers();
	}

	protected boolean isValid() {
		return _isValid;
	}

	protected void setValid(boolean b) {
		_isValid = b;
	}

	private void setIdentifiers() {
		_identifier = L_SEED.getAndIncrement();
		_identifierRadius = (int) (_identifier % 256);
	}

	protected long getIdentifier() {
		return _identifier;
	}

	protected int getRadiusIdentifier() {
		return _identifierRadius;
	}

	protected abstract RadiusMessageFacade getRequest();

	protected abstract Enumeration getResponseAttributes();

	protected abstract int getResponseCode();

	protected abstract void handleRadiusResponse(byte[] buff, int off, int len);

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("valid=").append(isValid());
		res.append(", id=").append(getIdentifier());
		res.append(", radius id=").append(getRadiusIdentifier());
		res.append(", request=").append(getRequest());
		return res.toString();
	}

}
