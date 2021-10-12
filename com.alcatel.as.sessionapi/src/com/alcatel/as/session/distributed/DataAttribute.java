// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public interface DataAttribute extends Serializable
{
  /**
   * The object implements the readData method to restore its contents 
   * by calling the methods of DataInput. 
   * The readData method must read the values in the same sequence 
   * and with the same types as were written by writeData.
   * 
   * @param in - the stream to read data from in order to restore the object
   * @param version - the version returned by writeData 
   * (the version can be used to manage compatibility during an upgrade)
   * @throws IOException if I/O errors occur
   * @throws ClassNotFoundException If the class of the restored object cannot be found
   */
  public void readData(DataInput in, int version) throws IOException;
  
  /**
   * The object implements the writeData method to save its contents 
   * by calling the methods of DataOutput
   * 
   * @param out - the stream to write the object to
   * @return the version number
   * @throws IOException Includes any I/O exceptions that may occur
   */
  public int writeData(DataOutput out) throws IOException;

}
