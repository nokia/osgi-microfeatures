// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;


/**
 * This interface define the behavior of classes that must be allocated
 * with the <code>{@link alcatel.tess.hometop.gateways.utils.ObjectPool</code> class.
 *
 */
public interface Recyclable {
  /**
   * Recycle a object into the pool. This method is called by the 
   * ObjectPool when the application release this object with the 
   * <code>{@link alcatel.tess.hometop.gateways.utils.ObjectPool#release()]</code> 
   * method.
   */
  public void recycled();
  
  /**
   * Is this object in a valid State ? This method may be called at any
   * time by the ObjectPool class to check if this object is currently 
   * in a consistent state.
   *
   * @return true if This Recyclable object is in a consistent state, 
   *	or false if not.
   */
  public boolean isValid();
}
