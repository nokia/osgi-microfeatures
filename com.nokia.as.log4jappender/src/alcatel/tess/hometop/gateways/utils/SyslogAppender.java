// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * A Syslog (rfc5424) log4j appender.
 * This class replaces the legacy SyslogNGAppender
 */
public class SyslogAppender extends AppenderSkeleton {
  /** Kernel messages */
  final static public int LOG_KERN = 0;
  /** Random user-level messages */
  final static public int LOG_USER = 1 << 3;
  /** Mail system */
  final static public int LOG_MAIL = 2 << 3;
  /** System daemons */
  final static public int LOG_DAEMON = 3 << 3;
  /** security/authorization messages */
  final static public int LOG_AUTH = 4 << 3;
  /** messages generated internally by syslogd */
  final static public int LOG_SYSLOG = 5 << 3;
  
  /** line printer subsystem */
  final static public int LOG_LPR = 6 << 3;
  /** network news subsystem */
  final static public int LOG_NEWS = 7 << 3;
  /** UUCP subsystem */
  final static public int LOG_UUCP = 8 << 3;
  /** clock daemon */
  final static public int LOG_CRON = 9 << 3;
  /** security/authorization  messages (private) */
  final static public int LOG_AUTHPRIV = 10 << 3;
  /** ftp daemon */
  final static public int LOG_FTP = 11 << 3;
  
  // other codes through 15 reserved for system use
  /** reserved for local use */
  final static public int LOG_LOCAL0 = 16 << 3;
  /** reserved for local use */
  final static public int LOG_LOCAL1 = 17 << 3;
  /** reserved for local use */
  final static public int LOG_LOCAL2 = 18 << 3;
  /** reserved for local use */
  final static public int LOG_LOCAL3 = 19 << 3;
  /** reserved for local use */
  final static public int LOG_LOCAL4 = 20 << 3;
  /** reserved for local use */
  final static public int LOG_LOCAL5 = 21 << 3;
  /** reserved for local use */
  final static public int LOG_LOCAL6 = 22 << 3;
  /** reserved for local use*/
  final static public int LOG_LOCAL7 = 23 << 3;
  
  /** log size greater than this size will be split in UDP mode */
  private static final int MAX_UDP_PACKETSIZE = 4096;
  
  /** Our hostname */
  private volatile String _host;
  
  /** The syslog server port number */
  private volatile int _port;
  
  /** connection mode */
  private volatile boolean _tcp;
  
  /** the syslog facility we are using */
  private volatile int _facility;
  
  /** the Connection object used to send messages */
  private volatile Connection _cnx;
  
  /** Must we exit when detecting out of memory exception */
  private volatile boolean _checkOutOfMemory;
  
  /** Host name used to identify messages from this appender. */
  private volatile String _localHostname;
  
  /** Date format used if header = true. */
  private volatile SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'",
      Locale.ENGLISH);
  
  /** Our jvm instance name */
  public volatile String _instanceName;
  
  /** The timestamp when we lost the syslog socket */
  private volatile long _cnxLossTime;
  
  /** Flag used to check if exceptions must be sent */
  private volatile boolean _stackTrace = true;
  
  /** Flag used to compactly format logger names and stack traces */
  private volatile boolean _compact = false;
  
  /** Min level appended to this appender */
  private volatile Level _level = Level.ALL;
  
  /**
   * Event buffer, also used as monitor to protect itself and
   * discardMap from simulatenous modifications.
   */
  private final List<LoggingEvent> _queue = new ArrayList();
  
  /** Map of DiscardSummary objects keyed by logger name. */
  private final Map<String, DiscardSummary> _discardMap = new java.util.HashMap();
  
  /** Buffer size. */
  private volatile int _queueSize = 1024;
  
  /** Thread which handles logging events */
  private Dispatcher _dispatcher;
  
  /** Message HEADER (required by mcas rsyslog configuration) */
  private volatile String _appName = "ASR";
  
  /**
   * Summary of discarded logging events for a logger.
   */
  private static final class DiscardSummary {
    /**
     * First event of the highest severity.
     */
    private LoggingEvent maxEvent;
    
    /**
     * Total count of messages discarded.
     */
    private int count;
    
    /**
     * Create new instance.
     *
     * @param event event, may not be null.
     */
    public DiscardSummary(final LoggingEvent event) {
      maxEvent = event;
      count = 1;
    }
    
    /**
     * Add discarded event to summary.
     *
     * @param event event, may not be null.
     */
    public void add(final LoggingEvent event) {
      if (event.getLevel().toInt() > maxEvent.getLevel().toInt()) {
        maxEvent = event;
      }
      
      count++;
    }
    
    /**
     * Create event with summary information.
     *
     * @return new event.
     */
    public LoggingEvent createEvent() {
      String msg = MessageFormat.format("Discarded {0} messages due to full event queue including: {1}",
                                        new Object[] { new Integer(count), maxEvent.getMessage() });
      
      return new LoggingEvent("alcatel.tess.hometop.gateways.utils.AsyncAppender.DONT_REPORT_LOCATION",
          Logger.getLogger(maxEvent.getLoggerName()), maxEvent.getLevel(), msg, null);
    }
  }
  
  /**
   * Event dispatcher.
   */
  private class Dispatcher extends Thread {
    private volatile boolean _running = true;
    
    public void run() {
      try {
        while (_running) {
          LoggingEvent[] events = null;
          
          synchronized (_queue) {
            int queueSize = _queue.size();
            while (queueSize == 0) {
              _queue.wait();
              queueSize = _queue.size();
            }
            
            if (queueSize > 0) {
              events = new LoggingEvent[queueSize + _discardMap.size()];
              _queue.toArray(events);
              
              int index = queueSize;
              for (Iterator<DiscardSummary> it = _discardMap.values().iterator(); it.hasNext();) {
                events[index++] = it.next().createEvent();
              }
              
              _queue.clear();
              _discardMap.clear();
              _queue.notifyAll();
            }
          }
          
          if (events != null) {
            for (LoggingEvent e : events) {
              _append(e);
            }
            if (_cnx != null) {
              _cnx.flush();
            }
          }
        }
      } catch (InterruptedException ex) {
      }
      
      catch (Throwable t) {
        LogLog.error("exception in syslog appender thread", t);
      }
    }
    
    public void shutdown() {
      _running = false;
      synchronized (_queue) {
        _queue.notifyAll();
      }
    }
  }
  
  /**
   * Encapsulates a tcp or udp socket.
   */
  private class Connection {
    // tcp socket if we run in tcp mode
    private Socket _tcpSocket;
    
    // output stream of our tcp socket (if we are running in tcp mode)
    private OutputStream _tcpSocketOS;
    
    // datagram socket if we are running in udp mode
    private DatagramSocket _udpSocket;
    
    // dest syslogd addr
    private InetAddress _addr;
    
    public Connection() throws IOException {
      _addr = InetAddress.getByName(_host);
      
      if (_tcp) {
        _tcpSocket = new Socket(_addr, _port);
        _tcpSocket.setKeepAlive(true);
        _tcpSocketOS = new BufferedOutputStream(_tcpSocket.getOutputStream(), 65536);
      } else {
        _udpSocket = new DatagramSocket();
      }
    }
    
    public void flush() {
      if (_tcp && _tcpSocketOS != null) {
        try {
          _tcpSocketOS.flush();
        } catch (IOException e) {
        }
      }
    }
    
    public void close() {
      try {
        if (_tcpSocket != null) {
          _tcpSocket.close();
        } else if (_udpSocket != null) {
          _udpSocket.close();
        }
      } catch (IOException e) {
      }
    }
    
    public void log(byte[] data, Level level) throws Exception {
      if (_tcpSocketOS != null) {
        _tcpSocketOS.write(data);
      } else {
        DatagramPacket packet = new DatagramPacket(data, data.length, _addr, _port);
        _udpSocket.send(packet);
      }
    }
  }
  
  /**
   * Sets a headers, which is prepended in front of every log message body.
   */
  public void setAppName(String appName) {
    _appName = appName;
  }
  
  /**
   * Sets async queue size
   */
  public void setQueueSize(int size) {
    _queueSize = size;
  }
  
  /**
   * Min level this appender accepts
   */
  public void setLevel(String minLevel) {
    _level = getLevel(minLevel);
  }
  
  /**
   * Formats compact logger name and compact stack traces.
   */
  public void setCompact(boolean compact) {
    _compact = compact;
  }
  
  /**
   * Sets flag used to send (or not) exception stacktraces.
   */
  public void setStackTrace(boolean stackTrace) {
    _stackTrace = stackTrace;
  }
  
  /**
   * Set the syslog date format
   */
  public void setDateFormat(String df) {
    _dateFormat = new SimpleDateFormat(df.trim(), Locale.ENGLISH);
  }
  
  /**
   * Set the syslogNG host name.
   */
  public void setHost(String host) {
    _host = host;
  }
  
  /**
   * Set the syslogNG port number.
   */
  public void setPort(int port) {
    _port = port;
  }
  
  /**
   * Turn on/off tcp mode.
   */
  public void setTcp(boolean tcp) {
    _tcp = tcp;
  }
  
  /**
   * Set the syslog facility.
   */
  public void setFacility(String facilityString) {
    _facility = getFacility(facilityString);
  }
  
  /**
   * Must we exit on OutOfMemory errors.
   */
  public void setCheckOutOfMemory(boolean checkOutOfMemory) {
    _checkOutOfMemory = checkOutOfMemory;
  }
  
  /**
   * Initialize our appender.
   */
  public void activateOptions() {
    _instanceName = System.getProperty("platform.agent.instanceName");
    if (_instanceName != null) {
      _instanceName = _instanceName.replace(" ", "");
    }
    _dispatcher = new Dispatcher();
    _dispatcher.setName("SyslogAppender");
    _dispatcher.setDaemon(true);
    _dispatcher.start();
  }
  
  public void close() {
    if (_cnx != null) {
      _cnx.close();
    }
    if (_dispatcher != null) {
      _dispatcher.shutdown();
    }
  }
  
  public boolean requiresLayout() {
    return false;
  }
  
  /**
   * Method called by AppenderSkeleton (already synchronized).
   */
  public void append(LoggingEvent event) {
    if (!event.getLevel().isGreaterOrEqual(_level)) {
      return;
    }
    
    // Set the NDC and thread name for the calling thread as these
    // LoggingEvent fields were not set at event creation time.
    event.getNDC();
    event.getThreadName();
    // Get a copy of this thread's MDC.
    event.getMDCCopy();
    
    synchronized (_queue) {
      int previousSize = _queue.size();
      if (previousSize < _queueSize) {
        _queue.add(event);
        
        // Buffer was empty: signal all threads waiting on buffer to check their conditions.
        if (previousSize == 0) {
          _queue.notifyAll();
        }
        
        return;
      }
      
      // Queue is full, we have to discard this logging event.
      String loggerName = event.getLoggerName();
      DiscardSummary summary = _discardMap.get(loggerName);
      
      if (summary == null) {
        _discardMap.put(loggerName, new DiscardSummary(event));
      } else {
        summary.add(event);
      }
    }
  }
  
  // Called from dispatcher thread.  
  public void _append(LoggingEvent event) {
    try {
      StringBuilder packet = new StringBuilder();
      StringBuilder syslogHdr = getPacketHeader(event);
      
      if (getLayout() != null) {
        packet.append(syslogHdr);
        BufferedReader reader = new BufferedReader(new StringReader(super.layout.format(event)));
        String line;
        while ((line = reader.readLine()) != null) {
          packet.append(line);
          packet.append("\n");
        }
      } else {
        StringBuilder packetHdr = new StringBuilder();
        formatMessage(packetHdr, event);
        BufferedReader reader = new BufferedReader(new StringReader(event.getRenderedMessage()));
        String line;
        while ((line = reader.readLine()) != null) {
          packet.append(syslogHdr);
          if (packetHdr != null) {
            packet.append(packetHdr);
            packetHdr = null;
          }
          packet.append(line);
          packet.append("\n");
        }
      }
      
      if ((getLayout() == null || layout.ignoresThrowable()) && event.getThrowableInformation() != null) {
        // Dump exception message with full stacktrace, but on one line, possibly in compact form
        packet.setLength(packet.length() - 1); // remove last \n
        String[] s = event.getThrowableStrRep();
        if (s != null) {
          packet.append(" - ");
          boolean firstAt = true;
          for (int i = 0; i < s.length; i++) {
            if (s[i] != null) {
              if (s[i].startsWith("\tat ")) {
                if (_stackTrace) {
                  if (firstAt) {
                    firstAt = false;
                    packet.append(" - at ");
                  } else {
                    packet.append(", ");
                  }
                  compactStackTrace(packet, s[i].substring(4));
                }
              } else {
                packet.append(s[i]);
              }
            }
          }
        }
      }
      
      packet.append("\n");
      
      byte[] packetBytes = packet.toString().getBytes();
      
      if (!_tcp && packetBytes.length > MAX_UDP_PACKETSIZE) {
        splitPacket(event, packetBytes);
      } else {
        send(event, packetBytes);
      }
      if (checkOutOfMemoryError(event, null)) {
        Runtime.getRuntime().halt(1);
      }
    }
    
    catch (IOException e) {
      _cnxLossTime = System.currentTimeMillis();
      if (_cnx != null) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lost syslog connection ");
        sb.append("(host=").append(_host);
        sb.append(", port=").append(_port);
        if (_instanceName != null) {
          sb.append(", logging instance name=").append(_instanceName);
        }
        sb.append("): ");
        sb.append(e);
        LogLog.error(sb.toString());
        
        _cnx.close();
        _cnx = null;
      }
    }
    
    catch (Throwable t) {
      LogLog.error("Got unexpected exception while logging to syslog", t);
      if (checkOutOfMemoryError(null, t)) {
        Runtime.getRuntime().halt(1);
      }
    }
  }
  
  // See rfc5424: <fac|pri>1 TIMESTAMP(yyyy-MM-dd'T'HH:mm:ss.S'Z') HOST APPNAME PROCID MSGID MESSAGE
  private StringBuilder getPacketHeader(LoggingEvent event) {
    StringBuilder buf = new StringBuilder();
    int priority = event.getLevel().getSyslogEquivalent();
    long timestamp = event.getTimeStamp();
    
    // Message format: <facpri>date host ident millis group_instance
    buf.append("<");
    buf.append(_facility | priority);
    buf.append(">1 "); // version
    
    // Add 3164 date
    buf.append(_dateFormat.format(new Date(timestamp)));
    buf.append(' ');
    
    // Add host
    buf.append(getLocalHostname());
    buf.append(' ');
    
    // Add appname
    buf.append(_appName);
    buf.append(' ');
    
    // Add empty procid (we can't put instancename, because procid length must not exceed 32 chars).
    buf.append(_instanceName == null ? '-' : _instanceName);
    
    // Unknown msg id 
    buf.append(" - ");
    return buf;
  }
  
  // default message formatter (%c %t %p - %m)
  private void formatMessage(StringBuilder packet, LoggingEvent event) {
    compactLogger(packet, event.getLoggerName());
    packet.append(" ").append(event.getThreadName());
    packet.append(" ").append(event.getLevel());
    packet.append(" - ");
  }
  
  /**
   * Compacts names that look like fully qualified class names. All packages
   * will be shortened to the first letter, except for the last one. So
   * something like "foo.bar.MyClass" will become "f.b.MyClass".
   */
  private void compactLogger(StringBuilder output, String input) {
    if (_compact) {
      int lastIndex = 0;
      for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        switch (c) {
        case '.':
          output.append(input.charAt(lastIndex));
          output.append('.');
          lastIndex = i + 1;
          break;
        }
      }
      if (lastIndex < input.length()) {
        output.append(input.substring(lastIndex));
      }
    } else {
      output.append(input);
    }
  }
  
  /**
   * Compacts names that look like fully qualified class names. All packages
   * will be shortened to the first letter, except for the last one. 
   * Examples:
   *    "java.lang.Thread.run(Thread.java:722)" = "j.l.T.run(Thread.java:722)".
   *    "foo.bar.MyClass$Test$Inner.test(MyClass.java:804)" = "f.b.M$T$I.test(MyClass.java:804)
   *    "foo.bar.MyClass$Test.access$700(SyslogAppender.java:792)" = "f.b.M$T.a$7(SyslogAppender.java:792)
   */
  private void compactStackTrace(StringBuilder output, String input) {
    if (_compact) {
      int lastIndex = 0;
      
      for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        switch (c) {
        case '.':
        case '$':
          output.append(input.charAt(lastIndex));
          output.append(c);
          lastIndex = i + 1;
          break;
        case '(':
          int rightPar = input.indexOf(')', i);
          if (rightPar != -1) {
            output.append(input.substring(lastIndex, i));
            output.append(input.substring(i, rightPar + 1));
          }
          return;
        }
      }
    } else {
      output.append(input);
    }
  }
  
  private void splitPacket(LoggingEvent event, byte[] packet) throws Exception {
    if (packet.length <= MAX_UDP_PACKETSIZE) {
      send(event, packet);
    } else {
      byte[] begin = new byte[MAX_UDP_PACKETSIZE];
      System.arraycopy(packet, 0, begin, 0, MAX_UDP_PACKETSIZE - 1);
      begin[MAX_UDP_PACKETSIZE - 1] = '\n';
      send(event, begin);
      
      String end = new String(packet, MAX_UDP_PACKETSIZE - 1, packet.length - MAX_UDP_PACKETSIZE + 1);
      _append(new LoggingEvent(event.getFQNOfLoggerClass(), event.getLogger(), event.getTimeStamp(),
          event.getLevel(), end, event.getThreadName(), null, event.getNDC(), event.getLocationInformation(),
          event.getProperties()));
      
    }
  }
  
  public static String parse(Throwable e) {
    StringWriter buffer = new StringWriter();
    PrintWriter pw = new PrintWriter(buffer);
    e.printStackTrace(pw);
    return (buffer.toString());
  }
  
  private void send(LoggingEvent event, byte[] message) throws Exception {
    // Send the message.
    if (_cnx == null) {
      if (_cnxLossTime == 0 || (System.currentTimeMillis() - _cnxLossTime) > 3000) {
        _cnx = new Connection();
        _cnxLossTime = 0;
      } else {
        return;
      }
    }
    
    _cnx.log(message, event.getLevel());
  }
  
  // -----------------------------------------------------------------------------------------------
  //  Private methods
  // -----------------------------------------------------------------------------------------------
  
  /**
   * Get the host name used to identify this appender.
   * @return local host name
   */
  private String getLocalHostname() {
    if (_localHostname == null) {
      try {
        InetAddress addr = InetAddress.getLocalHost();
        _localHostname = addr.getHostName();
      } catch (UnknownHostException uhe) {
        _localHostname = "UNKNOWN_HOST";
      }
    }
    return _localHostname;
  }
  
  private boolean checkOutOfMemoryError(LoggingEvent event, Throwable t) {
    if (_checkOutOfMemory) {
      if (event != null) {
        ThrowableInformation ti = event.getThrowableInformation();
        if (ti != null && ti.getThrowable() instanceof OutOfMemoryError) {
          return true;
        }
      }
      if (t != null && t instanceof OutOfMemoryError) {
        return true;
      }
    }
    
    return false;
  }
  
  private Level getLevel(String level) {
    if (level.equalsIgnoreCase("ERROR")) {
      return Level.ERROR;
    } else if (level.equalsIgnoreCase("WARN")) {
      return Level.WARN;
    } else if (level.equalsIgnoreCase("INFO")) {
      return Level.INFO;
    } else if (level.equalsIgnoreCase("DEBUG")) {
      return Level.DEBUG;
    } else if (level.equalsIgnoreCase("ALL")) {
      return Level.ALL;
    } else {
      return Level.ALL;
    }
  }
  
  private static int getFacility(String facilityName) {
    if (facilityName != null) {
      facilityName = facilityName.trim();
    }
    if ("KERN".equalsIgnoreCase(facilityName)) {
      return LOG_KERN;
    } else if ("USER".equalsIgnoreCase(facilityName)) {
      return LOG_USER;
    } else if ("MAIL".equalsIgnoreCase(facilityName)) {
      return LOG_MAIL;
    } else if ("DAEMON".equalsIgnoreCase(facilityName)) {
      return LOG_DAEMON;
    } else if ("AUTH".equalsIgnoreCase(facilityName)) {
      return LOG_AUTH;
    } else if ("SYSLOG".equalsIgnoreCase(facilityName)) {
      return LOG_SYSLOG;
    } else if ("LPR".equalsIgnoreCase(facilityName)) {
      return LOG_LPR;
    } else if ("NEWS".equalsIgnoreCase(facilityName)) {
      return LOG_NEWS;
    } else if ("UUCP".equalsIgnoreCase(facilityName)) {
      return LOG_UUCP;
    } else if ("CRON".equalsIgnoreCase(facilityName)) {
      return LOG_CRON;
    } else if ("AUTHPRIV".equalsIgnoreCase(facilityName)) {
      return LOG_AUTHPRIV;
    } else if ("FTP".equalsIgnoreCase(facilityName)) {
      return LOG_FTP;
    } else if ("LOCAL0".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL0;
    } else if ("LOCAL1".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL1;
    } else if ("LOCAL2".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL2;
    } else if ("LOCAL3".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL3;
    } else if ("LOCAL4".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL4;
    } else if ("LOCAL5".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL5;
    } else if ("LOCAL6".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL6;
    } else if ("LOCAL7".equalsIgnoreCase(facilityName)) {
      return LOG_LOCAL7;
    } else {
      return -1;
    }
  }
  
  static class Test {
    private void run() throws Exception {
      Inner i = new Inner();
      i.test();
    }
    
    class Inner {
      void test() throws Exception {
        Logger l = Logger.getLogger(SyslogAppender.class);
        l.warn("multiline\ntest #");            
        l.warn("multiline\ntest with Exception", new Exception("stacktrace"));
      }
    }
  }
  
  public static void main(String ... args) throws Exception {
    Test t = new Test();
    t.run();
    Thread.sleep(Integer.MAX_VALUE);
  }
}
