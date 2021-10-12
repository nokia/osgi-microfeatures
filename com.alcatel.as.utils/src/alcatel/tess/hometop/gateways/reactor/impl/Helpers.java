// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.ThreadContext;

import alcatel.tess.hometop.gateways.reactor.Channel;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;
import alcatel.tess.hometop.gateways.utils.Log;
import alcatel.tess.hometop.gateways.utils.Utils;

class Helpers {
	
  public interface ThrowingConsumer<E extends Exception> {
	  void accept() throws E;
  }

  private final static Logger _logClose = Logger.getLogger("as.service.reactor.close");
  private final static int SND_BUF_SIZE;
  private final static int RCV_BUF_SIZE;
  private final static Log _logger = Log.getLogger("as.service.reactor.BufferHelper");
  private static volatile Executor _defaultOutputExecutor;
  private final static Object BARRIER = new Object();
  // Counter used to span tcp channels over all selector threads 
  public final static AtomicInteger _tcpSelectorCounter = new AtomicInteger();
  // Counter used to span udp channels over all selector threads 
  public final static AtomicInteger _udpSelectorCounter = new AtomicInteger();
  // Counter used to span tcp secure channels over all selector threads 
  public final static AtomicInteger _tlsSelectorCounter = new AtomicInteger();
  // Counter used to span sctp channels over all selector threads 
  public final static AtomicInteger _sctpSelectorCounter = new AtomicInteger();
  private static Security _clientSSLConfig;
  private static Security _serverSSLConfig;
  
  /**
   * Flag used to control whether the socket writes are dispatch to the processing threadpool (true by default).
   * if true, it means socket writes may happen while a socket read is happening. but it might not be a good idea
   * to allow concurrent socket read/write, because Socket read/write methods are actually synchronized ...
   * if you set the reactor.write.dispatch to false, you shall also reduce the reactor.rcvBufSize to a smaller value
   * like 4k, else writes could be delayed too much ...
   */
  private final static boolean _dispatchWrites = "true".equals(System.getProperty("reactor.write.dispatch", "true"));

  /**
   * Static initializer.
   */
  static {
    RCV_BUF_SIZE = parseBytes("reactor.rcvBufSize", "64k");
    SND_BUF_SIZE = parseBytes("reactor.sndBufSize", "64k");
    _logger.info("TcpChannel buffer sizes: sndBufSize=%d, rcvBufSize=%d", SND_BUF_SIZE, RCV_BUF_SIZE);
  }
  
  private final static class Buffers {
	  Buffers() {
		  _buffer =  ByteBuffer.allocate(Helpers.getRcvBufSize());
		  _directBuffer = ByteBuffer.allocateDirect(Helpers.getRcvBufSize());
	  }
	  final ByteBuffer _buffer;
	  final ByteBuffer _directBuffer;
  }
  
  private final static ThreadLocal<Buffers> _currThreadRcvBuf = new ThreadLocal<Buffers>() {
    protected Buffers initialValue() {
      return new Buffers();
    }
  };
  
  public static <T> void runSafe(ThrowingConsumer<Exception> throwingConsumer) {
	  try {
		  throwingConsumer.accept();
	  } catch (Exception ex) {
		  throw new RuntimeException(ex);
	  }
  }
  
  /**
   * Tells whether write system calls be scheduled in the processing threadpool ? 
   * (true by default, see important comment from the _dispatchWrites class attribute)
   */
  public static boolean dispatchWrites() {
	  return _dispatchWrites;
  }
  
  public static ByteBuffer getCurrentThreadReceiveBuffer(boolean direct) {
	  Buffers buffers = _currThreadRcvBuf.get();
	  return direct ? buffers._directBuffer : buffers._buffer;
  }
  
  static void setDefaultOutputExecutor(Executor executor) {
    _defaultOutputExecutor = executor;
  }
  
  static Executor getDefaultOutputExecutor() {
    return _defaultOutputExecutor;
  }
  
  static boolean isSocketException(Throwable t) {
    return t instanceof IOException || t instanceof CancelledKeyException;
  }
  
  static Object copy(Object msg) {
    if (msg instanceof ByteBuffer) {
      return copy((ByteBuffer) msg);
    }
    return copy((ByteBuffer[]) msg);
  }
  
  static ByteBuffer copy(ByteBuffer buf) {
    ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
    copy.put(buf);
    copy.flip();
    return copy;
  }
  
  static ByteBuffer[] copy(ByteBuffer[] bufs) {
    ByteBuffer[] copy = new ByteBuffer[bufs.length];
    for (int i = 0; i < bufs.length; i++) {
      copy[i] = ByteBuffer.allocate(bufs[i].remaining());
      copy[i].put(bufs[i]);
      copy[i].flip();
    }
    return copy;
  }
  
  static ByteBuffer[] duplicate(ByteBuffer[] bufs, int off, int len) {
    ByteBuffer[] dups = new ByteBuffer[len];
    for (int i = 0; i < len; i++) {
      dups[i] = bufs[i + off].duplicate();
    }
    return dups;
  }
  
  static int length(ByteBuffer[] bufs) {
    int length = 0;
    for (int i = 0; i < bufs.length; i++) {
      length += bufs[i].remaining();
    }
    return length;
  }
  
  static ByteBuffer compact(ByteBuffer[] bufs) {
    if (bufs.length == 1) {
      return bufs[0];
    }
    
    int length = Helpers.length(bufs);
    ByteBuffer buf = ByteBuffer.allocate(length);
    for (int i = 0; i < bufs.length; i++) {
      buf.put(bufs[i]);
    }
    buf.flip();
    return buf;
  }
  
  static void copyTo(ByteBuffer from, ByteBuffer to, int n) {
    int oldLimit = from.limit();
    from.limit(from.position() + n);
    to.put(from);
    from.limit(oldLimit);
  }
  
  public static int getRcvBufSize() {
    return RCV_BUF_SIZE;
  }
  
  public static int getSndBufSize() {
    return SND_BUF_SIZE;
  }
  
  /**
   * Logs a received packaet
   * 
   * @param logger the logger to use when logging
   * @param buf the buffer to log. This buffer will be duplicated in order to avoid modifying current
   *        buffer position
   * @param addr The local/remote address endpoint, which is receiving the data.
   */
  static void logPacketReceived(Logger logger, ByteBuffer buf, Object addr) {
    StringBuilder sb = new StringBuilder();
    sb.append("Received ").append(buf.remaining()).append(" bytes from ").append(addr.toString());
    if (logger.isDebugEnabled()) {
      sb.append(":\n");
      buf = buf.duplicate();
      Helpers.dumpBuffers(sb, buf.remaining(), buf);
      logger.debug(sb.toString());
    } else {
      logger.info(sb.toString());
    }
  }
  
  /**
   * Logs a sent packet
   * 
   * @param logger the logger to use.
   * @param buf the packet sent. WARNING: The buf won't be duplicated and the buf position will be affected by this method.
   * @param size the number of bytes sent
   * @param addr the address when the packet has been sent
   */
  static void logPacketSent(Logger logger, boolean sent, ByteBuffer buf, int size, Object addr) {
    StringBuilder sb = new StringBuilder();
    sb.append(sent ? "Sent " : "Sending ");
    sb.append(String.valueOf(size));
    sb.append(" bytes to ");
    sb.append(addr);
    
    if (logger.isDebugEnabled()) {
      sb.append(":\n");
      ByteOutputStream out = new ByteOutputStream();
      try {
        long bytesLogged = 0;
        
        while (buf.hasRemaining()) {
          if (bytesLogged >= size) {
            break;
          }
          out.write(buf.get());
          bytesLogged++;
        }
        
        byte[] bytes = out.toByteArray(false);
        boolean dump = true;
        for (int i = 0; i < out.size(); i++) {
          if (!Utils.isPrintable(bytes[i])) {
            sb.append(Utils.toString(bytes, 0, out.size()));
            dump = false;
            break;
          }
        }
        
        if (dump) {
          sb.append(new String(bytes, 0, out.size()));
        }
        logger.debug(sb.toString());
      } finally {
        try {
          out.close();
        } catch (IOException e) {
        }
      }
    } else {
      logger.info(sb.toString());
    }
  }
  
  // Make visible all current thread variables to another executor (memory barrier)
  static void barrier(final Executor exec) {
    synchronized (BARRIER) {
      exec.execute(new Runnable() {
        public void run() {
          synchronized (BARRIER) {
            if (_logger.isDebugEnabled()) {
              _logger.debug("performed barrier with executor %s", exec);
            }
          }
        }
      });
    }
  }
  
  // Start a server channel (make a barrier, and enable accept interest)
  static void accept(NioSelector selector, final SelectionKey key, Executor inputExecutor) {
    Helpers.barrier(inputExecutor);
    // Accept
    selector.scheduleNow(new Runnable() {
      public void run() {
        try {
          key.interestOps(SelectionKey.OP_ACCEPT);
        } catch (Throwable t) {
          // the key is probably cancelled, or the socket has been closed
          _logger.info("enableReading failed (selection key cancelled or socket closed)", t);        
          Object attached = key.attachment();
          if (attached instanceof Channel) {
           ((Channel) attached).shutdown();
          }
        }
      }
    });
  }
    
  private static StringBuilder dumpBuffers(StringBuilder sb, long count, ByteBuffer ... bufs) {
    ByteOutputStream out = new ByteOutputStream();
    try {
      long bytesLogged = 0;
      
      loop: for (int i = 0; i < bufs.length; i++) {
        while (bufs[i].hasRemaining()) {
          if (bytesLogged >= count) {
            break loop;
          }
          out.write(bufs[i].get());
          bytesLogged++;
        }
      }
      
      byte[] bytes = out.toByteArray(false);
      boolean dump = true;
      for (int i = 0; i < out.size(); i++) {
        if (!Utils.isPrintable(bytes[i])) {
          sb.append(Utils.toString(bytes, 0, out.size()));
          dump = false;
          break;
        }
      }
      
      if (dump) {
        sb.append(new String(bytes, 0, out.size()));
      }
      return sb;
    } finally {
      try {
        out.close();
      } catch (IOException e) {
      }
    }
  }
  
  private static int parseBytes(String prop, String def) {
    String s = System.getProperty(prop, def);
    try {
      s = s.trim();
      if (s.endsWith("k") || s.endsWith("K")) {
        s = s.substring(0, s.length() - 1);
        return Integer.parseInt(s) * 1024;
      } else if (s.endsWith("m") || s.endsWith("M")) {
        s = s.substring(0, s.length() - 1);
        return Integer.parseInt(s) * 1024 * 1024;
      } else {
        return Integer.parseInt(s);
      }
    } catch (NumberFormatException e) {
      _logger.warn("Could not parse socket buffer size: %s. Using 4096 size by default", s);
      return 4096;
    }
  }

  public static ByteBuffer duplicate(ByteBuffer[] bufs, int len) {
    int bytes = 0;
    for (int i = 0; i < len; i++) {
      bytes += bufs[i].remaining();
    }
    ByteBuffer copy = ByteBuffer.allocate(bytes);
    for (int i = 0; i < len; i++) {
      bufs[i].mark();
      copy.put(bufs[i]);
      bufs[i].reset();
    }    
    copy.flip();
    return copy;
  }
  
  public static String toString(Collection<?> list) {
    StringBuilder sb = new StringBuilder("[");
    if (list != null && list.size() > 0) {
      for (Object o : list) {
        sb.append(o);
        sb.append(",");
      }
      sb.setLength(sb.length()-1);
    }
    sb.append("]");
    return sb.toString();
  }
    
  public static synchronized Security getDefaulClientSSLConfig() {
		String clientPass;

		if (_clientSSLConfig != null) {
			return _clientSSLConfig;
		}

		URL params = Utils.getResource("tls/tls.properties", Thread.currentThread().getContextClassLoader(),
				ClassLoader.getSystemClassLoader());

		if (params == null) {
			throw new RuntimeException("TLSEngine: tls/tls.properties not found from classpath");
		}

		try (InputStream in = params.openStream()) {
			Properties p = new Properties();
			p.load(in);
			clientPass = (String) p.get("client.authFilePassword");

		} catch (IOException e) {
			throw new RuntimeException("Can't load tls/tls.properties file", e);
		}

		URL ccclient = Utils.getResource("tls/ccclient.auth", Thread.currentThread().getContextClassLoader(),
				ClassLoader.getSystemClassLoader());
		if (ccclient == null) {
			throw new RuntimeException("TLSEngine: tls/ccclient.auth not found from classpath");
		}

		try (InputStream in = ccclient.openStream()) {
			_clientSSLConfig = new Security().keyStore(in).keyStorePassword(clientPass).build();
			return _clientSSLConfig;
		} catch (Exception e) {
			throw new RuntimeException("Can't load tls/ccclient.auth file", e);
		}
  	}
	  
	public static synchronized Security getDefaulServerSSLConfig() {
		if (_serverSSLConfig != null) {
			return _serverSSLConfig;
		}

		URL params = Utils.getResource("tls/tls.properties", Thread.currentThread().getContextClassLoader(),
				ClassLoader.getSystemClassLoader());
		if (params == null) {
			throw new RuntimeException("TLSEngine: tls/tls.properties not found from the classpath");
		}

		Properties p = null;
		try (InputStream in = params.openStream()) {
			p = new Properties();
			p.load(in);
		} catch (IOException e) {
			throw new RuntimeException("TLSEngine: can't load tls/tls.properties", e);
		}

		_logger.debug("loaded tls.properties: %s", p);

		String serverPass = (String) p.get("server.authFilePassword");
		boolean serverNeedsClientAuth = Boolean.valueOf(p.getProperty("server.needsClientAuth", "false"));

		URL ccserver = Utils.getResource("tls/ccserver.auth", Thread.currentThread().getContextClassLoader(),
				ClassLoader.getSystemClassLoader());
		if (ccserver == null) {
			throw new RuntimeException("TLSEngine: tls/ccserver.auth not found from the classpath");
		}

		try (InputStream in = ccserver.openStream()) {
			_serverSSLConfig = new Security().keyStore(in).keyStorePassword(serverPass).authenticateClients(serverNeedsClientAuth).build();
			return _serverSSLConfig;
		} catch (Exception e) {
			throw new RuntimeException("TLSEngine: can't load tls/ccserver.auth", e);
		}
	}
	
	public static void schedule(Executor queue, ExecutorPolicy policy, Runnable task) {
		if (queue instanceof PlatformExecutor) {
			((PlatformExecutor) queue).execute(task, policy);
		} else if (queue instanceof Reactor) {
			((Reactor) queue).getPlatformExecutor().execute(task, policy);
		} else {
			queue.execute(task);
		}
	}

	/**
	 * Returns true if current thread is being run inside the specified queue
	 */
	public static boolean isCurrentThreadInQueue(Executor queue) {
		PlatformExecutor queueExec = null;
		
		if (queue instanceof PlatformExecutor) {
			queueExec = (PlatformExecutor) queue;
		} else if (queue instanceof Reactor) {
			queueExec = ((Reactor) queue).getPlatformExecutor();
		}

		if (queueExec != null) {
			ThreadContext ctx = queueExec.getPlatformExecutors().getCurrentThreadContext();
			PlatformExecutor currentThreadQueue = ctx.getCurrentExecutor();
			return currentThreadQueue.equals(queueExec);
		} else {
			return false; // queue is not assumed to be anything else than a PfExec or a Reactor ! anyway, let's return false
		}
	}

	public static void channelAborted(Channel channel, Throwable t) {
	    if (_logClose.isEnabledFor(Level.DEBUG)) {
			_logClose.debug("channel aborted: " + channel, t);
		}
	}

	public static void channelClosing(Channel channel) {
	    if (_logClose.isEnabledFor(Level.DEBUG)) {
			_logClose.debug("closing " + channel, new Exception());
		}
	}
	
	public static void channelShuttingDown(Channel channel) {
	    if (_logClose.isEnabledFor(Level.DEBUG)) {
			_logClose.debug("shutting down " + channel, new Exception());
		}
	}

}
