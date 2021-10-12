// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

// JDK imports
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class EmptyEnumeration implements Enumeration {
  /* **********
  * Constants *
  *************/
  public final static EmptyEnumeration INSTANCE = new EmptyEnumeration();
  
  /* *********************
   * Implements Iterator *
   ***********************/
  public boolean hasMoreElements() {
    return false;
  }
  
  public Object nextElement() throws NoSuchElementException {
    throw new NoSuchElementException();
  }
}
