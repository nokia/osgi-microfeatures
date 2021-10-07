package alcatel.tess.hometop.gateways.reactor.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.StopWatch;

/**
 * Reactor metrics. One Metrics instance per Reactor object.
 */
public class Meters extends SimpleMonitorable {
  private final MeteringService _metering;
  private final static boolean DO_IP_METERS = Boolean.getBoolean("reactor.socketMeters");
  private final static AtomicInteger _tcpPrefix = new AtomicInteger();
  private final static AtomicInteger _sctpPrefix = new AtomicInteger();

  public Meters(MeteringService metering, BundleContext bctx) {
    super("as.service.reactor", "Reactor Metrics");
    _metering = metering;
    if (bctx != null) {
      super.start(bctx);
    }
  }
  
  public ReactorMeters newReactorMeters(String reactorName) {
    return new ReactorMeters(reactorName);
  }
  
  public SelectorMeters newSelectorMeters(String selectorName) {
      return new SelectorMeters(selectorName);
  }

  // Each reactor instance may create its own meters.
  public class ReactorMeters {
    private final String _reactorName;
    
    // Tcp (global to all sockets)
    private final Meter _tcpWriteBytes; // number of bytes sent
    private final Meter _tcpWriteBuffer; // number of tcp buffered bytes
    private final Meter _tcpReadBytes; // number of bytes received
    private final Meter _tcpWrites; // number of tcp write system calls
    private final Meter _tcpReads; // number of tcp read system calls
    private final Meter _tcpWriteBlocked; // number of write blocked (because socket send-buf is full)
    private final Meter _tcpWriteUnblocked; // number of write unblocked (because socket becomes ready to be written)
    private final Meter _tcpWriteDelay; // flush delay in nano
    private final Meter _tcpWriteDelayReady;
    private final Meter _tcpWriteDelaySchedule; 

    // Sctp
    private final Meter _sctpWriteBytes; // number of bytes sent
    private final Meter _sctpReadBytes; // number of bytes received
    private final Meter _sctpWriteBuffer; // number of tcp buffered bytes
    private final Meter _sctpWrites; // number of tcp write system calls
    private final Meter _sctpReads; // number of tcp read system calls
    
    // Udp
    private final Meter _udpWriteBytes; // number of bytes sent
    private final Meter _udpReadBytes; // number of bytes received
    private final Meter _udpWriteBuffer; // number of tcp buffered bytes
    private final Meter _udpWrites; // number of tcp write system calls
    private final Meter _udpReads; // number of tcp read system calls
    private final Meter _udpSessions; // number of active udp sessions

    ReactorMeters(String reactorName) {
      _reactorName = reactorName;
      
      //
      // Create some meters. The last "false" flag means "do not update service properties" because we'll do that 
      // once all meters have been created.
      //
      _tcpWriteBytes = createIncrementalMeter(_metering, reactorName + ":*:tcp.write.bytes", null);
      _tcpWriteBuffer = createIncrementalMeter(_metering, _reactorName + ":*:tcp.write.buffer", null); 
      _tcpReadBytes = createIncrementalMeter(_metering, reactorName + ":*:tcp.read.bytes", null);
      _tcpWrites = createIncrementalMeter(_metering, reactorName + ":*:tcp.write.events", null);
      _tcpReads = createIncrementalMeter(_metering, reactorName + ":*:tcp.read.events", null);
      _tcpWriteBlocked = createIncrementalMeter(_metering, reactorName + ":*:tcp.write.blocked", null);
      _tcpWriteUnblocked = createIncrementalMeter(_metering, reactorName + ":*:tcp.write.unblocked", null);
      _tcpWriteDelay = createIncrementalMeter(_metering, _reactorName + ":*:tcp.write.delay", null);
      _tcpWriteDelayReady = createIncrementalMeter(_metering, _reactorName + ":*:tcp.write.delay.ready", null);
      _tcpWriteDelaySchedule =  createIncrementalMeter(_metering, _reactorName + ":*:tcp.write.delay.schedule", null);

      _sctpWriteBytes = createIncrementalMeter(_metering, reactorName + ":*:sctp.write.bytes", null);
      _sctpReadBytes = createIncrementalMeter(_metering, reactorName + ":*:sctp.read.bytes", null);
      _sctpWriteBuffer = createIncrementalMeter(_metering, reactorName + ":*:sctp.write.buffer", null);
      _sctpWrites = createIncrementalMeter(_metering, reactorName + ":*:sctp.write.events", null);
      _sctpReads = createIncrementalMeter(_metering, reactorName + ":*:sctp.read.events", null);
      
      _udpWriteBytes = createIncrementalMeter(_metering, reactorName + ":udp.write.bytes", null);
      _udpReadBytes = createIncrementalMeter(_metering, reactorName + ":udp.read.bytes", null);
      _udpWriteBuffer = createIncrementalMeter(_metering, reactorName + ":udp.write.buffer", null);
      _udpWrites = createIncrementalMeter(_metering, reactorName + ":udp.write.events", null);
      _udpReads = createIncrementalMeter(_metering, reactorName + ":udp.read.events", null);
      _udpSessions = createIncrementalMeter(_metering, reactorName + ":udp.session.count", null);
    }
    
    // All meters are created: now we can update our service properties.
    void updated() {
    	Meters.this.updated();
    }
            
    // Udp
            
    void udpWriteBytes(long bytesOut) {
      _udpWriteBytes.inc(bytesOut);
    }
    
    void udpReadBytes(long bytesIn) {
      _udpReadBytes.inc(bytesIn);
    }
    
    void udpWriteBuffer(long bytes) {
      _udpWriteBuffer.inc(bytes);
    }
    
    void udpWrite() {
      _udpWrites.inc(1);
    }
    
    void udpRead() {
      _udpReads.inc(1);
    }
    
    void udpSession(int addedSession) {
    	_udpSessions.inc(addedSession);
    }
    
    TcpMeters newTcpMeters(InetSocketAddress to) {
      return new TcpMeters(to);
    }

    SctpMeters newSctpMeters(InetSocketAddress to) {
      return new SctpMeters(to);
    }
    
    // Each Tcp Channel of a given reactor create its own meters.
    public class TcpMeters {
      // Tcp
      private final Meter _tcpWriteBytes; // number of bytes sent
      private final Meter _tcpWriteBuffer; // number of tcp buffered bytes
      private final Meter _tcpReadBytes; // number of bytes received
      private final Meter _tcpWrites; // number of tcp write system calls
      private final Meter _tcpReads; // number of tcp read system calls
      private final Meter _tcpWriteBlocked; // number of write blocked (because socket send-buf is full)
      private final Meter _tcpWriteUnblocked; // number of write unblocked (because socket becomes ready to be written)
      private final Meter _tcpWriteDelayReady;
      private final Meter _tcpWriteDelaySchedule; 

      TcpMeters(InetSocketAddress to) {
        if (DO_IP_METERS) {
          final String tostr = _tcpPrefix.incrementAndGet() + "/" + to.toString().replace("/", "").replace(":", "/");              
          _tcpWriteBytes = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.bytes", ReactorMeters.this._tcpWriteBytes);
          _tcpReadBytes = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.read.bytes", ReactorMeters.this._tcpReadBytes);
          _tcpWriteBuffer = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.buffer", ReactorMeters.this._tcpWriteBuffer);
          _tcpWrites = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.events", ReactorMeters.this._tcpWrites);
          _tcpReads = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.read.events", ReactorMeters.this._tcpReads);
          _tcpWriteBlocked = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.blocked", ReactorMeters.this._tcpWriteBlocked);
          _tcpWriteUnblocked = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.unblocked", ReactorMeters.this._tcpWriteUnblocked);
          _tcpWriteDelayReady = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.delay.ready", ReactorMeters.this._tcpWriteDelayReady);
          _tcpWriteDelaySchedule =  createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":tcp.write.delay.schedule", ReactorMeters.this._tcpWriteDelaySchedule);
          
          // All meters are created: now we can update our service properties.
          updated();
        } else {
          _tcpWriteBytes = ReactorMeters.this._tcpWriteBytes;
          _tcpReadBytes = ReactorMeters.this._tcpReadBytes;
          _tcpWriteBuffer = ReactorMeters.this._tcpWriteBuffer;
          _tcpWrites = ReactorMeters.this._tcpWrites;
          _tcpReads = ReactorMeters.this._tcpReads;
          _tcpWriteBlocked = ReactorMeters.this._tcpWriteBlocked;
          _tcpWriteUnblocked = ReactorMeters.this._tcpWriteUnblocked;
          _tcpWriteDelayReady = ReactorMeters.this._tcpWriteDelayReady;
          _tcpWriteDelaySchedule =  ReactorMeters.this._tcpWriteDelaySchedule;
        }
      }
      
      void close() {
        if (DO_IP_METERS) {
          removeMeter(_tcpWriteBytes);
          removeMeter(_tcpReadBytes);
          removeMeter(_tcpWriteBuffer);
          removeMeter(_tcpWrites);
          removeMeter(_tcpReads);
          removeMeter(_tcpWriteBlocked);
          removeMeter(_tcpWriteUnblocked);
          removeMeter(_tcpWriteDelayReady);
          removeMeter(_tcpWriteDelaySchedule);
          updated();
        }
      }
      
      // Tcp
      
      StopWatch startTcpWriteDelayReadyWatch() {
        return _tcpWriteDelayReady.startWatch(false);
      }
      
      StopWatch startTcpWriteDelayScheduleWatch() {
        return _tcpWriteDelaySchedule.startWatch(false);
      }
      
      void tcpBuffered(int buffered) {
        _tcpWriteBuffer.inc(buffered);
      }
      
      void tcpWriteBlocked() {
        _tcpWriteBlocked.inc(1);
      }
      
      void tcpWriteUnblocked() {
        _tcpWriteUnblocked.inc(1);
      }

      void tcpWriteBytes(long bytesOut) {
        _tcpWriteBytes.inc(bytesOut);
      }
      
      void tcpReadBytes(long bytesIn) {
        _tcpReadBytes.inc(bytesIn);
      }
            
      void tcpWrite() {
        _tcpWrites.inc(1);
      }
      
      void tcpRead() {
        _tcpReads.inc(1);
      }            
    }
    
    // Each Sctp Channel of a given reactor create its own meters.
    public class SctpMeters {
      private final Meter _sctpWriteBytes; // number of bytes sent
      private final Meter _sctpReadBytes; // number of bytes received
      private final Meter _sctpWriteBuffer; // number of tcp buffered bytes
      private final Meter _sctpWrites; // number of tcp write system calls
      private final Meter _sctpReads; // number of tcp read system calls
      
      SctpMeters(InetSocketAddress to) {
        if (DO_IP_METERS) {
          final String tostr = _sctpPrefix.incrementAndGet() + "/" + to.toString().replace("/", "").replace(":", "/");              
          _sctpWriteBytes = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":sctp.write.bytes", ReactorMeters.this._sctpWriteBytes);
          _sctpReadBytes = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":sctp.read.bytes", ReactorMeters.this._sctpReadBytes);
          _sctpWriteBuffer = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":sctp.write.buffer", ReactorMeters.this._sctpWriteBuffer);
          _sctpWrites = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":sctp.write.events", ReactorMeters.this._sctpWrites);
          _sctpReads = createIncrementalMeter(_metering, _reactorName + ":" + tostr + ":sctp.read.events", ReactorMeters.this._sctpReads);

          // All meters are created: now we can update our service properties.
          updated();
        } else {
          _sctpWriteBytes = ReactorMeters.this._sctpWriteBytes;
          _sctpReadBytes = ReactorMeters.this._sctpReadBytes;
          _sctpWriteBuffer = ReactorMeters.this._sctpWriteBuffer;
          _sctpWrites = ReactorMeters.this._sctpWrites;
          _sctpReads = ReactorMeters.this._sctpReads;
        }
      }
      
      void close() {
        if (DO_IP_METERS) {
          removeMeter(_sctpWriteBytes);
          removeMeter(_sctpReadBytes);
          removeMeter(_sctpWriteBuffer);
          removeMeter(_sctpWrites);
          removeMeter(_sctpReads);
          updated();
        }
      }
      
      void sctpWriteBytes(long bytesOut) {
        _sctpWriteBytes.inc(bytesOut);
      }
      
      void sctpReadBytes(long bytesIn) {
        _sctpReadBytes.inc(bytesIn);
      }
      
      void sctpWriteBuffer(long bytes) {
        _sctpWriteBuffer.inc(bytes);
      }
      
      void sctpWrite() {
        _sctpWrites.inc(1);
      }
      
      void sctpRead() {
        _sctpReads.inc(1);
      }
    }
  }
  
  // Each Selector create its own meters.
  public class SelectorMeters {
    private final Meter _tcpSecuredChannels; // number of tcp secured channels managed by this selector
    private final Meter _tcpChannels; // number of tcp channels managed by this selector
    private final Meter _sctpSecuredChannels; // number of sctp secured channels managed by this selector
    private final Meter _sctpChannels; // number of sctp channels managed by this selector

    SelectorMeters(String selectorName) {
    	_tcpSecuredChannels = createIncrementalMeter(_metering, "selector" + ":" + selectorName + ":tcp.channels.secured", null);
    	_tcpChannels = createIncrementalMeter(_metering, "selector" + ":" + selectorName + ":tcp.channels", null);
    	_sctpSecuredChannels = createIncrementalMeter(_metering, "selector" + ":" + selectorName + ":sctp.channels.secured", null);
    	_sctpChannels = createIncrementalMeter(_metering, "selector" + ":" + selectorName + ":sctp.channels", null);
    }
    
    void close() {
    }
    
    void addTcpChannel(int count, boolean secure) {
    	if (secure) {
    		_tcpSecuredChannels.inc(count);
    	} else {
        	_tcpChannels.inc(count);
    	}
    }    
    
    void addSctpChannel(int count, boolean secure) {
    	if (secure) {
    		_sctpSecuredChannels.inc(count);
    	} else {
    		_sctpChannels.inc(count);
    	}
    }    
  }
}

