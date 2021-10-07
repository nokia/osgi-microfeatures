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
