// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

public interface ConfigListener {
  public void propertyChanged(Config cnf, String propertyNames[]) throws ConfigException;
}
