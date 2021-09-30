package com.alcatel.as.proxylet.deployer.impl;

import com.nextenso.proxylet.admin.Bearer;

public interface BearerProvider {
  public Bearer readDeployedBearerContext() throws Exception ;
}
