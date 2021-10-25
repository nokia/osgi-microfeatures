// // Copyright 2000-2021 Nokia
// //
// // Licensed under the Apache License 2.0
// // SPDX-License-Identifier: Apache-2.0
// //
//
//

package com.nokia.as.diameter.tools.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.StopWatch;

public class TcpDiameterLoader extends DiameterLoader {
  private static Logger _logger = Logger.getLogger(TcpDiameterLoader.class);
  
  @Override
  void connect(InetSocketAddress from, InetSocketAddress to) throws IOException {
    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(false);
    if (from != null) {
      socket.socket().bind(from);
    }
    socket.socket().setKeepAlive(false);
    socket.socket().setTcpNoDelay(true);
    socket.socket().setReuseAddress(true);
    SelectionKey sk = socket.register(_selector, 0);
    
    if (!socket.connect(to)) {
      sk.interestOps(SelectionKey.OP_CONNECT);
    } else {
      connected(sk);
    }
  }
  
  @Override
  void connected(SelectionKey sk) throws IOException {
    SocketChannel sc = (SocketChannel) sk.channel();
    
    if (!sc.finishConnect()) {
      _logger.error("Can not finish connect");
      System.exit(2);
    }
    Executor queue = new SerialQueue(_tpool);
    PeerContext ctx = new TcpPeerContext(_bulkSize, _acrUserName, _fillbackSize, queue);
    sk.attach(ctx);
    sk.interestOps(SelectionKey.OP_READ);
    sendCer(ctx._originHost, sc);
    _connected.incrementAndGet();
  }
  
  void sendCer(String originHost, SocketChannel sc) throws IOException {
    byte[] cer = DiameterUtils.makeCER(originHost);
    if (sc.write(ByteBuffer.wrap(cer)) <= 0) {
      _logger.error("could not send initial CER");
      System.exit(2);
    }
  }
  
  @Override
  void readReady(final SelectionKey sk) throws Exception {
    final SocketChannel sc = (SocketChannel) sk.channel();
    final PeerContext ctx = (PeerContext) sk.attachment();
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_READ);
    
    Runnable r = new Runnable() {
      public void run() {
        try {
          int n;
          ByteBuffer rcvBuf = ctx._rcvBuf;
          
          // read socket, until no more data is available
          readLoop: while (true) {
            if ((n = sc.read(rcvBuf)) < 0) {
              _logger.error("Socket closed");
              System.exit(1);
            }
            
            if (n == 0) {
              rcvBuf.compact();
              break;
            } else if (n < 0) {
              _logger.error("socket error");
              System.exit(2);
            }
            
            // parse Diameter responses
            rcvBuf.flip();
            while (rcvBuf.hasRemaining()) {
              int len = _parser.parse(rcvBuf);
              if (len < 0) {
                rcvBuf.compact();
                break readLoop;
              }
              
              // check if message is a DPA
              if (DiameterUtils.isDPA(rcvBuf)) {
                rcvBuf.clear();
                dpaReceived(sk);
                sk.cancel();
                sc.close();
                return;
              }
              
              boolean NOK = DiameterUtils.isNOK(rcvBuf);
              if (!ctx._ceaReceived) {
                ctx._ceaReceived = true;
                rcvBuf.clear();
                ceaReceived(sk);
                break readLoop;
              }
              received(rcvBuf, !NOK);
              rcvBuf.position(rcvBuf.position() + len);
            }
            rcvBuf.clear();
          }
          
          scheduleReadInterest(sk);
        } catch (Throwable t) {
          _logger.warn("socket error", t);
          System.exit(2);
        } finally {
        }
      }
    };
    ctx._executor.execute(r);
  }
  
  @Override
  void writeReady(final SelectionKey sk) throws Exception {
    final SocketChannel socket = (SocketChannel) sk.channel();
    final PeerContext ctx = (PeerContext) sk.attachment();
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
    
    Runnable r = new Runnable() {
      public void run() {
        try {
          if (!ctx.flush(sk)) {
            scheduleWriteInterest(sk);
            return;
          }
          
          // send a bulk of diameter requests, if no tps is configured
          if (_tpsManager == null) {
            if (!_shutdown) {
              // send ACR
              ByteArrayOutputStream out = ctx._out;
              out.reset();

	      int count = 0;
	      
              // check if we can send ACRs, or if we must 
              while (out.size() < _bulkSize) {
                int hopID = DiameterUtils.changeHopIds(ctx._acr);
		DiameterUtils.changeSessionId (ctx._acr, count++);
                out.write(ctx._acr);
                sending(hopID);
              }
              
              if (send(ctx, socket, out.toByteArray(), false)) {
                ctx.setBlocked(false);
              } else {
                ctx.setBlocked(true);
              }
              scheduleWriteInterest(sk);
            } else {
              // send DPR
              if (!ctx._dprSent) {
                _logger.info("sending DPR ...");
                ctx._dprSent = true;
                byte[] dpr = DiameterUtils.makeDPR(ctx._originHost);
                if (send(ctx, socket, dpr, false)) {
                  ctx.setBlocked(false);
                } else {
                  ctx.setBlocked(true);
                  scheduleWriteInterest(sk);
                }
              }
            }
          } else {
            ctx.setBlocked(false);
          }
        } catch (Throwable t) {
          _logger.error("error while writing", t);
          System.exit(2);
        }
      }
    };
    ctx._executor.execute(r);
  }
  
  @Override
  void startTpsTimer(final SelectionKey sk) {
    final SocketChannel socket = (SocketChannel) sk.channel();
    final PeerContext ctx = (PeerContext) sk.attachment();
    if (ctx._tpsTimer != null) {
      ctx._tpsTimer.cancel(false);
    }
    long period = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS) / _tpsPerConnections;
    _logger.info("setting TPS connection to " + TimeUnit.MILLISECONDS.convert(period, TimeUnit.NANOSECONDS));

    final AtomicInteger count = new AtomicInteger ();
    
    ctx._tpsTimer = _timer.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        if (!ctx.isBlocked()) {
          ctx._executor.execute(new Runnable() {
            @Override
            public void run() {
              if (!ctx.isBlocked()) {
                if (_shutdown) {
                  if (!ctx._dprSent) {
                    _logger.info("send DPR ...");
                    // send DPR
                    ctx._dprSent = true;
                    byte[] dpr = DiameterUtils.makeDPR(ctx._originHost);
                    if (!send(ctx, socket, dpr, false)) {
                      ctx.setBlocked(true);
                      scheduleWriteInterest(sk);
                    }
                  }
                } else {
                  // send ACR
                  int hopID = DiameterUtils.changeHopIds(ctx._acr);
		  DiameterUtils.changeSessionId (ctx._acr, count.getAndIncrement ());
                  sending(hopID);
                  if (!send(ctx, socket, ctx._acr, true)) {
                    ctx.setBlocked(true);
                    scheduleWriteInterest(sk);
                  }
                }
              }
            }
          });
        }
      }
    }, 0, period, TimeUnit.NANOSECONDS);
  }
  
  private boolean send(PeerContext ctx, SocketChannel socket, byte[] data, boolean copy) {
    if (_fragment) {
      return sendWithMultipleWrites(ctx, socket, data, copy);
    }
    try {
      ByteBuffer b = ByteBuffer.wrap(data);
      while (b.hasRemaining()) {
        long sent = socket.write(b);
        if (sent == 0) {
          if (copy) {
            ByteBuffer tmp = ByteBuffer.allocate(b.remaining());
            tmp.put(b);
            tmp.flip();
            b = tmp;
          }
          ctx.enqueue(b);
          return false;
        } else if (sent < 0) {
          _logger.error("socket error while sending");
          System.exit(2);
        }
      }
    } catch (IOException e) {
      _logger.error("exception while writing", e);
      System.exit(2);
    } finally {
    }
    return true;
  }
  
  private boolean sendWithMultipleWrites(PeerContext ctx, SocketChannel socket, byte[] data, boolean copy) {
    try {
      ByteBuffer b = ByteBuffer.wrap(data);
      while (b.hasRemaining()) {  
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
//        Thread.sleep(0, rnd.nextInt(0, 10));
                
        int chunkSize = b.remaining() > 1 ? rnd.nextInt(1, b.remaining()) : 1;
        long sent = socket.write(ByteBuffer.wrap(b.array(), b.position() + b.arrayOffset(), chunkSize));
        if (sent == 0) {
          if (copy) {
            ByteBuffer tmp = ByteBuffer.allocate(b.remaining());
            tmp.put(b);
            tmp.flip();
            b = tmp;
          }
          ctx.enqueue(b);
          return false;
        } else if (sent < 0) {
          _logger.error("socket error while sending");
          System.exit(2);
        } else {
          for (int i = 0; i < sent; i ++) b.get(); // consume bytes sent.
        }
      }
    } catch (Exception e) {
      _logger.error("exception while writing", e);
      System.exit(2);
    } finally {
    }
    return true;
  }
}
