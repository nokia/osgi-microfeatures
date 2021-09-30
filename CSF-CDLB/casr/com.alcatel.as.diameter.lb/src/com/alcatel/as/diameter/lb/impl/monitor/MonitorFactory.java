package com.alcatel.as.diameter.lb.impl.monitor;

/**
 * Helper class used to easily switch between monitor implementations.
 */
public class MonitorFactory {
  public static Monitor newMonitor() {
    return newMonitor (null);
  }
  public static Monitor newMonitor(Object attachment) {
    return new LongAdderMonitor(attachment);
  }
  public static Monitor newMonitor (Monitor aggregated){
    return newMonitor (aggregated, null);
  }
  public static Monitor newMonitor (final Monitor aggregated, Object attachment){
    if (aggregated == null) return newMonitor (attachment); // for conveniency
    final Monitor m = newMonitor (attachment);
    return new Monitor (){
      public void add(long n){
	m.add (n);
	aggregated.add (n);
      }
      public void increment(){
	m.increment ();
	aggregated.increment ();
      }			     
      public void decrement(){
	m.decrement ();
	aggregated.decrement ();
      }
      public long get(){
	return m.get ();
      }
      public <T> T attachment (){
	return m.attachment ();
      }
    };
  }
}
