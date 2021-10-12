// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;

/**
 * Open a tcp socket to the TestServer program (port 9999), and send him
 * "Hello".
 */
public class TestSctpClientSecure implements SctpClientChannelListener {
	static Logger _logger = Logger.getLogger("client");
	static volatile Reactor _reactor;

	public static void main(String args[]) throws Exception {
		System.out.println("HELLO, I AM SCTP CLIENT SECURE");
		TestSctpClientSecure client = new TestSctpClientSecure();
		ReactorProvider factory = ReactorProvider.provider();

		_reactor = factory.create("reactorClient");
		_reactor.start();
		
		String NEEDED_CIPHERS[] = {
	    		".*",
	    		"SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
	    		"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"
	    };
	    
	    Security security = new Security()
	    		.addProtocol("DTLSv1.2")
	    		.addCipher(NEEDED_CIPHERS)
	    		.keyStore(new FileInputStream("tls/client.ks"))
	    		.keyStorePassword("password")
			.build();
		
		SocketAddress local = new InetSocketAddress(args[0], 5000);
		//InetAddress[] secondaryLocals = new InetAddress[] { InetAddress.getByName(args[1]) };
		SocketAddress remote = new InetSocketAddress(args[2], 5500);

		Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
		opts.put(SctpClientOption.SO_RCVBUF, new Integer(10000));
		opts.put(SctpClientOption.SO_SNDBUF, new Integer(20000));
		opts.put(SctpClientOption.SECURITY, security);
		opts.put(SctpClientOption.LOCAL_ADDR, local);
		//opts.put(SctpClientOption.SECONDARY_LOCAL_ADDRS, secondaryLocals);    

		factory.sctpConnect(_reactor, remote, client, opts);
		Thread.sleep(Integer.MAX_VALUE);
	}

	@Override
	public void connectionEstablished(final SctpChannel cnx) {
		try {
			_logger.warn("connection established: remotePort=" + cnx.getRemotePort() + ", locals="
					+ cnx.getLocalAddresses() + ", remotes=" + cnx.getRemoteAddresses());
			_logger.warn("new association=" + cnx.getAssociation());

			_reactor.scheduleAtFixedRate(new Runnable() {
				public void run() {
					//System.out.println("sending ...");
					cnx.send(false, null, 0, ByteBuffer.wrap("hello".getBytes()));
				}
			}, 0L, 1000L, TimeUnit.MILLISECONDS);      
		} catch (IOException e) {
			_logger.warn(e);
		}
	}

	@Override
	public void connectionFailed(SctpChannel cnx, Throwable error) {
		_logger.warn("connection failed");
	}

	@Override
	public void messageReceived(SctpChannel cnx, ByteBuffer msg, SocketAddress addr, int bytes,
			boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber) {
		_logger.warn("message received: remaining=" + msg.remaining());
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
		_logger.warn("sendFailed: addr=" + addr + ", buf=" + buf + ", errocde=" + errcode + ", stream="
				+ streamNumber);
	}

	public void shutdown(SctpChannel cnx) {
		_logger.warn("shutdown");
		//cnx.close();
	}

	@Override
	public void receiveTimeout(SctpChannel cnx) {
	}

	@Override
	public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
		_logger.warn("Peer address change event: addr="  + addr + ", event=" + event);
	}
}
