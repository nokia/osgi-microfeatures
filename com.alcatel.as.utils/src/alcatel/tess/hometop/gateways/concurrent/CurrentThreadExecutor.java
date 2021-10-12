// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.concurrent;

// Utils
import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

@Deprecated
public class CurrentThreadExecutor implements Executor {
  private final PlatformExecutor _currentExecutor;
  
  public CurrentThreadExecutor() {
    _currentExecutor = PlatformExecutors.getInstance().getCurrentThreadContext().getCurrentExecutor();
  }
  
  public void execute(Runnable task) {
    _currentExecutor.execute(task);
  }
}
