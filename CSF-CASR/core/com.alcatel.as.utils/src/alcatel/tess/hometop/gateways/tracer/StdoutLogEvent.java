package alcatel.tess.hometop.gateways.tracer;


/**
 * This class represents a log message that must be printed
 * without any formating. For example, this event is used to
 * redirect stdout and stderr messages to the default logger
 * without adding any headers in messages.
 *
 */
public class StdoutLogEvent extends LogEvent {
  /**
   * We needs a public constructor the ObjectPool class needs it
   * (reflection api issue).
   */
  public StdoutLogEvent() {
  }
  
  /**
   * The <code>acquire</code> method allocate a StdoutLogEvent message
   * from an object pool. The release method must be called in order
   * to make this message return to its pool.
   *
   * @param tracer a <code>TracerImpl</code> value
   * @param level an <code>int</code> value
   * @param logMsg a <code>String</code> value
   * @param logX a <code>Throwable</code> value
   * @return a <code>StdoutLogEvent</code> value
   */
  static StdoutLogEvent acquire(Tracer tracer, int level, String logMsg) {
    StdoutLogEvent le = new StdoutLogEvent();
    
    le.level = level;
    le.logMsg = logMsg;
    le.tracer = tracer;
    
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
    return (logMsg);
  }
  
  static void statistics() {
  }
  
  private Tracer tracer;
  private int level;
  private String logMsg;
}
