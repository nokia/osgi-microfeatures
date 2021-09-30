package com.alcatel_lucent.as.agent.web.itest.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketTestServlet extends WebSocketServlet {
private final static Logger _log = Logger.getLogger(WebSocketTestServlet.class);
  private static final long serialVersionUID = 1L;

  private final static byte[] PONG_MSG = new byte[] { (byte) 0x31, (byte) 0x32 };

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.register(MyWebSocket.class);
    factory.getPolicy().setIdleTimeout(15000);
  }
  
  @WebSocket
  public static class MyWebSocket {

    @OnWebSocketConnect
    public void onOpen(Session session) {
      _log.warn("Server opened session=" + session);
      // session.setIdleTimeout(100000);
    }

    @OnWebSocketFrame
    public void onFrame(Session session, Frame frame) {
      byte opCode = frame.getOpCode();
      byte[] data = null;
      if (frame.hasPayload()) {
        ByteBuffer buffer = frame.getPayload();
        int len = buffer.remaining();
        data = new byte[len];
        buffer.get(data, buffer.position(), buffer.remaining());
        _log.warn("Server MyWebSocket.onFrame: opcode=" + opCode);
      } else {
        _log.warn("Server MyWebSocket.onFrame: opcode=" + opCode);
      }

      switch (opCode) {
      case 1:
        _log.warn(Thread.currentThread().getName() + " - Server Echo data=" + new String(data));
        try {
          session.getRemote().sendString("Server reply " + new String(data));
        } catch (IOException e) {
          _log.warn("Server Error while echoing" + e);
        }
        break;

      case 9: // PING
        _log.warn(session.getClass().toString());
        _log.warn("Server Send PONG on " + session);
        //session.getRemote().sendPong(ByteBuffer.wrap(PONG_MSG));
        break;

      case 8: // CLOSE
        _log.warn("Server CLOSE on " + session);
        session.close(1000, "SERVER");
        break;

      default:
        break;
      }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
      _log.warn("Server MyWebSocket.onClose: code=" + statusCode + " ,reason=" + reason);
    }

  }
}
