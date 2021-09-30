package alcatel.tess.hometop.gateways.concurrent;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.utils.Hashtable;

public class ThreadContext {
  public static synchronized void addInheriableThreadLocal(Object key) {
    Object[] old = _inheritableThreadLocals;
    int currSize = old.length;
    Object[] tab = new Object[currSize + 1];
    System.arraycopy(old, 0, tab, 0, currSize);
    tab[currSize] = key;
    _inheritableThreadLocals = tab;
  }
  
  public static Object[] getInheritableThreadLocals() {
    return _inheritableThreadLocals;
  }
  
  public static Hashtable getContext() {
    return ((Hashtable) myThreadLocal.get());
  }
  
  public static void reset() {
    Hashtable workerCtx = getContext();
    workerCtx.clear();
  }
  
  private static ThreadLocal myThreadLocal = new ThreadLocal() {
    protected synchronized Object initialValue() {
      return new WrappedHashtable(4);
    }
  };
  
  private static Object[] _inheritableThreadLocals;
  
  /**
   * This hashtable is used for compatibility reasons, in order to be
   * able to get the current thread reactor.
   */
  private static class WrappedHashtable extends Hashtable {
    public WrappedHashtable(int i) {
      super(i);
    }
    
    public Object get(Object key) {
      if (key.equals(Reactor.class)) {
        return ReactorProvider.provider().getCurrentThreadReactor();
      } else {
        return super.get(key);
      }
    }
  }
}
