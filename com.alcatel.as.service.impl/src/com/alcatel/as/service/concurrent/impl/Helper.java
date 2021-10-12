// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeterListener;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.MonitoringJob;
import com.alcatel.as.service.metering2.StopWatch;
import com.alcatel.as.service.metering2.ValueSupplier;
import com.alcatel.as.service.metering2.Meter.Type;
import com.alcatel.as.util.serviceloader.ServiceLoader;

class Helper {
  /** The PlatformExecutor service singleton */
  private static PlatformExecutorsImpl _pfExecutors;
  
  /** Standalone version of Meters */
  private static Meters _standaloneMeters;

  /**
   * Executor used to invoke runnables from the caller thread.
   */
  static Executor inlineExecutor = command -> command.run();
  
  static void bind(PlatformExecutorsImpl pfExecs) {
    _pfExecutors = pfExecs;
  }
  
  static PlatformExecutorsImpl getPlatformExecutors() {
    return _pfExecutors;
  }
    
  static ClassLoader getTCCL() {
    if (_pfExecutors.useTCCL()) {
      return Thread.currentThread().getContextClassLoader();
    }
    return null;
  }
  
  static PlatformExecutor getRootExecutor(PlatformExecutor defExec) {
    PlatformExecutor root = ((ThreadContextImpl) _pfExecutors.getCurrentThreadContext()).getRootExecutor();
    return root != null ? root : defExec;
  }
  
  static void runTask(Runnable task, ClassLoader cl, PlatformExecutor current, PlatformExecutor root) {
    if (cl != null) {
      Thread.currentThread().setContextClassLoader(cl);
    }
    ThreadContextImpl ctx = (ThreadContextImpl) _pfExecutors.getCurrentThreadContext();
    ctx.setRootExecutor(root);
    ctx.setCurrentThreadExecutor(current);
    task.run();
  }
  
  static <V> V runCallable(Callable<V> callable, ClassLoader cl, PlatformExecutor current,
                           PlatformExecutor root) throws Exception {
    if (cl != null) {
      Thread.currentThread().setContextClassLoader(cl);
    }
    ThreadContextImpl ctx = (ThreadContextImpl) _pfExecutors.getCurrentThreadContext();
    ctx.setRootExecutor(root);
    ctx.setCurrentThreadExecutor(current);
    return callable.call();
  }
  
  public static synchronized Meters getStandaloneMeters() {
    if (_standaloneMeters == null) {
      MeteringService ms = mockMeteringService();
      _standaloneMeters = new Meters();
      _standaloneMeters.bindMeteringService(ms);      
      _standaloneMeters.start(null);
    }
    return _standaloneMeters;
  }
  
	public static MeteringService mockMeteringService() {
		return new MeteringService() {

			@Override
			public Meter createAbsoluteMeter(String name) {
				return new Meter() {
					@Override
					public long getValue() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Type getType() {
						// TODO Auto-generated method stub
						return Type.ABSOLUTE;
					}

					@Override
					public Meter set(long value) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public long getAndSet(long value) {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public long getAndReset() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Meter inc(long delta) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Meter dec(long delta) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public String getName() {
						return name;
					}

					@Override
					public boolean hasJobs() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void updated() {
						// TODO Auto-generated method stub

					}

					@Override
					public MonitoringJob startJob(MeterListener<?> listener, Object context, Executor executor) {
						return null;
					}

					@Override
					public MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor executor,
							long schedule, int reportCount) {
						return null;
					}

					@Override
					public StopWatch startWatch(boolean force) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Collection<MonitoringJob> getJobs() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void stopAllJobs() {
						// TODO Auto-generated method stub

					}

					@Override
					public <T> T attach(T x) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public <T> T attachment() {
						// TODO Auto-generated method stub
						return null;
					}

				};
			}

			@Override
			public Meter createIncrementalMeter(String name, Meter parent) {
				return new Meter() {

					volatile Object _attach;

					@Override
					public long getValue() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Type getType() {
						return Type.INCREMENTAL;
					}

					@Override
					public Meter set(long value) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public long getAndSet(long value) {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public long getAndReset() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Meter inc(long delta) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Meter dec(long delta) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public String getName() {
						// TODO Auto-generated method stub
						return name;
					}

					@Override
					public boolean hasJobs() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void updated() {
						// TODO Auto-generated method stub

					}

					@Override
					public MonitoringJob startJob(MeterListener<?> listener, Object context, Executor executor) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor executor,
							long schedule, int reportCount) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public StopWatch startWatch(boolean force) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Collection<MonitoringJob> getJobs() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void stopAllJobs() {
						// TODO Auto-generated method stub

					}

					@Override
					public <T> T attach(T x) {
						_attach = x;
						return x;
					}

					@Override
					public <T> T attachment() {
						return (T) _attach;
					}
				};
			}

			@Override
			public Meter createValueSuppliedMeter(String name, ValueSupplier valueSupplier) {
				return new Meter() {

					@Override
					public long getValue() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Type getType() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Meter set(long value) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public long getAndSet(long value) {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public long getAndReset() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Meter inc(long delta) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Meter dec(long delta) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public String getName() {
						// TODO Auto-generated method stub
						return name;
					}

					@Override
					public boolean hasJobs() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void updated() {
						// TODO Auto-generated method stub

					}

					@Override
					public MonitoringJob startJob(MeterListener<?> listener, Object context, Executor executor) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public MonitoringJob startScheduledJob(MeterListener<?> listener, Object context, Executor executor,
							long schedule, int reportCount) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public StopWatch startWatch(boolean force) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Collection<MonitoringJob> getJobs() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void stopAllJobs() {
						// TODO Auto-generated method stub

					}

					@Override
					public <T> T attach(T x) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public <T> T attachment() {
						// TODO Auto-generated method stub
						return null;
					}

				};
			}

			@Override
			public Monitorable getMonitorable(String name) {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}
  
}
