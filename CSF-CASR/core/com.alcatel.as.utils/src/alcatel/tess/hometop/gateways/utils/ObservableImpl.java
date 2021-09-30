package alcatel.tess.hometop.gateways.utils;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is a rewrite of the java.util.Observable class.
 * This class implements the javaworld article that may be found in 
 * http://www.javaworld.com/javaworld/jw-02-2000/jw-02-2000/jw-02.fast_p.html
 * and should give better performance than the standard java.util.Observable 
 * class (no clone operation and no thread synchronization are performed in
 * crucial methods notification).
 * 
 */
public class ObservableImpl extends Observable {
  private volatile boolean changed = false;
  private volatile Observer[] _observers;
  private final ReentrantLock _lock;
  
  /**
   * Constructs a new Watchable object.
   */
  public ObservableImpl() {
    this(true);
  }
  
  public ObservableImpl(boolean threadSafe) {
    _observers = new Observer[0];
    _lock = threadSafe ? new ReentrantLock() : null;
  }
  
  /**
   * Adds a observer to the set of Observers for this object. 
   * @param w a observer to be added.
   */
  public void addObserver(Observer w) {
    try {
      lockme();
      int length = _observers.length;
      Observer observers[] = new Observer[length + 1];
      System.arraycopy(_observers, 0, observers, 0, length);
      observers[length] = w;
      _observers = observers;
      observers = null;
    }
    
    finally {
      unlockme();
    }
  }
  
  /**
   * Deletes a observer from the set of Observers of this object. 
   *
   * @param obs the Observer to be deleted.
   */
  public void deleteObserver(Observer w) {
    Observer found = null;
    int i = 0;
    
    if (w == null) {
      return;
    }
    
    try {
      lockme();
      int length = _observers.length;
      
      for (i = 0; i < length; i++) {
        if (((Object) w).equals(_observers[i])) {
          found = _observers[i];
          break;
        }
      }
      
      if (found == null) {
        return;
      }
      
      Observer observers[] = new Observer[length - 1];
      System.arraycopy(_observers, 0, observers, 0, i);
      System.arraycopy(_observers, i + 1, observers, i, observers.length - i);
      
      _observers = observers;
      observers = null;
    }
    
    finally {
      unlockme();
    }
  }
  
  /**
   * Remove all Observers.
   */
  public void deleteObservers() {
    try {
      lockme();
      _observers = new Observer[0];
    } finally {
      unlockme();
    }
  }
  
  /**
   * Notify all of our Observers.<p>
   * Each Observer has its <code>update</code> method called with two
   * arguments: this Watchable object and <code>arg</code>. 
   * Please notice that, unlike the java.util.Observable class, this method does
   * not need neither to synchronize nor to clone internal list of observers ...
   *
   * @param arg the argument given to Observers
   */
  public void notifyObservers(Object arg) {
    if (!changed)
      return;
    
    clearChanged();
    Observer observers[] = _observers;
    for (int i = 0; i < observers.length; i++) {
      observers[i].update(this, arg);
    }
  }
  
  /**
   * Returns the number of observers of this <tt>Observable</tt> object.
   *
   * @return  the number of observers of this object.
   */
  public int countObservers() {
    try {
      lockme();
      Observer observers[] = _observers;
      return observers.length;
    } finally {
      unlockme();
    }
  }
  
  public boolean hasChanged() {
    return changed;
  }
  
  protected void setChanged() {
    changed = true;
  }
  
  protected void clearChanged() {
    changed = false;
  }
  
  protected void lockme() {
    if (_lock != null) {
      _lock.lock();
    }
  }
  
  protected void unlockme() {
    if (_lock != null) {
      _lock.unlock();
    }
  }
}
