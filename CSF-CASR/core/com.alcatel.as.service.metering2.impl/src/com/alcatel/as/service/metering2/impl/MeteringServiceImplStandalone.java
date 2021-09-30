package com.alcatel.as.service.metering2.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.concurrent.Executor;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.StopWatch;
import com.alcatel.as.service.metering2.ValueSupplier;

/**
 * Metering Service implementation used when running outside OSGi. 
 * When running outside osgi, the MeteringService service is implemented as a null object.
 */
public class MeteringServiceImplStandalone implements MeteringService {
  private final StopWatch _nullWatch = new StopWatch() {
    @Override
    public void close() {
    }
  };
  
  public MeteringServiceImplStandalone() {
    this(null);
  }
  
  public MeteringServiceImplStandalone(Dictionary<?,?> conf) {
  }

  public Meter createAbsoluteMeter(final String name) {
    return createNullMeter(name, Meter.Type.ABSOLUTE);
  }

  public Meter createIncrementalMeter(String name, Meter parent) {
    return createNullMeter(name, Meter.Type.INCREMENTAL);
  }

  public Meter createValueSuppliedMeter(String name, ValueSupplier valueSupplier) {
    return createNullMeter(name, Meter.Type.SUPPLIED);
  }

  @Override
  public Monitorable getMonitorable(String name) {
    return null;
  }
  
  private Meter createNullMeter(final String name, final Meter.Type type) {
    return new Meter() {
      @Override
      public long getValue() {
        return 0;
      }

      @Override
      public Type getType() {
        return type;
      }

      @Override
      public Meter set(long value) {
        return this;
      }

      @Override
      public long getAndSet(long value) {
        return 0;
      }

      @Override
      public long getAndReset() {
        return 0;
      }

      @Override
      public Meter inc(long delta) {
        return this;
      }

      @Override
      public Meter dec(long delta) {
        return this;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public boolean hasJobs() {
        return false;
      }

      @Override
      public void updated() {
      }

      @Override
      public MonitoringJob startJob(MeterListener<?> listener, Object context, Executor executor) {
        return createNullJob(listener, context, executor, this);
      }

      @Override
      public MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor executor,
                                             long schedule, int reportCount) {
        return createNullJob(listener, context, executor, this);
      }

      @Override
      public StopWatch startWatch(boolean force) {
        return _nullWatch;
      }

      @Override
      public Collection<MonitoringJob> getJobs() {
        return Collections.emptyList();
      }

      @Override
      public void stopAllJobs() {        
      }
      
      @Override
      public <T> T attach (T x){
	return null;
      }
      
      @Override
      public <T> T attachment (){
	return null;
      }
    };        
  }
  
  private MonitoringJob createNullJob(MeterListener<?> listener, final Object context, final Executor exec, final Meter m) {
    return new MonitoringJob() {
      @Override
      public boolean isRunning() {
        return false;
      }

      @Override
      public void stop() {
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> T getContext() {
        return (T) context;
      }

      @Override
      public Meter getMeter() {
        return m;
      }

      @Override
      public Executor getExecutor() {
        return exec;
      }
    };
  }
}
