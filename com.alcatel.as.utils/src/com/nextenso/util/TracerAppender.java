package com.nextenso.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import alcatel.tess.hometop.gateways.tracer.Tracer;
import alcatel.tess.hometop.gateways.tracer.TracerManager;

/**  Description of the Class */
public class TracerAppender extends AppenderSkeleton {
  private Tracer tracer = TracerManager.getThreadTracer();
  
  public TracerAppender() {
    super();
    tracer = TracerManager.getThreadTracer();
  }
  
  /**
   *  Description of the Method
   *
   * @param  loggingevent  Description of the Parameter
   */
  public void append(LoggingEvent loggingevent) {
    //System.out.println("TracerAppender " + loggingevent + " into " + tracer);
    StringBuffer buffer = new StringBuffer(loggingevent.getLoggerName());
    buffer.append(": ");
    buffer.append(loggingevent.getMessage());
    ThrowableInformation ti = loggingevent.getThrowableInformation();
    if (ti == null) {
      tracer.log(loggingevent.getLevel().getSyslogEquivalent(), buffer.toString());
    } else {
      tracer.log(loggingevent.getLevel().getSyslogEquivalent(), buffer.toString(), ti.getThrowable());
    }
  }
  
  /**
   *  Description of the Method
   *
   * @return    Description of the Return Value
   */
  public boolean requiresLayout() {
    return false;
  }
  
  /**  Description of the Method */
  public void close() {
    // should i do something ?
  }
}
