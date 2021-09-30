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
