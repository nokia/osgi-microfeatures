// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

// Utils

/**
 * This is the base class for executors.
 * Mainly, this class load from the classpath some snapshot
 * managers used to take/restore states from the current
 * running environment.
 */
public class SnapshotManager {
  public final static SnapshotManager instance = new SnapshotManager();
  
  public synchronized void addSnapshot(String c) throws Exception {
    if (_table.getObject(c) == null) {
      int currLength = _snapshots.length;
      Snapshot[] snapshots = new Snapshot[currLength + 1];
      snapshots[currLength] = (Snapshot) Class.forName(c).newInstance();
      _snapshots = snapshots;
      _table.putObject(c, c);
    }
  }
  
  public Object[] takeSnapshots() {
    Snapshot[] snapshots = _snapshots;
    if (snapshots.length == 0) {
      return null;
    }
    
    Object[] states = new Object[snapshots.length];
    
    for (int i = 0; i < snapshots.length; i++) {
      states[i] = snapshots[i].takeSnapshot();
    }
    return states;
  }
  
  public void restoreSnapshots(Object[] states) {
    Snapshot[] snapshots = _snapshots;
    
    if (states != null && snapshots.length != states.length) {
      throw new RuntimeException("invalid parameters");
    }
    
    for (int i = 0; i < snapshots.length; i++) {
      if (states[i] != null) {
        snapshots[i].restoreSnapshot(states[i]);
      }
    }
  }
  
  private SnapshotManager() {
  }
  
  private Snapshot[] _snapshots = new Snapshot[0];
  private StringCaseHashtable _table = new StringCaseHashtable();
}
