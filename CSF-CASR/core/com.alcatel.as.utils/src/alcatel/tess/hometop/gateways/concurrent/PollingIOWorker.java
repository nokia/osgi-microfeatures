package alcatel.tess.hometop.gateways.concurrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple polling io worker thread class. This class poll some
 * (large) number of input stream and notifies listeners when
 * data is available.
 */
public class PollingIOWorker extends Thread {
  /**
   * Constructs a new polling io instance. 
   *
   * @param priority the priority under wich this thread will
   *	run.
   * @param sleepTime the amount of time to wait before restarting to
   *	poll input streams.
   */
  public PollingIOWorker(int priority, long sleepTime) {
    setPriority(priority);
    this.priority = priority;
    this.sleepTime = sleepTime;
  }
  
  /**
   * Add an io event listener. This io event listener will
   * be notified when data is available from its input stream.
   */
  public void addIOEventListener(IOEventListener t) {
    tasks.add(t);
  }
  
  /**
   * Polling io worker thread run method.
   */
  public void run() {
    while (true) {
      for (int i = 0;; i++) {
        try {
          IOEventListener t = (IOEventListener) tasks.get(i);
          
          if (t.done()) {
            tasks.remove(i);
            continue;
          }
          
          boolean trigger;
          int avail;
          
          try {
            trigger = ((avail = t.input().available()) > 0);
          }
          
          catch (IOException ex) {
            ex.printStackTrace(System.err);
            
            trigger = true; // trigger if exception on check
            tasks.remove(i);
            avail = 1;
          }
          
          if (trigger) {
            setPriority(NORM_PRIORITY);
            t.processIO(avail);
            setPriority(this.priority);
          }
        }
        
        catch (IndexOutOfBoundsException e) {
          // sleep a little bit, then restart iteration
          
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
            ie.printStackTrace();
          }
          
          break;
        }
      }
    }
  }
  
  private List tasks = (List) Collections.synchronizedList(new ArrayList());
  private long sleepTime;
  private int priority;
}
