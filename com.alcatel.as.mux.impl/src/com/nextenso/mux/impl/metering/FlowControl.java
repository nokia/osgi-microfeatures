package com.nextenso.mux.impl.metering;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.ValueSupplier;
import com.alcatel.as.service.metering2.util.Meters;
import com.alcatel.as.service.metering2.util.ThresholdListener;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.impl.ioh.AgentSideMuxConnection;

@Component(immediate = true)
public class FlowControl {
  private final static Logger _logger = Logger.getLogger("as.service.flowcontrol2");
  private final String MEMPOOLMATCH = "Old";
  private final String SYSTEM = "system.";
  private final String SYSTEM_MEM = SYSTEM + "memory";
  private final String SYSTEM_CPU = SYSTEM + "cpu";
  public final String SYSTEM_MEM_USED = SYSTEM_MEM + ".used";
  public final String SYSTEM_MEM_FGC = SYSTEM_MEM + ".fgc.count";
  public final String SYSTEM_MEM_FGCT = SYSTEM_MEM + ".fgc.duration";
  private MeteringService _metering;
  int _delay = 500;
  int _threshold = 85;
  private ThresholdListener<List<MuxConnection>> _controller;
  private PlatformExecutor _executor;
  private List<MuxConnection> _ctx = Collections.synchronizedList(new LinkedList<MuxConnection>());
  List<MonitoringJob> _jobs;
  
  @Reference
  void bindMetering(MeteringService service) {
    _metering = service;
  }
  
  @Reference
  void bindExecutors(PlatformExecutors executors) {
    _executor = executors.createProcessingThreadPoolExecutor("flowcontrol", "flowcontrol", 1);
  }
  
  void unbindConnection(MuxConnection cx) {
    _ctx.remove(cx);
    _logger.info("unbindConnection  " + _ctx.size());
  }
  
  ThresholdListener<List<MuxConnection>> basicController() {
    final long timeout = 3000;
    return new ThresholdListener<List<MuxConnection>>() {
      @Override
      public List<MuxConnection> above(long threshold, Meter meter, List<MuxConnection> ctx) {
        _logger.warn(meter.getName() + ">" + meter.getValue());
        if (ctx.isEmpty() || !(ctx.get(0) instanceof AgentSideMuxConnection)) {
          return ctx;
        }
        _logger.warn("disabling..." + ctx.size());
        for (MuxConnection cx : ctx) {
          TcpChannel channel = ((AgentSideMuxConnection) cx).getChannel();
          if (channel.isClosed()) {
            _logger.warn("closing..." + channel);
            ctx.remove(channel);
          } else {
            channel.disableReading();
            channel.setSoTimeout(0);
          }
        }
        _logger.warn("disabled ." + ctx.size());
        return ctx;
      }
      
      @Override
      public List<MuxConnection> below(long threshold, Meter meter, List<MuxConnection> ctx) {
        _logger.warn(meter.getName() + "<" + meter.getValue());
        if (ctx.isEmpty() || !(ctx.get(0) instanceof AgentSideMuxConnection)) {
          return ctx;
        }
        _logger.warn("enabling..." + ctx.size());
        for (MuxConnection cx : ctx) {
          TcpChannel channel = ((AgentSideMuxConnection) cx).getChannel();
          if (channel.isClosed()) {
            _logger.warn("closing..." + channel);
            ctx.remove(channel);
          } else {
            channel.enableReading();
            channel.setSoTimeout(timeout);
          }
        }
        _logger.warn("enabled ." + ctx.size());
        return ctx;
      }
      
    };
  }
  
  ThresholdListener<List<MuxConnection>> fakeController() {
    return new ThresholdListener<List<MuxConnection>>() {
      
      @Override
      public List<MuxConnection> above(long threshold, Meter meter, List<MuxConnection> ctx) {
        _logger.warn(meter.getName() + ">" + meter.getValue());
        return ctx;
      }
      
      @Override
      public List<MuxConnection> below(long threshold, Meter meter, List<MuxConnection> ctx) {
        _logger.warn(meter.getName() + "<" + meter.getValue());
        return ctx;
      }
      
    };
  }
  
  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, unbind = "unbindConnection")
  void bindConnection(MuxConnection cx) {
    if (_controller == null) {
      if (cx instanceof AgentSideMuxConnection) {
        _controller = basicController();
      } else {
        _controller = fakeController();
      }
    }
    _ctx.add(cx);
    _logger.info("bindConnection  " + _ctx.size());
  }
  
  public String toString() {
    return "with threshold=" + _threshold + "%,delay=" + _delay + "ms";
  }
  
  private MonitoringJob memoryWacher() {
    List<MemoryPoolMXBean> mempoolsmbeans = ManagementFactory.getMemoryPoolMXBeans();
    MemoryPoolMXBean mc = null;
    for (MemoryPoolMXBean m : mempoolsmbeans) {
      if (m.getName().contains(MEMPOOLMATCH)) {
        _logger.info(m.getName() + ":" + m.getUsage().getMax());
        mc = m;
      }
    }
    if (mc == null)
      throw new IllegalArgumentException("missing old memory");
    final MemoryPoolMXBean mbean = mc;
    long high = mbean.getUsage().getMax() * _threshold / 100;
    MeterListener<List<MuxConnection>> listener = Meters.newDelayedHighLowWatermarksListener(high, high, _delay,
                                                                                             _delay, _executor,
                                                                                             _controller);
    // TODO : find the executor for each connection ??
    _logger.info("watching over " + high / 1024 + "Ko");
    return _metering.createValueSuppliedMeter(SYSTEM_MEM_USED, new ValueSupplier() {
      
      @Override
      public long getValue() {
        return mbean.getUsage().getUsed();
      }
      
    }).startScheduledJob(listener, _ctx, _executor, _delay, 0);
    
  }
  
  private MonitoringJob cpuWatcher() {
    final OperatingSystemMXBean opmbean = ManagementFactory.getOperatingSystemMXBean();
    Meter cpuMeter = _metering.createValueSuppliedMeter(SYSTEM_CPU, new ValueSupplier() {
      
      @Override
      public long getValue() {
        return (long) opmbean.getSystemLoadAverage();
      }
      
    });
    MeterListener<List<MuxConnection>> listener = Meters.newDelayedHighLowWatermarksListener(_threshold, _threshold,
                                                                                             _delay, _delay, _executor,
                                                                                             _controller);
    // TODO : find the executor for each connection ??
    _logger.info("watching over " + _threshold + "% of load");
    return cpuMeter.startScheduledJob(listener, _ctx, _executor, _delay, 0);
    
  }
  
  @Activate
  void start() {
    _logger.info("starting... ");
    _jobs = new ArrayList<MonitoringJob>();
    _jobs.add(cpuWatcher());
    _jobs.add(memoryWacher());
    _logger.info("started :" + _jobs.size());
  }
  
  // TODO provide gogo command
  void stop() {
    for (MonitoringJob job : _jobs) {
      job.stop();
    }
  }
}
