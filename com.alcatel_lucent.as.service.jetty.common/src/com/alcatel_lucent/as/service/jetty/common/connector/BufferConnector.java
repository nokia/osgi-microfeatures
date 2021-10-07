package com.alcatel_lucent.as.service.jetty.common.connector;

import java.io.IOException;

import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;

public class BufferConnector extends AbstractConnector {
	private EndPointManager manager;

	public BufferConnector(EndPointManager manager, Server server, ConnectionFactory... factory) {
		super(server, null, null, null, 0, factory);
		this.manager = manager;
		AbstractBufferEndPoint.connector = this;
	}

	@Override
	public Object getTransport() {
		return this;
	}

	@Override
	protected void accept(int acceptorID) throws IOException, InterruptedException {
	}

	public void addEndPoint(EndPoint endPoint) {
		Connection connection = getDefaultConnectionFactory().newConnection(this, endPoint);
		endPoint.setConnection(connection);
		// take care: the following method registers the endpoint in a list, so if you call this method, 
		// you should remove the endpoint from the connection  at some point ...
//		endPoint.onOpen(); 
//		onEndPointOpened(endPoint);
		connection.onOpen();
	}

	protected EndPointManager getEndPointManager() {
		return manager;
	}

}
