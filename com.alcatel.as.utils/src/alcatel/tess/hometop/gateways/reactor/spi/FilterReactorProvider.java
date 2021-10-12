// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.spi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;

/**
 * This class is the superclass of all classes that filter a ReactorProvider. These classes sit on top of an already existing ReactorProvider 
 * which it uses as its basic usage, but possibly transforming some ReactorProvider methods, like tcpConnect/tcpAccept etc ...
 */
public class FilterReactorProvider extends ReactorProvider {

	protected ReactorProvider _provider;
	
	public FilterReactorProvider() {
	}

	public FilterReactorProvider(ReactorProvider provider) {
		_provider = provider;
	}
	
	public void setReactorProvider(ReactorProvider provider) {
		_provider = provider;
	}

	public Reactor getDefaultReactor() throws IOException {
		return _provider.getDefaultReactor();
	}

	public Reactor newReactor(String name, boolean start, Logger logger) throws IOException {
		return _provider.newReactor(name, start, logger);
	}

	public int hashCode() {
		return _provider.hashCode();
	}

	public Reactor newReactor(Logger tr) throws IOException {
		return _provider.newReactor(tr);
	}

	public Reactor newReactor(Logger tr, String threadName) throws IOException {
		return _provider.newReactor(tr, threadName);
	}

	public void newTcpClientChannel(InetSocketAddress to, TcpClientChannelListener listener, Reactor reactor,
			Object attachment, long timeout, Logger tr) {
		_provider.newTcpClientChannel(to, listener, reactor, attachment, timeout, tr);
	}

	public boolean equals(Object obj) {
		return _provider.equals(obj);
	}

	public void newTcpClientChannel(InetSocketAddress from, InetSocketAddress to, TcpClientChannelListener listener,
			Reactor reactor, Object attachment, long timeout, int priority, Logger tr, boolean secure) {
		_provider.newTcpClientChannel(from, to, listener, reactor, attachment, timeout, priority, tr, secure);
	}

	public void newTcpServerChannel(InetSocketAddress listenedAddr, TcpServerChannelListener listener, Reactor reactor,
			Object attachment, Logger tr, boolean secure) {
		_provider.newTcpServerChannel(listenedAddr, listener, reactor, attachment, tr, secure);
	}

	public String toString() {
		return _provider.toString();
	}

	public TcpServerChannel newTcpServerChannel(Reactor reactor, TcpServerChannelListener listener,
			InetSocketAddress listenedAddr, Object attachment, boolean secure, Logger tr) throws IOException {
		return _provider.newTcpServerChannel(reactor, listener, listenedAddr, attachment, secure, tr);
	}

	public void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor, Object attachment,
			Logger tr) {
		_provider.newUdpChannel(local, listener, reactor, attachment, tr);
	}

	public void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor, int priority,
			Object attachment, Logger tr) {
		_provider.newUdpChannel(local, listener, reactor, priority, attachment, tr);
	}

	public void newSctpClientChannel(SocketAddress local, InetAddress[] secondaryLocals, int maxOutStreams,
			int maxInStreams, SocketAddress to, Object attachment, long timeout, int priority, Logger logger,
			SctpClientChannelListener listener, Reactor reactor) {
		_provider.newSctpClientChannel(local, secondaryLocals, maxOutStreams, maxInStreams, to, attachment, timeout,
				priority, logger, listener, reactor);
	}

	public Reactor create(String name) {
		return _provider.create(name);
	}

	public Reactor getReactor(String name) {
		return _provider.getReactor(name);
	}

	public void aliasReactor(String name, Reactor reactor) {
		_provider.aliasReactor(name, reactor);
	}

	public Reactor getCurrentThreadReactor() {
		return _provider.getCurrentThreadReactor();
	}

	public SctpServerChannel newSctpServerChannel(SocketAddress local, InetAddress[] secondaryLocals, int maxOutStreams,
			int maxInStreams, int priority, Logger logger, SctpServerChannelListener listener, Object attachment,
			Reactor reactor) throws IOException {
		return _provider.newSctpServerChannel(local, secondaryLocals, maxOutStreams, maxInStreams, priority, logger,
				listener, attachment, reactor);
	}

	public void tcpConnect(Reactor reactor, InetSocketAddress to, TcpClientChannelListener listener,
			Map<?, Object> opts) {
		_provider.tcpConnect(reactor, to, listener, opts);
	}

	public TcpServerChannel tcpAccept(Reactor reactor, InetSocketAddress listenAddr, TcpServerChannelListener listener,
			Map<TcpServerOption, Object> opts) throws IOException {
		return _provider.tcpAccept(reactor, listenAddr, listener, opts);
	}

	public UdpChannel udpBind(Reactor reactor, InetSocketAddress local, UdpChannelListener listener,
			Map<UdpOption, Object> opts) throws IOException {
		return _provider.udpBind(reactor, local, listener, opts);
	}

	public void sctpConnect(Reactor reactor, SocketAddress to, SctpClientChannelListener listener,
			Map<SctpClientOption, Object> options) {
		_provider.sctpConnect(reactor, to, listener, options);
	}

	public SctpServerChannel sctpAccept(Reactor reactor, SocketAddress local, SctpServerChannelListener listener,
			Map<SctpServerOption, Object> options) throws IOException {
		return _provider.sctpAccept(reactor, local, listener, options);
	}

}
