// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.concurrent;

// Utils
import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.PlatformExecutors;

@Deprecated
public class ThreadPoolExecutor implements Executor {
  public void execute(Runnable task) {
    PlatformExecutors.getInstance().getThreadPoolExecutor().execute(task);
  }
}
