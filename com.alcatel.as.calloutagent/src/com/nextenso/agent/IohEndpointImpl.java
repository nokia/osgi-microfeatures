// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import java.util.concurrent.CompletableFuture;

import com.alcatel.as.service.ioh.IohEndpoint;
import com.nextenso.mux.MuxConnection;

import alcatel.tess.hometop.gateways.utils.Log;

public class IohEndpointImpl implements IohEndpoint {

	final MuxConnection _muxCnx;
	private Boolean _isRegistered; // null means the user has not yet called either register or unregister
	private boolean _active;
	public final static Log _log = Log.getLogger("callout.iohendpoint");

	public IohEndpointImpl(MuxConnection cnx) {
		_muxCnx = cnx;
		_log.info("IohEndPoint[%s]: created", cnx.getRemoteAddress());
	}

	public synchronized boolean activate(boolean registered) {
		_active = true;
		boolean accept;
		if (_isRegistered != null) {
			// user already called either register() or unregister. In this case, accept the
			// registered flag from the activate only if it matches the user choice
			accept =  _isRegistered.equals(Boolean.valueOf(registered));
		} else {
			// user has not yet called this.register() or this.unregister() methods:
			// return true to indicate that the cnx can be started or stopped.
			_isRegistered = new Boolean(registered);
			accept = true;
		}
		_log.info("IohEndPoint[%s]: activate: registered=%b, _isRegistered=%s, accept=%b", _muxCnx.getRemoteAddress(), registered, _isRegistered, accept);
		return accept;
	}

	@Override
	public synchronized void register() {
		_log.info("IohEndPoint[%s]: register", _muxCnx.getRemoteAddress());

		_isRegistered = new Boolean(true);
		if (_active) {
			_log.info("IohEndPoint[%s]: calling sendMuxStart", _muxCnx.getRemoteAddress());
			_muxCnx.sendMuxStart();
		}
	}

	@Override
	public synchronized CompletableFuture<Boolean> deregister() {
		_log.info("IohEndPoint[%s]: deregister", _muxCnx.getRemoteAddress());
		_isRegistered = new Boolean(false);
		if (_active) {
			_log.info("IohEndPoint[%s]: calling sendMuxStop", _muxCnx.getRemoteAddress());
			_muxCnx.sendMuxStop();
		}
		CompletableFuture<Boolean> cf = new CompletableFuture<>();
		cf.complete(true);
		return cf;
	}

	@Override
	public synchronized boolean isRegistered() {
		if (_isRegistered == null || _active == false) {
			return false;
		}
		return _isRegistered;
	}

}
