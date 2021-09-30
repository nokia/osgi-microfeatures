package com.alcatel_lucent.as.agent.web.itest;

import static org.junit.Assert.assertEquals;

import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketWebAppTest extends IntegrationTestBase {
    
  private final static byte[] PING_MSG = new byte[] { (byte) 0x99, (byte) 0x01 };
  private final static Logger _log = Logger.getLogger(WebSocketWebAppTest.class);
  
  @Test
  public void testWebSocket() throws Exception {
	  //Set up console loggers
	  Utils.initLoggers();

	WebSocketClient client = null;
    Thread.sleep(5_000L);
    try {
      client = new WebSocketClient();
      client.start();
      WebSocketClientHandler socket = new WebSocketClientHandler();
      client.connect(socket, new URI("ws://localhost:8080/test/websocket"));
      Assert.assertTrue(socket.awaitClose(20,TimeUnit.SECONDS));
    } finally {
      if(client != null) {
        client.stop();
      }
    }
  }
  
  
  @WebSocket(maxTextMessageSize = 64 * 1024)
  public class WebSocketClientHandler {
    private final CountDownLatch closeLatch;
    @SuppressWarnings("unused")
    private Session session;

    public WebSocketClientHandler() {
      this.closeLatch = new CountDownLatch(4);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
      _log.warn("Count = " + closeLatch.getCount());
      return closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      _log.warn(String.format("Client closed: %d - %s%n", statusCode, reason));
      this.session = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
      _log.warn(String.format("Client connected: %s%n", session));
      this.closeLatch.countDown();
      this.session = session;
      try {
        Future<Void> fut, fut2;
        fut = session.getRemote().sendStringByFuture("Hello");
        
        fut.get(2, TimeUnit.SECONDS); // wait for send to complete.
        fut = session.getRemote().sendStringByFuture("Thanks for the conversation.");
        fut.get(2, TimeUnit.SECONDS); // wait for send to complete.
        
        session.getRemote().sendPing(ByteBuffer.wrap(PING_MSG));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    @OnWebSocketFrame
    public void onFrame(Frame frame) {
      _log.warn("Client received frame opCode=" + frame.getOpCode());
      String message = StandardCharsets.UTF_8.decode(frame.getPayload()).toString();

      switch(frame.getOpCode()) {
      case 1:
        _log.warn("Client got message: " + message);
        if(closeLatch.getCount() == 3) {
          assertEquals(message, "Server reply Hello");
        } else {
          assertEquals(message, "Server reply Thanks for the conversation.");
        }
        
        this.closeLatch.countDown();
        break;
      case 8:
        _log.warn("Client closed: " + message);
        break;
      case 10:
        _log.warn("Client got pong!");
        this.closeLatch.countDown();
        break;
      }
    }
  }

  
}
