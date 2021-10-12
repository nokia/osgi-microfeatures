// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.util.sctp.SctpSocketOption;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;

/**
 * Open a tcp socket to the TestServer program (port 9999), and send him
 * "Hello".
 */
public class TestSctpClient implements SctpClientChannelListener {
	static Logger _logger = Logger.getLogger("client");
	static volatile Reactor _reactor;

	public static void main(String args[]) throws IOException, InterruptedException {
		TestSctpClient client = new TestSctpClient();
		ReactorProvider factory = ReactorProvider.provider();

		_reactor = factory.create("Client");
		_reactor.start();
		SocketAddress local = new InetSocketAddress(args[0], 9990);
		SocketAddress remote = new InetSocketAddress(args[1], 9991);
		Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
		opts.put(SctpClientOption.LOCAL_ADDR, local);

		if (args.length > 2) {
			InetAddress[] secondaryLocals = new InetAddress[] { InetAddress.getByName(args[2]) };
			opts.put(SctpClientOption.SECONDARY_LOCAL_ADDRS, secondaryLocals);
		}

		factory.sctpConnect(_reactor, remote, client, opts);
		Thread.sleep(Integer.MAX_VALUE);
	}

	@Override
	public void connectionEstablished(final SctpChannel cnx) {
		try {
			_logger.warn("connection established: remotePort=" + cnx.getRemotePort() + ", locals="
					+ cnx.getLocalAddresses() + ", remotes=" + cnx.getRemoteAddresses());
			_logger.warn("new association=" + cnx.getAssociation());

			_reactor.scheduleAtFixedRate(() -> {
				System.out.println("sending ...");
				cnx.send(false, null, 0, ByteBuffer.wrap("hello".getBytes()));
			}, 30, 30, TimeUnit.SECONDS);
			
		} catch (IOException e) {
			_logger.warn(e);
		}
	}

	@Override
	public void connectionFailed(SctpChannel cnx, Throwable error) {
		_logger.warn("connection failed");
	}

	@Override
	public void messageReceived(SctpChannel cnx, ByteBuffer msg, SocketAddress addr, int bytes, boolean isComplete,
			boolean isUnordered, int ploadPID, int streamNumber) {
		_logger.warn("message received: remaining=" + msg.remaining() + ", addr=" + addr);
		byte[] b = new byte[msg.remaining()];
		msg.get(b);
		_logger.warn(new String(b));
	}

	@Override
	public void writeBlocked(SctpChannel cnx) {
		_logger.warn("write blocked");
	}

	@Override
	public void writeUnblocked(SctpChannel cnx) {
		_logger.warn("write unblocked");
	}

	@Override
	public void connectionClosed(SctpChannel cnx, Throwable err) {
		_logger.warn("connection Closed", err);
	}

	public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) {
		_logger.warn("sendFailed: addr=" + addr + ", buf=" + buf + ", errocde=" + errcode + ", stream=" + streamNumber);
	}

	public void shutdown(SctpChannel cnx) {
		_logger.warn("shutdown");
		// cnx.close();
	}

	@Override
	public void receiveTimeout(SctpChannel cnx) {
	}

	@Override
	public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
		_logger.warn("Peer address change event: addr=" + addr + ", event=" + event);
	}
}
