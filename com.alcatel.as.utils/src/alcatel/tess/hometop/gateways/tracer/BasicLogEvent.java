// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import java.io.PrintWriter;
import java.io.StringWriter;

import alcatel.tess.hometop.gateways.utils.DateFormatter;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * Class declaration
 *
 *
 * @author
 */
public class BasicLogEvent extends LogEvent {
  public BasicLogEvent() {
  }
  
  /**
   * The <code>acquire</code> method allocate a BasicLogEvent message
   * from an object pool. The release method must be called in order
   * to make this message return to its pool.
   *
   * @param tracer a <code>TracerImpl</code> value
   * @param level an <code>int</code> value
   * @param logMsg a <code>String</code> value
   * @param logX a <code>Throwable</code> value
   * @return a <code>BasicLogEvent</code> value
   */
  static BasicLogEvent acquire(TracerImpl tracer, int level, String logMsg, Throwable logX, TracerBox box) {
    BasicLogEvent le = new BasicLogEvent();
    
    le.level = level;
    le.logMsg = logMsg;
    le.logX = logX;
    le.date = System.currentTimeMillis();
    le.thread = Thread.currentThread();
    le.context = box.getContext(le.thread);
    le.tracer = tracer;
    
    if (tracer.doStackTrace() == true && logX == null) {
      le.logX = new StackException();
    }
    
    return (le);
  }
  
  static BasicLogEvent acquire(TracerImpl tracer, int level, String logMsg, Throwable logX) {
    BasicLogEvent le = new BasicLogEvent();
    
    le.level = level;
    le.logMsg = logMsg;
    le.logX = logX;
    le.tracer = tracer;
    
    if (tracer.doStackTrace() == true && logX == null) {
      le.logX = new StackException();
    }
    
    return (le);
  }
  
  static BasicLogEvent acquire(TracerImpl tracer, int level, String logMsg, Throwable logX, long date,
                               Thread thread, String context) {
    BasicLogEvent le = new BasicLogEvent();
    
    le.level = level;
    le.logMsg = logMsg;
    le.logX = logX;
    le.tracer = tracer;
    le.date = date;
    le.thread = thread;
    le.context = context;
    
    if (tracer.doStackTrace() == true && logX == null) {
      le.logX = new StackException();
    }
    
    return (le);
  }
  
  /**
   * Method declaration
   */
  void release() {
  }
  
  Tracer getTracer() {
    return (this.tracer);
  }
  
  int getLevel() {
    return (this.level);
  }
  
  public String toString() {
    StringBuffer tmp = new StringBuffer();
    
    if (date != 0L && tracer.doInsertHeader() == true) {
      tmp.append(BEGIN_HOOK);
      tmp.append(tracer.getApplInstance());
      
      if (context != null) {
        tmp.append(LEFT_PAR);
        tmp.append(context);
        tmp.append(RIGHT_PAR);
      }
      
      tmp.append(SPACE);
      tmp.append(tracer.getName());
      
      tmp.append(SPACE);
      tmp.append(Level.levelStr[level]);
      
      tmp.append(SPACE);
      tmp.append(thread.getName());
      
      tmp.append(SPACE);
      df.format(date, tmp);
      tmp.append(END_HOOK);
    }
    
    if (logX instanceof StackException) {
      // its a debugging stack trace
      if (logMsg != null) {
        tmp.append(logMsg);
      }
      tmp.append(Utils.LINE_SEPARATOR);
      formatStackTrace(tmp);
      tmp.append(Utils.LINE_SEPARATOR);
    } else if (logX != null) {
      // its an error
      
      if (logMsg != null) {
        tmp.append(logMsg);
        if (!logMsg.endsWith(Utils.LINE_SEPARATOR)) {
          tmp.append(Utils.LINE_SEPARATOR);
        }
      }
      
      tmp.append(Utils.parse(logX));
    } else if (logMsg != null) {
      // neither a stack trace nor an exception: its a log message.
      tmp.append(logMsg);
      if (!logMsg.endsWith(Utils.LINE_SEPARATOR)) {
        tmp.append(Utils.LINE_SEPARATOR);
      }
    }
    
    return (tmp.toString());
  }
  
  static void statistics() {
  }
  
  /**
   * Method declaration
   *
   * @param sb
   */
  private void formatStackTrace(StringBuffer sb) {
    StringWriter buffer = new StringWriter();
    
    this.logX.printStackTrace(new PrintWriter(buffer));
    
    String st = buffer.toString();
    String tmp = st.substring(STACK_EXCEPTION.length());
    int i;
    
    while ((i = tmp.indexOf(PACKAGE)) != -1) {
      tmp = tmp.substring(i);
      
      if ((i = tmp.indexOf(Utils.LINE_SEPARATOR)) != -1) {
        tmp = tmp.substring(i + Utils.LINE_SEPARATOR.length());
      }
    }
    
    int index = tmp.lastIndexOf(Utils.LINE_SEPARATOR);
    
    if (index != -1) {
      tmp = tmp.substring(0, index);
    }
    
    sb.append(tmp.toString());
  }
  
  private TracerImpl tracer;
  private int level;
  private String logMsg;
  private Throwable logX;
  private long date;
  private Thread thread;
  private String context;
  
  private final static String LEFT_PAR = "(";
  private final static String RIGHT_PAR = ")";
  private final static String SPACE = " ";
  private final static String THREAD = "T=";
  private final static String CNAME = "C=";
  private final static String DATE = "D=";
  private final static String SEMI = ":";
  private final static String BEGIN_HOOK = "[";
  private final static String LEVEL = "L=";
  private final static String END_HOOK = "] ";
  private final static String STACK_EXCEPTION = "alcatel.tess.hometop.gateways.tracer.StackException";
  private final static String PACKAGE = "alcatel.tess.hometop.gateways.tracer.";
  
  final static DateFormatter df = new DateFormatter(DateFormatter.SIMPLE_FORMAT);
}
