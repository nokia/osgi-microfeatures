package com.nokia.as.osgi.osgilog2log4j;

import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.lang.reflect.Array;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

@Component(provides = {})
public class OsgiLogRedirector implements BundleListener, FrameworkListener, ServiceListener, LogListener {

	private final static Logger _logger = Logger.getLogger("osgi");
	private final static Logger _bndLogger = Logger.getLogger("osgi.event.bundle");
	private final static Logger _fwkLogger = Logger.getLogger("osgi.event.fwk");
	private final static Logger _srvLogger = Logger.getLogger("osgi.event.service");

	@Inject
	private BundleContext _bctx;

	@ServiceDependency
	private LogReaderService _logReader;

	@Start
	protected void start() {
		_bctx.addBundleListener(this);
		_bctx.addFrameworkListener(this);
		_bctx.addServiceListener(this);
		Enumeration<LogEntry> logs = _logReader.getLog();
		if (logs.hasMoreElements()) {
		        _logger.info("redirecting previous osgi logs to log4j"); 
			Collections.list(logs).stream().collect(Collectors.toCollection(LinkedList::new)).descendingIterator().forEachRemaining(entry -> {					
				Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss:S");
				Level level = getLog4jLevel(entry.getLevel());
				if (_logger.isEnabledFor(level)) {
					StringBuilder sb = new StringBuilder();
					sb.append("[").append(format.format(entry.getTime())).append("] ")
					.append(entry.getMessage());
					log(entry.getBundle(), entry.getServiceReference(), entry.getLevel(), sb.toString(), entry.getException());							
				}
			});
		}

		_logReader.addLogListener(this);
	}

	@Stop
	void stop() {
		_bctx.removeBundleListener(this);
		_bctx.removeFrameworkListener(this);
		_bctx.removeServiceListener(this);
		_logReader.removeLogListener(this);
	}

	// ------------- BundleListener

	public void bundleChanged(final BundleEvent event) {
		if (_bndLogger.isInfoEnabled()) {
			int eventType = event.getType();
			String msg = getBundleEventMessage(eventType);

			if (msg != null) {
				_bndLogger.info(
						msg + ": " + event.getBundle().getSymbolicName() + "(" + getVersion(event.getBundle()) + ")");
			} else {
				_bndLogger.info(
						"Received unknown bundle event: " + event + " for bundle: " + toString(event.getBundle()));
			}
		}
	}

	// ------------- FrameworkListener

	public void frameworkEvent(final FrameworkEvent event) {
		int eventType = event.getType();
		String msg = getFrameworkEventMessage(eventType);
		Level level = (eventType == FrameworkEvent.ERROR) ? Level.ERROR : Level.INFO;
		if (msg != null) {
			_fwkLogger.log(level, msg, event.getThrowable());
		} else {
			_fwkLogger.log(level, "Unknown fwk event: " + event);
		}
	}

	// ------------- ServiceListener

	public void serviceChanged(final ServiceEvent event) {
		if (_srvLogger.isInfoEnabled()) {
			int eventType = event.getType();
			String msg = getServiceEventMessage(eventType);
			if (msg != null) {
				_srvLogger.info(msg + toString(event.getServiceReference().getBundle()) + "\n"
						+ getRefProperties(event.getServiceReference()));
			} else {
				_srvLogger.info("Received unknown serviceChanged event: " + event);
			}
		}
	}

	// ------------ LogListener

	@Override
	public void logged(LogEntry entry) {
		if (entry != null) {
			log(entry.getBundle(), entry.getServiceReference(), entry.getLevel(), entry.getMessage(),
					entry.getException());
		}
	}

	// private methods

	private void log(Bundle bundle, ServiceReference sr, int osgiLevel, String msg, Throwable exception) {
		Level level = getLog4jLevel(osgiLevel);
		if (_logger.isEnabledFor(level)) {
			if (sr != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("Bundle: ").append(bundle.getSymbolicName());
				if (sr != null) {
					sb.append(" - " + sr.toString());
				}
				sb.append(" - ");
				sb.append(msg);
				_logger.log(level, sb.toString(), exception);
			} else {
				_logger.log(level, msg, exception);
			}
		}
	}

	private Level getLog4jLevel(int osgiLevel) {
		switch (osgiLevel) {
		case LogService.LOG_WARNING:
			return Level.WARN;

		case LogService.LOG_INFO:
			return Level.INFO;

		case LogService.LOG_DEBUG:
			return Level.DEBUG;

		case LogService.LOG_ERROR:
		default:
			return Level.ERROR;
		}
	}

	private String getBundleEventMessage(int event) {
		switch (event) {
		case BundleEvent.INSTALLED:
			return "Bundle INSTALLED";
		case BundleEvent.LAZY_ACTIVATION:
			return "Bundle LAZY_ACTIVATION";
		case BundleEvent.RESOLVED:
			return "Bundle RESOLVED";
		case BundleEvent.STARTED:
			return "Bundle STARTED";
		case BundleEvent.STARTING:
			return "Bundle STARTING";
		case BundleEvent.STOPPED:
			return "Bundle STOPPED";
		case BundleEvent.STOPPING:
			return "Bundle STOPPING";
		case BundleEvent.UNINSTALLED:
			return "Bundle UNINSTALLED";
		case BundleEvent.UNRESOLVED:
			return "Bundle UNRESOLVED";
		case BundleEvent.UPDATED:
			return "Bundle UPDATED";
		default:
			return null;
		}
	}

	private String getFrameworkEventMessage(int event) {
		switch (event) {
		case FrameworkEvent.ERROR:
			return "FrameworkEvent: ERROR";
		case FrameworkEvent.INFO:
			return "FrameworkEvent INFO";
		case FrameworkEvent.PACKAGES_REFRESHED:
			return "FrameworkEvent: PACKAGE REFRESHED";
		case FrameworkEvent.STARTED:
			return "FrameworkEvent: STARTED";
		case FrameworkEvent.STARTLEVEL_CHANGED:
			return "FrameworkEvent: STARTLEVEL CHANGED";
		case FrameworkEvent.WARNING:
			return "FrameworkEvent: WARNING";
		default:
			return null;
		}
	}

	private String getServiceEventMessage(int event) {
		switch (event) {
		case ServiceEvent.MODIFIED:
			return "Service modified by bundle ";
		case ServiceEvent.REGISTERED:
			return "Service registered by bundle ";
		case ServiceEvent.UNREGISTERING:
			return "Service Unregistering from bundle ";
		default:
			return null;
		}
	}

	private String toString(Bundle b) {
		return b.getSymbolicName() + "(" + getVersion(b) + ")";
	}

	private static String getVersion(Bundle parBundle) {
		return getVersion(parBundle.getHeaders());
	}

	private static String getVersion(Dictionary headers) {
		return checkDefaultVersion((String) headers.get(BUNDLE_VERSION));
	}

	private static String checkDefaultVersion(String v) {
		if (v == null) {
			v = "1.0.0";
		}
		return v;
	}

	private String getRefProperties(ServiceReference ref) {
		String[] propKeys = ref.getPropertyKeys();
		if (propKeys != null) {
			StringBuffer buf = new StringBuffer(" {");
			int nbProps = propKeys.length;
			for (int i = 0; i < nbProps; i++) {
				buf.append(propKeys[i]);
				buf.append('=');
				Object obj = ref.getProperty(propKeys[i]);
				if (obj != null) {
					if (obj.getClass().isArray()) {
						buf.append('[');
						for (int j = 0; j < Array.getLength(obj); j++) {
							buf.append(Array.get(obj, j).toString());
							if (j < (Array.getLength(obj) - 1)) {
								buf.append(",");
							}
						}
						buf.append(']');
					} else {
						buf.append(obj.toString());
					}
					if (i < (nbProps - 1))
						buf.append(',');
				}
			}
			buf.append('}');
			return buf.toString();
		} else {
			return "";
		}
	}
}
