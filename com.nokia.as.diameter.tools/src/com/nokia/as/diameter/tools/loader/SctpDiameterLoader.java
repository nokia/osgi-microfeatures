package com.nokia.as.diameter.tools.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.metering.StopWatch;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpStandardSocketOptions;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

public class SctpDiameterLoader extends DiameterLoader {
  private NotificationListener _sctpListener;
  private static Logger _logger = Logger.getLogger(SctpDiameterLoader.class);
  
  class NotificationListener extends AbstractNotificationHandler<SctpChannel> {
    public HandlerResult handleNotification(Notification notification, SctpChannel attachment) {
      _logger.info("Handling Sctp Notification: " + notification);
      return HandlerResult.CONTINUE;
    }
    
    public HandlerResult handleNotification(AssociationChangeNotification notification, SctpChannel attachment) {
      _logger.info("Handling Sctp AssociationChangeNotification: " + notification);
      return HandlerResult.CONTINUE;
    }
    
    public HandlerResult handleNotification(PeerAddressChangeNotification notification, SctpChannel channel) {
      _logger.info("Handling Sctp PeerAddressChangeNotification: " + notification);
      return HandlerResult.CONTINUE;
    }
    
    public HandlerResult handleNotification(SendFailedNotification notification, SctpChannel attachment) {
      _logger.info("Handling Sctp SendFailedNotification: " + notification);
      return HandlerResult.CONTINUE;
    }
    
    public HandlerResult handleNotification(ShutdownNotification notification, SctpChannel attachment) {
      _logger.info("Handling Sctp ShutdownNotification notification: " + notification);
      return HandlerResult.CONTINUE;
    }
  }
  
  public SctpDiameterLoader() {
    _sctpListener = new NotificationListener();
  }
  
  @Override
  void connect(InetSocketAddress from, InetSocketAddress to) throws IOException {
    final SctpChannel socket = SctpChannel.open();
    socket.setOption(SctpStandardSocketOptions.SCTP_NODELAY, false);
    if (from != null) {
      socket.bind(from);
    }
    if (_sctpSecondary != null) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(_sctpSecondarySchedule);
                    _logger.warn("Adding local secondary address: " + _sctpSecondary);
                    socket.bindAddress(InetAddress.getByName(_sctpSecondary));
                }
                catch (Throwable e) {
                    _logger.warn("Failed to bind local secondary address: " + _sctpSecondary, e);
                }
            }
        };
        if (_sctpSecondarySchedule == 0) {
            task.run();
        } else {
            new Thread(task).start();
        }
    }
    socket.configureBlocking(false);
    SelectionKey sk = socket.register(_selector, 0);
    sk.attach(this);
    
    if (!socket.connect(to, 10, 10)) {
      sk.interestOps(SelectionKey.OP_CONNECT);
    } else {
      connected(sk);
    }
  }
  
  @Override
  void connected(SelectionKey sk) throws IOException {
    SctpChannel sc = (SctpChannel) sk.channel();
    if (!sc.finishConnect()) {
      _logger.error("Connect error");
      System.exit(2);
    }
    Executor queue = new SerialQueue(_tpool);
    PeerContext ctx = new SctpPeerContext(_bulkSize, _acrUserName, _fillbackSize, queue);
    sk.attach(ctx);
    sk.interestOps(SelectionKey.OP_READ);
    sendCer(ctx._originHost, sc);
    _connected.incrementAndGet();
  }
  
  void sendCer(String originHost, SctpChannel sc) throws IOException {
    _logger.warn("sending CER ...");
    byte[] cer = DiameterUtils.makeCER(originHost);
    MessageInfo info = MessageInfo.createOutgoing(null, 0);
    info.complete(true);
    int sent = sc.send(ByteBuffer.wrap(cer), info);
    if (sent <= 0) {
      _logger.error("Socket write error: could not send initial CER");
      System.exit(2);
    }
  }
  
  @Override
  void readReady(final SelectionKey sk) throws Exception {
    final SctpChannel sc = (SctpChannel) sk.channel();
    final PeerContext ctx = (PeerContext) sk.attachment();
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_READ);
    
    Runnable r = new Runnable() {
      public void run() {
        try {
          MessageInfo info;
          
          // read socket, until no more data is available
          ctx._rcvBuf.clear();
          while ((info = sc.receive(ctx._rcvBuf, null, _sctpListener)) != null) {
            if (info.bytes() == -1) {
              // -1 means EOF. The peer probably closed, and our listener 
              // has received a SHUTDOWN event (and must explicitly invoke cnx.close
              // for really closing our channel).
              throw new ClosedChannelException();
            }
            
            if (info.bytes() == 0) {
              // 0 means we probably got a service event (our listener has received it).
              // in this case, nothing to do.
              ctx._rcvBuf.clear();
              continue;
            }
            
            // parse response
            ctx._rcvBuf.flip();
            
            while (ctx._rcvBuf.hasRemaining()) {
              int len = _parser.parse(ctx._rcvBuf);
              if (len < 0) {
                _logger.error("received imcomplete message");
                System.exit(1);
              }
              
              // check if message is a DPA
              if (DiameterUtils.isDPA(ctx._rcvBuf)) {
                dpaReceived(sk);
                sk.cancel();
                sc.close();
                return;
              }
              
              boolean NOK = DiameterUtils.isNOK(ctx._rcvBuf);
              if (!ctx._ceaReceived) {
                ctx._ceaReceived = true;
                ceaReceived(sk);
                scheduleReadInterest(sk);
                return;
              }
              received(ctx._rcvBuf, !NOK);
              ctx._rcvBuf.position(ctx._rcvBuf.position() + len);
            }
            
            ctx._rcvBuf.clear();
          }
          
          scheduleReadInterest(sk);
        } catch (IOException t) {
          _logger.error("IO exception reading socket", t);
          System.exit(2);
        } catch (Throwable t) {
          _logger.error("unexpected exception reading socket", t);
          System.exit(1);
        }
      }
    };
    
    ctx._executor.execute(r);
  }
  
  @Override
  void writeReady(final SelectionKey sk) throws Exception {
    final SctpChannel socket = (SctpChannel) sk.channel();
    final PeerContext ctx = (PeerContext) sk.attachment();
    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
    
    Runnable r = new Runnable() {
      @SuppressWarnings("hiding")
      public void run() {
        try {
          // flush pending requests
          if (!ctx.flush(sk)) {
            scheduleWriteInterest(sk);
            return;
          }
          
          // send a bulk of diameter requests, if no tps is configured
          if (_tpsManager == null) {
            if (!_shutdown) {
              // Send ACR
              ByteArrayOutputStream out = ctx._out;
              out.reset();
              while (out.size() < _bulkSize) {
                int hopID = DiameterUtils.changeHopIds(ctx._acr);
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
        } catch (IOException t) {
          _logger.error("IOException while writing", t);
          System.exit(2);
        } catch (Throwable t) {
          _logger.error("unexpected exception while writing", t);
          System.exit(1);
        }
      }
    };
    ctx._executor.execute(r);
  }
  
  @Override
  void startTpsTimer(final SelectionKey sk) {
    final SctpChannel socket = (SctpChannel) sk.channel();
    final PeerContext ctx = (PeerContext) sk.attachment();
    if (ctx._tpsTimer != null) {
      ctx._tpsTimer.cancel(false);
    }
    long period = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS) / _tpsPerConnections;
    _logger.info("setting TPS connection to " + TimeUnit.MILLISECONDS.convert(period, TimeUnit.NANOSECONDS));
    
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
  
  private boolean send(PeerContext ctx, SctpChannel socket, byte[] data, boolean copy) {
    try {
      ByteBuffer b = ByteBuffer.wrap(data);
      long sent = 0;
      MessageInfo info = MessageInfo.createOutgoing(null, 0);
      info.complete(true);
      sent = socket.send(b, info);
      if (sent < 0) {
        _logger.error("socket write error");
        System.exit(2);
      } else if (sent == 0) {        
        if (copy) {
          ByteBuffer tmp = ByteBuffer.allocate(b.remaining());
          tmp.put(b);
          tmp.flip();
          b = tmp;
        }
        
        ctx.enqueue(b);
        return false;
      }
    } catch (IOException e) {
      _logger.error("exception while writing", e);
      System.exit(2);
    } finally {
    }
    return true;
  }
}
