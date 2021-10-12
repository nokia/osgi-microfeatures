// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl;

import com.nextenso.mux.MuxFactory;

public class SimpleMuxFactoryImplStandalone extends SimpleMuxFactoryImpl {
	public SimpleMuxFactoryImplStandalone() {
		_muxFactory = MuxFactory.getInstance();
	}
}
