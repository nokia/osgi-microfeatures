// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;
import java.util.ArrayList;

public interface Serializer {
  
  public void init(Config cnf) throws ConfigException;
  
  public int deserialize(String key, ArrayList output) throws IOException;
  
  public void serialize(String key, ArrayList data) throws IOException;
  
  public void close();
}
