package alcatel.tess.hometop.gateways.osgi;

import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.MeteringService;
import com.nokia.as.service.tlsexport.TlsExportService;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.impl.ReactorProviderImpl;
import alcatel.tess.hometop.gateways.reactor.impl.TLSLChannelListenerFactoryImpl;
import alcatel.tess.hometop.gateways.reactor.spi.ChannelListenerFactory;

public class Activator extends DependencyActivatorBase {
  final static String LOGROTATE_EVENT = "com/alcatel_lucent/as/service/agent/logging/LOGROTATE_EVENT";
  private volatile BundleContext _ctx;
  
  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    _ctx = ctx;
    //
    // Reactor service
    //
    Hashtable<String, Object> props = new Hashtable<>();
    props.put(Constants.SERVICE_RANKING, new Integer(10));
    props.put("type", "nio");
    
    dm.add(createComponent()
        .setInterface(ReactorProvider.class.getName(), props)
        .setImplementation(ReactorProviderImpl.class)
        .add(createServiceDependency().setService(TlsExportService.class).setRequired(false))
        .add(createServiceDependency().setService(MeteringService.class)
             .setRequired(true))
        .add(createServiceDependency().setService(TimerService.class, "(" + TimerService.STRICT + "=true)")
             .setRequired(true).setAutoConfig("_strictTimerService"))
        .add(createServiceDependency().setService(PlatformExecutors.class)
             .setRequired(true).setAutoConfig("_executors"))
        .add(createServiceDependency().setService(TimerService.class, "(" + TimerService.STRICT + "=false)")
             .setRequired(true).setAutoConfig("_approxTimerService")));
    
    //
    // TLS Channel Listener Filter
    //
    props = new Hashtable<>();
    props.put("type", "tls");

    dm.add(createComponent()
            .setInterface(ChannelListenerFactory.class.getName(), props)
            .setImplementation(TLSLChannelListenerFactoryImpl.class)
            .add(createServiceDependency().setService(ReactorProvider.class, "(type=nio)").setRequired(true)));
    
    // 
    // The ThreadPool.
    //
    dm.add(createComponent()
        .setImplementation(this)
        .add(createServiceDependency()
             .setService(PlatformExecutors.class).setCallbacks("bindPlatformExecutors", null)
             .setAutoConfig(false).setRequired(true)));
  }
  
  @Override
  public void destroy(BundleContext context, DependencyManager manager) throws Exception {
  }
    
  protected void bindPlatformExecutors(PlatformExecutors execs) {
	ThreadPool.bind(execs);
    _ctx.registerService(ThreadPool.class.getName(), ThreadPool.getInstance(), null);
  }
}
