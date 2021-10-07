package com.nextenso.mux.impl;

import com.nextenso.mux.MuxFactory;

public class SimpleMuxFactoryImplStandalone extends SimpleMuxFactoryImpl {
	public SimpleMuxFactoryImplStandalone() {
		_muxFactory = MuxFactory.getInstance();
	}
}
