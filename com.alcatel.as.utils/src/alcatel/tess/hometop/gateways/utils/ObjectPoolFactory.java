// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;


/**
 * The interface <code>ObjectPoolFactory</code> is meant to be used by
 * the <code>ObjectPool</code> class when fresh new objects needs to be
 * created.
 *
 */
public interface ObjectPoolFactory {
  
  /**
   * The <code>newInstance</code> method is called by the {@link ObjectPooo#acquire()}
   * method when no objects are available from the pool.
   *
   * @return a <code>Recyclable</code> value
   */
  public Recyclable newInstance();
}
