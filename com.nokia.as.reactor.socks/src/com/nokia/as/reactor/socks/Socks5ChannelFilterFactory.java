// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.reactor.socks;

import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.alcatel.as.service.concurrent.TimerService;

import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.spi.ChannelListenerFactory;

@Component
@Property(name="type", value="socks5")
public class Socks5ChannelFilterFactory implements ChannelListenerFactory {
	
	@ServiceDependency(filter="(strict=false)")
	TimerService _wheelTimer;

	@Override
	public TcpClientChannelListener createListener(TcpClientChannelListener listener, Map<?, Object> options) throws Exception {			
		return new Socks5TcpClientListener(listener, options, _wheelTimer);
	}

	@Override
	public TcpServerChannelListener createListener(TcpServerChannelListener listener, Map<?, Object> options) throws Exception {
		return listener;
	}

}
