// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gogo.ws;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.jline.utils.Log;

import com.alcatel.as.service.concurrent.PlatformExecutor;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.utils.Charset;

@WebSocket
public class GogoWebSocket implements TcpClientChannelListener {
  private static final Logger LOG = Logger.getLogger("gogo.ws");

  private class WebSocketOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      if (session != null) {
        try {
          session.getRemote().sendString(Character.valueOf((char) b).toString());
        } catch (WebSocketException e) {
          Log.warn("Got WebSocketException: " + e.getMessage());
          session.close();
          session = null;
        }
      }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      String str = Charset.CHARSET_US_ASCII.getString(b, off, len);
      if (session != null) {
        try {
          session.getRemote().sendString(str);
        } catch (WebSocketException e) {
          Log.warn("Got WebSocketException: " + e.getMessage());
          if (session != null) {
            session.close();
            session = null;
          }
        }
      }
    }

  }

  private final ReactorProvider reactors;
  private final PlatformExecutor executor;
  private final Reactor reactor;

  private volatile TcpChannel chan;
  private volatile Session session;
  private volatile boolean ready = false;
  private LineReader reader;
  private LazyCompleter completer;
  private LineDisciplineTerminal term;
  private Thread t;
  private final String ip;
  private final int port;

  volatile boolean shouldResize = false;

  public GogoWebSocket(ReactorProvider reactors, PlatformExecutor executor, Reactor reactor, String ip, int port)
      throws IOException {
    this.reactors = Objects.requireNonNull(reactors);
    this.executor = Objects.requireNonNull(executor);
    this.reactor = Objects.requireNonNull(reactor);
    this.ip = Objects.requireNonNull(ip);
    this.port = port;

    BufferedOutputStream bos = new BufferedOutputStream(new WebSocketOutputStream());
    term = new LineDisciplineTerminal("test", "ansi", bos, StandardCharsets.US_ASCII);

    completer = new LazyCompleter();
    reader = LineReaderBuilder.builder().terminal(term).completer(completer).build();

    t = new Thread(() -> {
      term.writer().println("Connecting to Gogo...");
      term.writer().println("Pod Internal IP " + ip + " Port " + port);

      try {
        connectToTcp();
      } catch (UnknownHostException e1) {
        term.writer().println("failed to connect");
        Thread.currentThread().interrupt();
      }

      String line;
      while (!Thread.interrupted()) {
        try {
          if ((line = reader.readLine("g!")) != null) {
            if (LOG.isDebugEnabled()) {
              LOG.warn("jline returned " + line);
            }
            line = line + "\n";
            if (ready) {
              chan.send(ByteBuffer.wrap(Charset.CHARSET_US_ASCII.getBytes(line)), true);
              chan.flush();
            }
          }
        } catch (UserInterruptException e) {
          Log.warn("Interrupted");
          Thread.currentThread().interrupt();
        } catch (EndOfFileException e) {
          LOG.info("EOF");
          Thread.currentThread().interrupt();
        }
      }
      LOG.warn("jline thread exiting");
      if (chan != null) {
        chan.close();
      }

      if (session != null) {
        session.close(1000, "Interrupted");
      }
    });
  }

  @OnWebSocketConnect
  public void onConnect(Session session) {
    LOG.warn("Server opened session=" + session);
    this.session = session;
    t.start();

  }

  private void connectToTcp() throws UnknownHostException {
    Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
    opts.put(TcpClientOption.INPUT_EXECUTOR, executor);
    reactors.tcpConnect(reactor, new InetSocketAddress(InetAddress.getByName(ip), port), this, opts);
  }

  private void resizeTerminal(String msg) {
    String[] tokens = msg.split(",");
    if (tokens.length != 3) {
      return;
    }
    try {
      int cols = Integer.parseInt(tokens[1]);
      int rows = Integer.parseInt(tokens[2]);
      term.setSize(new Size(cols, rows));
      LOG.debug("resized to col " + cols + " rows " + rows);
      term.raise(Signal.WINCH);
    } catch (NumberFormatException e) {
    }
  }

  @OnWebSocketMessage
  public void onMessage(Session s, String msg) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo ws: got string message " + msg);
    }
    if (msg.startsWith("SERVER::CLOSE")) {
      reader.printAbove("Closing...");
      t.interrupt();
      ready = false;
      return;
    } else if (msg.startsWith("NokiaCLI:resize")) {
      resizeTerminal(msg);
      return;
    } else if (msg.startsWith("NokiaCLI") || msg.startsWith("SERVER")) {
      return;
    }

    try {
      if (ready) {
        term.processInputBytes(msg.getBytes(StandardCharsets.US_ASCII));
      }
    } catch (IOException e) {
      LOG.warn("IOException raised when caling processInputBytes", e);
    }
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    t.interrupt();
    session = null;
    LOG.warn("Server MyWebSocket.onClose: code=" + statusCode + " ,reason=" + reason);

  }

  @Override
  public void connectionClosed(TcpChannel arg0) {
    LOG.warn("gogo ws chan: connection closed");
    if (session != null) {
      session.close();
      session = null;
    }
  }

  private ByteArrayOutputStream baos = new ByteArrayOutputStream();

  @Override
  public int messageReceived(TcpChannel arg0, ByteBuffer arg1) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo chan: raw message " + getASCII(arg1, false));
    }
    baos.write(arg1.array(), arg1.position(), arg1.remaining());

    String msg = getASCII(ByteBuffer.wrap(baos.toByteArray()), false);
    if (!msg.endsWith(".done\n")) {
      return 0;
    }

    baos.reset();
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo chan received: " + msg);
    }

    if (!ready && msg != null) {
      Stream<String> strings = Arrays.stream(msg.split("\n")).filter((str) -> !".done".equals(str));
      StringsCompleter stringsCompl = new StringsCompleter(strings::iterator);
      completer.setWrapped(stringsCompl);
      ready = true;
      reader.printAbove("Use TAB for autocompletion and a list of command.");
    } else {
      reader.printAbove(msg);
    }
    return 0;
  }

  @Override
  public void receiveTimeout(TcpChannel arg0) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo ws tcp chan: receive timeout");
    }
  }

  @Override
  public void writeBlocked(TcpChannel arg0) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo ws tcp chan: write blocked");
    }
  }

  @Override
  public void writeUnblocked(TcpChannel arg0) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo ws tcp chan: write unblocked");
    }
  }

  @Override
  public void connectionEstablished(TcpChannel arg0) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("gogo ws chan: connection established");
    }
    this.chan = arg0;
    reader.printAbove("Connected!");
    chan.send(ByteBuffer.wrap(Charset.CHARSET_US_ASCII.getBytes("help\n")), true);
  }

  @Override
  public void connectionFailed(TcpChannel arg0, Throwable arg1) {
    LOG.warn("gogo ws chan: connection failed " + arg1);
    reader.printAbove("Failed to connect to the Pod, please try again later.");
    if (session != null) {
      session.close();
      session = null;
    }
  }

  private static String getASCII(ByteBuffer buffer, boolean consume) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    String s = getASCII(bytes, 0, bytes.length);
    if (consume == false)
      buffer.position(buffer.position() - bytes.length);
    return s;
  }

  private static String getASCII(byte[] value, int off, int len) {
    return new String(value, off, len, StandardCharsets.US_ASCII);
  }
}