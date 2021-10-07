package alcatel.tess.hometop.gateways.utils;

import java.io.Serializable;

/**
 * Capture a snapshot of an object's state so that the object's state can be restored later.
 * The object that initiates the capture or restoration of the state does not need to know 
 * anything about the state information. 
 * It only needs to know that the object whose state it is restoring or capturing implements 
 * a particular interface.
 */
public interface Snapshot extends Serializable {
  /**
   * Take a snapshot of the current state. 
   * @return null if no state can be currently stored, or the current state.
   */
  Object takeSnapshot();
  
  /**
   * Restore a snapshot into the current environment.
   * @param state A state previously taken using the takeSnapshot() method.
   */
  void restoreSnapshot(Object state);
}
