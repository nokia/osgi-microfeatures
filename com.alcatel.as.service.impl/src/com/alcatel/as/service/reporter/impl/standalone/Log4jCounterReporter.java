// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.reporter.impl.standalone;

import static com.alcatel.as.service.reporter.api.CommandScopes.APP_COUNTER_SCOPE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.service.command.CommandProcessor;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.Visibility;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;

import alcatel.tess.hometop.gateways.utils.Log;

/**
 * Scan all counters and logs them regularly. Thread safe because all events are
 * handled using a plateform queue executor.
 */
@Config(section = "Standalone log4j counter reporter")
@Component(provides = {})
@Property(name = "asr.component.parallel", value = "true") // Initialize the component in the processing threadpool
@Property(name = "asr.component.cpubound", value = "false") // use the IO blocking threadpool
@Property(name = "asr.component.queue", value = "Log4jCounterReporter")
public class Log4jCounterReporter implements Runnable {
	final Log _logger = Log.getLogger("com.alcatel.as.service.reporter.impl.standalone.Log4jCounterReporter");

	private volatile boolean _started;
	private final List<CounterInfo> _counters = new ArrayList(); // thread safe
	private volatile PlatformExecutor _queue;
	private volatile ScheduledFuture<?> _task;
	private Dictionary<String, ?> _conf;

	@IntProperty(title = "My Int Property", 
				 defval = -1, 
				 max = 100, 
				 helpPath = "META-INF/helps/MyProperty.html", 
				 dynamic = true, required = true, visibility = Visibility.HIDDEN)
	private final static String RATE = "counter.reportingRateSec";

	private class CounterInfo {
		final Object _instance;
		final Method[] _methods;
		final String _module;

		public CounterInfo(Object instance, Map<String, String> props) {
			_instance = instance;
			_methods = getAnnotatedCounters(instance);
			_module = props.get(ConfigConstants.MODULE_NAME);
			if (_logger.isDebugEnabled()) {
				_logger.debug("new CounterInfo %s, %s, methods=%s", _instance, props, Arrays.toString(_methods));
			}
		}

		public String getModule() {
			return _module;
		}

		public int[] getCounters() {
			int[] res = new int[_methods.length];
			try {
				for (int i = 0; i < _methods.length; i++) {
					res[i] = (Integer) _methods[i].invoke(_instance, (Object[]) null);
				}
			} catch (Exception e) {
				_logger.warn("getCounters failed", e);
			}
			return res;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Method[] getAnnotatedCounters(Object instance) {
			List<Method> list = new ArrayList<Method>();

			Class[] annotations = new Class[] { Counter.class, Gauge.class };
			for (Method m : instance.getClass().getMethods()) {
				Annotation a;
				try {
					for (Class c : annotations)
						if ((a = m.getAnnotation(c)) != null) {
							if (a instanceof Counter || a instanceof Gauge) {
								list.add(m);
								break;
							}
						}
				} catch (Throwable t) {
					_logger.warn("annotation error in " + instance + "." + m, t);
				}
			}

			return list.toArray(new Method[list.size()]);
		}
	}

	// thread safe
	@ConfigurationDependency(pidClass = Log4jCounterReporter.class)
	void updated(Dictionary<String, Object> conf) {
		_conf = conf;
		_logger.debug("got configuration: " + _conf);

		if (_counters.size() > 0) {
			// reschedule task using new configuration.
			cancelTask();
			scheduleTask(); 
		}
	}

	// thread safe
	@ServiceDependency
	void bindPlatformExecutors(PlatformExecutors execs) {
		// Get our current thread queue executor
		_queue = execs.getCurrentThreadContext().getCurrentExecutor();
	}

	// thread safe
	@Start
	public void start() {
		_logger.debug("Starting standalone log4j counter reporter");
	}

	// thread safe
	@Stop
	void stop() {
		cancelTask();
	}

	// thread safe, called after start because we are using an optional dependency callback
	@ServiceDependency(service = Object.class, filter = "(&(objectclass=*)(" + CommandProcessor.COMMAND_SCOPE + "=" + APP_COUNTER_SCOPE + "))", required = false)			
	void addCounterHandler(Object instance, Map<String, String> serviceProperties) {
		if (serviceProperties.get(ConfigConstants.MODULE_NAME) == null) {
			_logger.debug("Ignored counter: %s : no MODULE NAME from service properties: %s", instance, serviceProperties);
			return;
		}
		_logger.debug("Registered counter info: %s,%s", instance, serviceProperties);
		_counters.add(new CounterInfo(instance, serviceProperties));
		
		// schedule a periodic task used to dump logs periodically
		scheduleTask();
	}
	
	private void cancelTask() {
		if (_task != null) {
			_task.cancel(false);
			_task = null;
		}
	}

	private void scheduleTask() {
		if (_task == null) {
			int rate = ConfigHelper.getInt(_conf, RATE, -1);
			if (rate != -1) {
				_logger.debug("Scheduling reporting task with rate: %d", rate);
				_task = _queue.scheduleWithFixedDelay(this, rate, rate, TimeUnit.SECONDS);
			}
		}		
	}

	@Override
	public void run() {
		for (CounterInfo info : _counters) {
			StringBuilder sb = new StringBuilder();
			sb.append(info.getModule()).append(": ");
			for (Method m : info._methods) {
				try {
					Integer value = (Integer) m.invoke(info._instance, (Object[]) null);
					sb.append(m.getName().startsWith("get") ? m.getName().substring(3) : m.getName()).append("=")
							.append(value);
					sb.append(";");
				} catch (Throwable t) {
					_logger.warn("exception while getting counter value for module %s, method=%s", t, info.getModule(),
							m.getName());
				}
			}
			sb.setLength(sb.length() - 1);
			_logger.info(sb.toString());
		}
	}
}
