// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.coordinator.impl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class similar to jdk Phaser, except that you can stop and report an exception instead of advancing.
 */
abstract class Phaser {
	private final AtomicInteger _parties = new AtomicInteger();
	private Throwable _error;
	
	abstract protected void done(Throwable err);
	
	void register() {
		_parties.incrementAndGet();
	}
	
	void arrive() {
		if (_parties.decrementAndGet() == 0) {
			done(_error); // will report last error seen.
		}
	}
	
	void arrive(Throwable error) {
		_error = error;
		arrive();
	}
}
