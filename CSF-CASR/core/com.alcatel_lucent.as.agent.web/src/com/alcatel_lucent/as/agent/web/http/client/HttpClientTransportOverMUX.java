package com.alcatel_lucent.as.agent.web.http.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.ConnectionPool.Factory;
import org.eclipse.jetty.client.DuplexConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.client.http.HttpDestinationOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

import com.alcatel_lucent.as.agent.web.muxhandler.Agent;
import com.nextenso.mux.MuxHandler;

@Component(provides={HttpClientTransport.class}, properties={ @Property(name="protocol", value="mux")})
public class HttpClientTransportOverMUX extends ContainerLifeCycle implements HttpClientTransport {

  final static Logger LOGGER = Logger.getLogger("agent.web.http.client");

  private volatile HttpClient client;
  private volatile Agent agent;
  private Factory factory;
  
  public HttpClientTransportOverMUX() {
    super();
    setConnectionPoolFactory(destination -> new DuplexConnectionPool(destination, 8, destination)); //TODO change this?

  }
  
  @ServiceDependency(filter="(protocol=web)")
  protected void bindMuxHandler(MuxHandler handler) { 
    agent = (Agent) handler;
  }

  @Override
  public void setHttpClient(HttpClient client) {
    this.client = client;
  }

  @Override
  public HttpDestination newHttpDestination(Origin origin) {
    return new HttpDestinationOverHTTP(client, origin);
  }

  @Override
  public void connect(InetSocketAddress addr, Map<String, Object> context) {
    final HttpDestination destination = (HttpDestination)context.get(HTTP_DESTINATION_CONTEXT_KEY);    
    @SuppressWarnings("unchecked")
    final Promise<Connection> promise = (Promise<Connection>)context.get(HTTP_CONNECTION_PROMISE_CONTEXT_KEY);
    final boolean secure = destination.getScheme().equalsIgnoreCase("https");        
    
    agent.connect(addr, secure, new Promise<HttpClientSocket>() {

      @Override
      public void succeeded(HttpClientSocket httpClientSocket) {
        EndPoint endPoint = httpClientSocket.createEndPoint(addr, secure);
        HttpConnectionOverHTTP connection = new HttpConnectionOverHTTP(endPoint, destination, promise);
        endPoint.setConnection(connection);
        connection.onOpen();
      }

      @Override
      public void failed(Throwable t) {
        promise.failed(new Throwable(t));        
      }
      
    });
    
  }

  @Override
  public org.eclipse.jetty.io.Connection newConnection(EndPoint endPoint, Map<String, Object> context) throws IOException {
    throw new IOException("Not implemented");
  }

@Override
public Factory getConnectionPoolFactory() {
  return factory;
}

@Override
public void setConnectionPoolFactory(Factory fac) {
  this.factory = fac;
}

}
