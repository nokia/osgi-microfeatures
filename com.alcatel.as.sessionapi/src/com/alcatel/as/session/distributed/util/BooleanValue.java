// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.util;

import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps a Boolean.
 */
public abstract class BooleanValue implements AttributeValue {

  /**
   * Returns an instance of BooleanValue.
   * @param value the wrapped boolean
   * @return TrueValue.INSTANCE or FalseValue.INSTANCE
   */
  public static BooleanValue getInstance (boolean value){
    if (value)
      return TrueValue.INSTANCE;
    else
      return FalseValue.INSTANCE;
  }

  /**
   * Constructor for subclasses.
   */
  protected BooleanValue (){
  }

  /**
   * Returns the boolean value.
   * @return the boolean value.
   */
  public abstract boolean booleanValue ();

  public void writeExternal (ObjectOutput out) {
  }

  public void readExternal (ObjectInput in) {
  }

  /**
   * Returns a String representation.
   * @return the String representation.
   */
  public abstract String toString ();
}
