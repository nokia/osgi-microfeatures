package alcatel.tess.hometop.gateways.reactor.impl;

import java.nio.channels.SelectionKey;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Channel;

/**
 * Class allowing to enable/disable read interest on a given selection key.
 * By default, read interest is considered to be
 * 
 *   - disabled internally by default
 *   - enabled (by user) by default 
 */
public class ReadInterestController {
  /**
   * Socket selection key, used to enable/disable read interest.
   */
  private final SelectionKey _key;
  
  /**
   * Flag set to true if the reactor impl has requested to disable read interest.
   * When true, it means that the user can't enable read interest.
   */
  private boolean _disabledInternal = true;
  
  /**
   * Flag set to true if the user has requested to disable read interest.
   * User can't enable read interest if the reactor impl has previously requested to disable
   * read interest.
   */
  private boolean _disabledUser = false;
  
  /** 
   * lock internally used for thread safety. 
   **/
  private final ReentrantLock _lock = new ReentrantLock();
  
  /**
   * Logger used
   */
  private final Logger _logger;
  
  /**
   * Listener called when read interest is enabled or disabled.
   */
  private final Consumer<Boolean> _listener;
            
  /**
   * Task scheduled in the selector thread in order to enable read interest.
   * We must do that in the selector thread.
   */
  private final Runnable _enableReadAction = new Runnable() {
    public void run() {
      try {
        _key.interestOps(_key.interestOps() | SelectionKey.OP_READ);
        _listener.accept(true);
      } catch (Throwable t) {
        _logger.info("enableRead failed (selection key cancelled or socket closed)", t);
        Object attached = _key.attachment();
        if (attached instanceof Channel) {
         ((Channel) attached).shutdown();
        }
      }
    }
  };
  
  /**
   * Task scheduled in the selector thread in order to disable read interest.
   * We must do that in the selector thread.
   */
  private final Runnable _disableReadAction = new Runnable() {
    public void run() {
      try {
        _key.interestOps(_key.interestOps() & ~SelectionKey.OP_READ);
        _listener.accept(false);
      } catch (Throwable t) {
        // the key is probably cancelled, or the socket has been closed
        _logger.info("disabledRead failed (selection key cancelled or socket closed)", t);        
        Object attached = _key.attachment();
        if (attached instanceof Channel) {
         ((Channel) attached).shutdown();
        }
      }
    }
  };

  /**
   * Select Key read interest controller. you can enable/disable read interests using this controller.
   * @param key  the selection key that is enabled in read mode when this conroller enables read mode
   * @param logger logger used
   * @param listener a callback invoked when read interest is enabled or not.
   */
  ReadInterestController(SelectionKey key, Logger logger, Consumer<Boolean> listener) {
    _key = key;
    _logger = logger;
    _listener = listener;
  }
  
  void disableReadingInternal(NioSelector selector) {
    _lock.lock();
    try {
      if (!_disabledInternal) {
        _disabledInternal = true;
        selector.scheduleNow(_disableReadAction);
      }
    } finally {
      _lock.unlock();
    }
  }
  
  void enableReadingInternal(NioSelector selector) {
    _lock.lock();
    try {
      if (_disabledInternal) {
        _disabledInternal = false;
        if (!_disabledUser) {
          selector.scheduleNow(_enableReadAction);
        }
      }
    } finally {
      _lock.unlock();
    }
  }
  
  void disableReading(NioSelector selector) {
    _lock.lock();
    try {
      if (!_disabledUser) {
        _disabledUser = true;
        if (!_disabledInternal) {
          selector.scheduleNow(_disableReadAction);
        }
      }
    } finally {
      _lock.unlock();
    }
  }
  
  void enableReading(NioSelector selector) {
    _lock.lock();
    try {
      if (_disabledUser) {
        _disabledUser = false;
        if (!_disabledInternal) {
          selector.scheduleNow(_enableReadAction);
        }
      }
    } finally {
      _lock.unlock();
    }
  }
  
}
