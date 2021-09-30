package alcatel.tess.hometop.gateways.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
  This class manages timers in an efficient manner.

  <p>A timer can be started through the static method:
  <br><code>
    NxTimer timer = NxTimer.start (handler, myObject, duration) ;
  </code>
  <br>Where handler is a user object which implements the NxTimerHandler
  interface. The timer expires after the number of seconds specified in
  the <b><i>duration</i></b> parameter, at which time the <b><i>timerExpired</i></b>
  method of the user handler is called with the user object <b><i>myObject</i></b>
  in parameter.

  <p>The timer may be stopped at any time while it is running through the
  stop method of the timer object. As an example, the following code starts
  a timer and waits for an external event to occur. The timer is used to
  limit the amount of time we wait for the external event to 10 seconds.

  <pre>
    class WaitEvent implements NxTimerHandler
    {
      private boolean timerHasExpired ;

      boolean waitForEvent()
      {
        //
        // Start our timer
        //
        NxTimer timer = null ;
        timerHasExpired = false ;
        try
        {
          timer = NxTimer.start (this, null, 10) ;
        }
        catch (Exception e)
        {
          System.out.println ("Unexpected timer exception") ;
          e.printStackTrace() ;
          return false ;
        }

        //
        // Wait for our external event to occur
        //
        for (;;)
        {
          synchronized (timer)
          {
            //
            // Get out of here if our timer has expired: we did not get
            // our external event in time...
            //
            if (timerHasExpired)
            {
              return false ;
            }

            //
            // If we received our external event, stop the timer and get
            // out of here
            //
            if (didWeGetOurExternalEvent())
            {
              timer.stop() ;
              return true ;
            }
          }
          try
          {
            //
            // Sleep a bit and check again
            //
            Thread.sleep (100) ;
          }
          catch (InterruptedException e)
          {
          }
        }
      }

      //
      // Our timer expiration handler. Note the fact our timer has expired
      //
      void timerExpired (Object o) {
        synchronized (timer)
        {
          timerHasExpired = true ;
        }
      }
    }
  </pre>
*/
public class NxTimer extends TimerTask {
  /** Maximum timer duration we support */
  private static final int MaxDuration = 20000;
  /** Maximum number of entries in our pool */
  private static final int PoolSize = 200;
  
  /**
    Start a new timer with the specified duration. The timerExpired method
    of the provided TimerHandler object will be called with the userObject
    in parameter when the timer has reached its expiration time.

    @param handler Timer expiration handler
    @param userObject User object associated with this timer
    @param duration Timer duration in milliseconds
    @return NxTimer object
    @throws NxTimerDurationException if the specified duration is out of range
    @throws NxTimerHandlerException if no timer handler is provided
  */
  public static NxTimer start(NxTimerHandler handler, Object userObject, int duration)
      throws NxTimerDurationException, NxTimerHandlerException {
    //
    // Make sure the requested duration is valid
    //
    if (duration <= 0) {
      throw new NxTimerDurationException("Negative duration");
    }
    if (duration >= MaxDuration) {
      throw new NxTimerDurationException("Duration exceed maximum of " + MaxDuration);
    }
    
    //
    // Make sure we have a handler to callback
    //
    if (handler == null) {
      throw new NxTimerHandlerException();
    }
    
    //
    // Get a new timer object, initialize it, and add it to the proper list
    // of active timers as per its duration
    //
    synchronized (pool) {
      //
      // Get a new Timer object. We grab one from our free pool if possible,
      // allocate a new one otherwise
      //
      NxTimer timer;
      if (pool.size() != 0) {
        timer = (NxTimer) pool.remove(pool.size() - 1);
      } else {
        timer = new NxTimer();
      }
      
      //
      // Initialize the timer structure
      //
      timer.allocated = true;
      timer.handler = handler;
      timer.userObject = userObject;
      
      //
      // And append the new timer to the proper list
      //
      timer.sourceList = (List) activeTimers.get((duration == 0) ? 1 : duration);
      timer.sourceList.add(timer);
      return timer;
    }
  }
  
  /**
    A private empty constructor...
  */
  private NxTimer() {
  }
  
  /**
    Our run method only used on our reference timer to handle our
    clock tick.
  */
  public void run() {
    synchronized (pool) {
      List list = (List) activeTimers.remove(0);
      for (Iterator i = list.iterator(); i.hasNext();) {
        NxTimer timer = (NxTimer) i.next();
        i.remove();
        if (!timer.allocated) {
          System.err.println("Skipping unallocated timer in active timer list");
          continue;
        }
        try {
          timer.handler.timerExpired(timer.userObject);
        } catch (Throwable t) {
        }
        timer.release();
      }
      activeTimers.add(list);
    }
  }
  
  /**
    Stop this timer.
  */
  public void stop() throws NxUnallocatedTimerException {
    synchronized (pool) {
      if (!allocated) {
        throw new NxUnallocatedTimerException();
      }
      if (sourceList != null) {
        sourceList.remove(this);
        release();
      }
    }
  }
  
  /**
    Return this NxTimer object to our pool.
  */
  private void release() {
    handler = null;
    userObject = null;
    sourceList = null;
    allocated = false;
    if (pool.size() < PoolSize) {
      pool.add(this);
    }
  }
  
  /** Our pool of available NxTimer objects */
  private static final List pool = new ArrayList();
  /** Our list of active timers */
  private static List activeTimers = null;
  /** User handler associated with this timer */
  private NxTimerHandler handler = null;
  /** User object associated with this timer */
  private Object userObject = null;
  /** Source list in which we inserted this timer */
  private List sourceList = null;
  /** Allocated element flag */
  private boolean allocated = false;
  
  /**
    Initialize our list of active timers and start our clock tick.
  */
  static {
    activeTimers = new ArrayList();
    for (int i = 0; i < MaxDuration; i++) {
      activeTimers.add(i, new ArrayList());
    }
    NxTimer refTimer = new NxTimer();
    Timer timer = new Timer();
    timer.schedule(refTimer, 1000, 1000);
  }
}
