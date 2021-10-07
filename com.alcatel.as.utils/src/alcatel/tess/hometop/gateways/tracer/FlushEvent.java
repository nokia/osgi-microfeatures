package alcatel.tess.hometop.gateways.tracer;

import alcatel.tess.hometop.gateways.utils.Hashtable;

/**
 * This event is sent by the Tracer in order to ask
 * the flush, or clear, or reopen log file descriptors.
 */
public class FlushEvent extends LogEvent {
  
  final static int CLEAR = 0x1;
  final static int FLUSH = 0x2;
  
  /**
   * The <code>acquire</code> method allocate a BasicLogEvent message
   * from an object pool. The release method must be called in order
   * to make this message return to its pool.
   *
   * @param tracer a <code>Tracer</code> value
   * @param flag an <code>int</code> value
   * @return a <code>FlushEvent</code> value
   */
  static FlushEvent acquire(Tracer tracer, int flag) {
    FlushEvent fe = new FlushEvent();
    fe.flag = flag;
    fe.tracer = tracer;
    return (fe);
  }
  
  public FlushEvent() {
  }
  
  int getLevel() {
    return (-1);
  }
  
  long getDate() {
    return (0);
  }
  
  int getFlag() {
    return (flag);
  }
  
  Tracer getTracer() {
    return (tracer);
  }
  
  public String toString(Hashtable contexts) {
    return (toString());
  }
  
  public String toString() {
    return ("FlushEvent(flag=" + flag + ")");
  }
  
  public void release() {
  }
  
  private int flag = 0;
  private Tracer tracer;
}
