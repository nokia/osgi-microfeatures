package alcatel.tess.hometop.gateways.reactor.impl;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.util.serviceloader.ServiceLoader;

public class ReactorProviderImplStandalone extends ReactorProviderImpl {
  public ReactorProviderImplStandalone() {
    _meteringService = ServiceLoader.getService(MeteringService.class);
    _strictTimerService = ServiceLoader.getService(TimerService.class, "(" + TimerService.STRICT + "=true)");
    _approxTimerService = ServiceLoader.getService(TimerService.class, "(" + TimerService.STRICT + "=false)");
    _executors = ServiceLoader.getService(PlatformExecutors.class);
    start();
  }
}
