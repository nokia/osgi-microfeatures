package com.nextenso.mux.impl;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

public class MuxFactoryImplStandalone extends MuxFactoryImpl {
	public MuxFactoryImplStandalone() {
		bindReactorProvider(ReactorProvider.provider());
		bindTimerService(ServiceLoader.getService(TimerService.class, "(strict=true)"));
		bindPlatformExecutors(ServiceLoader.getService(PlatformExecutors.class));
	}
}
