// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

import alcatel.tess.hometop.gateways.utils.Charset;

public class Syslog {
  
  // Facilities:
  public static final int KERN = 0;
  public static final int USER = 1;
  public static final int MAIL = 2;
  public static final int DAEMON = 3;
  public static final int AUTH = 4;
  public static final int SYSLOG = 5;
  public static final int LPR = 6;
  public static final int NEWS = 7;
  public static final int UUCP = 8;
  public static final int CRON = 9;
  public static final int LOCAL0 = 16;
  public static final int LOCAL1 = 17;
  public static final int LOCAL2 = 18;
  public static final int LOCAL3 = 19;
  public static final int LOCAL4 = 20;
  public static final int LOCAL5 = 21;
  public static final int LOCAL6 = 22;
  public static final int LOCAL7 = 23;
  
  // Priorities:
  public static final int EMERG = 0;
  public static final int ALERT = 1;
  public static final int CRIT = 2;
  public static final int ERR = 3;
  public static final int WARNING = 4;
  public static final int NOTICE = 5;
  public static final int INFO = 6;
  public static final int DEBUG = 7;
  public static final int ALL = 8;
  
  public static int getCode(String priOrFacStr) throws IllegalArgumentException {
    if (priOrFacStr == null) {
      throw new IllegalArgumentException(priOrFacStr);
    }
    
    Integer i = (Integer) strToCode.get(priOrFacStr.toUpperCase());
    
    if (i == null) {
      throw new IllegalArgumentException(priOrFacStr);
    }
    
    return (i.intValue());
  }
  
  public static String getStr(int code) throws IllegalArgumentException {
    Integer i = new Integer(code);
    return ((String) codeToStr.get(i));
  }
  
  public static String getOriginalStr(int code) throws IllegalArgumentException {
    Integer i = new Integer(code);
    return ((String) codeToOriginalStr.get(i));
  }
  
  /**
   * Create a new Syslog object.
   */
  public Syslog(String host, int port, boolean tcp) throws IOException {
    this.addr = InetAddress.getByName(host);
    this.port = port;
    
    if (tcp) {
      this.socket = new Socket();
      this.socket.connect(new InetSocketAddress(addr, port), 3000); // We won't block on the connect up to 3 seconds ...
      this.socket.setKeepAlive(true);
      this.os = socket.getOutputStream();
    } else {
      this.udpSocket = new DatagramSocket();
    }
  }
  
  /**
   * Optional method that closes any open socket for logging.
   */
  public void close() {
    try {
      if (socket != null) {
        socket.close();
      } else if (udpSocket != null) {
        udpSocket.close();
      }
    } catch (Exception e) {
    }
  }
  
  public void flush() throws IOException {
    if (os != null) {
      os.flush();
    }
  }
  
  /**
   * Call for logging.  
   */
  public void log(int facility, int priority, String identity, String msg) throws Exception {
    try {
      // Message format: <facpri>date ident: msg
      
      StringBuffer buf = new StringBuffer();
      buf.append(INF);
      buf.append((facility << 3 | priority));
      buf.append(SUP);
      buf.append(identity);
      buf.append(':');
      buf.append(' ');
      
      buf.append(msg);
      buf.append('\0');
      
      if (Debug.enabled)
        Debug.p(this, "log", "sending " + buf.toString());
      
      if (os != null) {
        os.write(Charset.makeBytes(buf.toString()));
      } else {
        byte[] data = Charset.makeBytes(buf.toString());
        DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
        this.udpSocket.send(packet);
      }
      
      if (Debug.enabled)
        Debug.p(this, "log", "sending " + buf.toString());
    }
    
    catch (Exception ex) {
      if (Debug.enabled)
        Debug.p(this, "log", "error:" + ex);
      
      if (printOnException) {
        System.out.println(msg);
      }
      
      throw ex;
    }
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length != 7) {
      System.out
          .println("Usage: Syslog <tcpFlag (true/false)> <host> <port> <facility> <priority> <message> <burstSize>");
      System.exit(1);
    }
    
    boolean tcp = Boolean.valueOf(args[0]).booleanValue();
    String host = args[1];
    int port = Integer.parseInt(args[2]);
    int fac = Integer.parseInt(args[3]);
    int pri = Integer.parseInt(args[4]);
    String msg = args[5];
    int burst = Integer.parseInt(args[6]);
    
    Syslog syslog = new Syslog(host, port, tcp);
    
    for (int i = 0; i < burst; i++) {
      syslog.log(fac, pri, "test", msg);
    }
    
    syslog.flush();
    Thread.sleep(1000);
    syslog.close();
  }
  
  private static HashMap strToCode;
  private static HashMap codeToStr;
  private static HashMap codeToOriginalStr;
  
  static {
    strToCode = new HashMap();
    strToCode.put("KERN", new Integer(KERN));
    strToCode.put("USER", new Integer(USER));
    strToCode.put("MAIL", new Integer(MAIL));
    strToCode.put("DAEMON", new Integer(DAEMON));
    strToCode.put("AUTH", new Integer(AUTH));
    strToCode.put("SYSLOG", new Integer(SYSLOG));
    strToCode.put("LPR", new Integer(LPR));
    strToCode.put("NEWS", new Integer(NEWS));
    strToCode.put("UUCP", new Integer(UUCP));
    strToCode.put("CRON", new Integer(CRON));
    strToCode.put("LOCAL0", new Integer(LOCAL0));
    strToCode.put("LOCAL1", new Integer(LOCAL1));
    strToCode.put("LOCAL2", new Integer(LOCAL2));
    strToCode.put("LOCAL3", new Integer(LOCAL3));
    strToCode.put("LOCAL4", new Integer(LOCAL4));
    strToCode.put("LOCAL5", new Integer(LOCAL5));
    strToCode.put("LOCAL6", new Integer(LOCAL6));
    strToCode.put("LOCAL7", new Integer(LOCAL7));
    strToCode.put("EMERG", new Integer(EMERG));
    strToCode.put("ALERT", new Integer(ALERT));
    strToCode.put("CRIT", new Integer(CRIT));
    strToCode.put("ERR", new Integer(ERR));
    strToCode.put("WARNING", new Integer(WARNING));
    strToCode.put("NOTICE", new Integer(NOTICE));
    strToCode.put("INFO", new Integer(INFO));
    strToCode.put("DEBUG", new Integer(DEBUG));
    strToCode.put("ALL", new Integer(ALL));
    
    codeToStr = new HashMap();
    codeToStr.put(new Integer(KERN), "KERN");
    codeToStr.put(new Integer(USER), "USER");
    codeToStr.put(new Integer(MAIL), "MAIL");
    codeToStr.put(new Integer(DAEMON), "DAEMON");
    codeToStr.put(new Integer(AUTH), "AUTH");
    codeToStr.put(new Integer(SYSLOG), "SYSLOG");
    codeToStr.put(new Integer(LPR), "LPR");
    codeToStr.put(new Integer(NEWS), "NEWS");
    codeToStr.put(new Integer(UUCP), "UUCP");
    codeToStr.put(new Integer(CRON), "CRON");
    codeToStr.put(new Integer(LOCAL0), "LOCAL0");
    codeToStr.put(new Integer(LOCAL1), "LOCAL1");
    codeToStr.put(new Integer(LOCAL2), "LOCAL2");
    codeToStr.put(new Integer(LOCAL3), "LOCAL3");
    codeToStr.put(new Integer(LOCAL4), "LOCAL4");
    codeToStr.put(new Integer(LOCAL5), "LOCAL5");
    codeToStr.put(new Integer(LOCAL6), "LOCAL6");
    codeToStr.put(new Integer(LOCAL7), "LOCAL7");
    codeToStr.put(new Integer(EMERG), "EMERG");
    codeToStr.put(new Integer(ALERT), "ALERT");
    codeToStr.put(new Integer(CRIT), "CRIT");
    codeToStr.put(new Integer(ERR), "ERR");
    codeToStr.put(new Integer(WARNING), "WARNING");
    codeToStr.put(new Integer(NOTICE), "NOTICE");
    codeToStr.put(new Integer(INFO), "INFO");
    codeToStr.put(new Integer(DEBUG), "DEBUG");
    codeToStr.put(new Integer(ALL), "ALL");
    
    codeToOriginalStr = new HashMap();
    codeToOriginalStr.put(new Integer(KERN), "kernel");
    codeToOriginalStr.put(new Integer(USER), "user");
    codeToOriginalStr.put(new Integer(MAIL), "mail");
    codeToOriginalStr.put(new Integer(DAEMON), "daemon");
    codeToOriginalStr.put(new Integer(AUTH), "auth");
    codeToOriginalStr.put(new Integer(SYSLOG), "syslog");
    codeToOriginalStr.put(new Integer(LPR), "lpr");
    codeToOriginalStr.put(new Integer(NEWS), "news");
    codeToOriginalStr.put(new Integer(UUCP), "uucp");
    codeToOriginalStr.put(new Integer(CRON), "cron");
    codeToOriginalStr.put(new Integer(LOCAL0), "local0");
    codeToOriginalStr.put(new Integer(LOCAL1), "local1");
    codeToOriginalStr.put(new Integer(LOCAL2), "local2");
    codeToOriginalStr.put(new Integer(LOCAL3), "local3");
    codeToOriginalStr.put(new Integer(LOCAL4), "local4");
    codeToOriginalStr.put(new Integer(LOCAL5), "local5");
    codeToOriginalStr.put(new Integer(LOCAL6), "local6");
    codeToOriginalStr.put(new Integer(LOCAL7), "local7");
    codeToOriginalStr.put(new Integer(EMERG), "panic");
    codeToOriginalStr.put(new Integer(ALERT), "alert");
    codeToOriginalStr.put(new Integer(CRIT), "critical");
    codeToOriginalStr.put(new Integer(ERR), "error");
    codeToOriginalStr.put(new Integer(WARNING), "warning");
    codeToOriginalStr.put(new Integer(NOTICE), "notice");
    codeToOriginalStr.put(new Integer(INFO), "info");
    codeToOriginalStr.put(new Integer(DEBUG), "debug");
    codeToOriginalStr.put(new Integer(ALL), "ALL");
  }
  
  private static SimpleDateFormat monthFormat = new SimpleDateFormat("MMM ", Locale.US);
  
  private static SimpleDateFormat dayAndTimeFormat = new SimpleDateFormat("d HH:mm:ss", Locale.US);
  
  // tcp socket if we run in tcp mode
  private Socket socket;
  
  // output stream of our tcp socket (if we are running in tcp mode)
  private OutputStream os;
  
  // datagram socket if we are running in udp mode
  private DatagramSocket udpSocket;
  
  private final static String INF = "<";
  private final static String SUP = ">";
  private final static String SPACE = " ";
  private final static String EMPTY = "";
  private final static String STUF = ": ";
  private final static byte END = 0;
  
  // dest syslogd addr
  private InetAddress addr;
  
  // dest syslogd port number
  private int port;
  
  // Print to System.err in case logging fails with ane exception?
  public static boolean printOnException = false;
}
