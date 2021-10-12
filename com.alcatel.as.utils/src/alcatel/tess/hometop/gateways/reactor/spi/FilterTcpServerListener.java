// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.spi;

import java.util.Map;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

/**
 * This class is the superclass of all classes that filter Tcp server Listeners. These classes sit on top of an already existing Tcp Client listener 
 * which it uses as its basic sink of data, but possibly transforming the data along the way or providing additional functionality
 */
public class FilterTcpServerListener extends FilterTcpListener implements TcpServerChannelListener {
	
	private final TcpServerChannelListener _listener;
	private TcpServerChannel _serverChannel;

	public FilterTcpServerListener(TcpServerChannelListener listener, Map<?, Object> options) {
		super(listener, options);
		_listener = listener;
	}

	@Override
	public void serverConnectionOpened(TcpServerChannel server) {
		_serverChannel = server;
		_listener.serverConnectionOpened(server);
	}

	@Override
	public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
		_serverChannel = server;
		_listener.serverConnectionFailed(server, err);
	}

	@Override
	public void serverConnectionClosed(TcpServerChannel server) {
		_listener.serverConnectionClosed(server);
	}

	@Override
	public void connectionAccepted(TcpServerChannel serverChannel, TcpChannel acceptedChannel) {
		setChannel(acceptedChannel);
		_listener.connectionAccepted(serverChannel, acceptedChannel);
	}

	@Override
	public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
		_listener.connectionFailed(serverChannel, err);
	}

}
