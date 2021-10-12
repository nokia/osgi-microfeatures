// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.proxylet.deployer.impl;

import com.nextenso.proxylet.admin.Bearer;

public interface BearerProvider {
  public Bearer readDeployedBearerContext() throws Exception ;
}
