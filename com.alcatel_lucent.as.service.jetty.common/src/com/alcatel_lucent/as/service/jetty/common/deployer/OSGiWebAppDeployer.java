// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.deployer;

import static org.osgi.framework.Constants.IMPORT_PACKAGE;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.apache.felix.dm.annotation.api.BundleDependency;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.diagnostics.ServiceDiagnostics;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.util.apptracker.AppService;

/**
 * This Service tracks web applications, and register into the OSGi registry some corresponding
 * WebApplication objects.
 * The JettyContainerPushlet will then be injected with such WebApplication objects, and will
 * then be activated once every expected web applications are registered.
 * The WebApplications objects will be registered with the service properties:
 * 
 *    - context-path -> the web application alias/context-path
 */
@Component(properties = { @Property(name = "protocol", value = "WEB") })
public class OSGiWebAppDeployer extends Observable implements AppService
{
	@Inject
	volatile BundleContext _bc;
	
    /**
     * Map of all configuration for registered webapps.
     * Key = WebApplication. Val = Configuration used to instantiate the WebApplicaton with the
     * webappFactory.
     * @see {@link #_webappFactory}.
     */
    private Map<WebApplicationImpl, ServiceRegistration<?>> _webappConfigs = new HashMap<>();

    /**
     * Map of all known web applications.
     * Key = Bundle Id. Val = Web Application.
     */
    private Hashtable<Long, WebApplicationImpl> _webappsByBundleId =
            new Hashtable<Long, WebApplicationImpl>();

    /**
     * Map of Web Applications, index by their context path.
     */
    private Map<String, WebApplicationImpl> _webappsByContextPath = new HashMap<String, WebApplicationImpl>();

    /**
     * HTTP Executor needed to schedule our service diagnostic task
     */
    @ServiceDependency(filter = "(id=main)")
    PlatformExecutor _mainExecutor;

    /**
     * Service used to lookup the bundle of a given java class.
     */
    @ServiceDependency
    private PackageAdmin _packageAdmin;

    /**
     * Service Diagnostics
     */
    @ServiceDependency
    private ServiceDiagnostics _diagnostics;

    /**
     * Our Logger
     */
    private LogService _logger;

    /**
     * Create our logger from this logFactory.
     */
    @ServiceDependency
    public void bind(LogServiceFactory logFactory)
    {
        _logger = logFactory.getLogger("jetty.common.deployer");
    }

    /**
     * Special headers provided by servlets
     */
    protected final static String EXPORT_SERVLET = "X-Export-Http-Servlets";
    protected final static String EXPORT_FILTER = "X-Export-Http-Filters";
    protected final static String EXPORT_LISTENER = "X-Export-Http-Listeners";

    /**
     * Timer used to diagnose web app unavailability.
     */
    private final static int COUNTDOWN = 60000;

    /**
     * Track all started bundles.
     */
    @BundleDependency(required = false, stateMask = Bundle.ACTIVE, removed = "bundleStopped")
    public void bundleStarted(Bundle bundle)
    {
        if (isWAR(bundle) && isAutoDeployable(bundle))
        {
            _logger.debug("discovered starting webapp bundle: %s", bundle.getSymbolicName());
            WebApplicationImpl webapp = getWebApp(bundle, true);
            webapp.started();
            register(webapp);
        }
    }

    /**
     * A bundle is stopped. Check if it contains a webapp, and unregister it.
     */
    public void bundleStopped(Bundle bundle)
    {
        if (isWAR(bundle) && isAutoDeployable(bundle))
        {
            unregister(null, bundle);
        }
    }

    // ----------------- AppService interface -------------------------------------------------

    public synchronized void registerApplication(Bundle b)
    {
        logDeprecated();
        _logger.debug("registerApplication: bundle=%s", b.getLocation());
        try
        {
            if (isExportingServletOrFilterOrListener(b))
            {
                logForbidden(); //really?
            }
            else
            {
                // Regular webapp using AppService, without using ServletAsService
                WebApplicationImpl webapp = getWebApp(b, true);
                webapp.started();
                register(webapp);
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Could not register webapp " + b.getLocation(), t);
        }
    }

    public synchronized void unregisterApplication(Bundle b)
    {
        _logger.debug("unregisterApplication: bundle=%s", b.getLocation());
        try
        {
            unregister(null, b);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Could not unregister webapp " + b.getLocation(), t);
        }
    }

    // ---------------------- Servlet/Filter life-cycle ---------------------------------------

    @ServiceDependency(required = false, removed = "servletRemoved")
    protected void servletRegistered(HttpServlet servlet)
    {
        WebApplicationImpl webapp = getWebApp(servlet.getClass(), true);
        webapp.addServlet(servlet);
        _logger.debug("Servlet %s registered for web application in %s", servlet, webapp);
        register(webapp);
    }

    protected void servletRemoved(HttpServlet servlet)
    {
        WebApplicationImpl webapp = getWebApp(servlet.getClass(), false);
        if (webapp != null)
        {
            webapp.removeServlet(servlet);
            _logger.warn("Servlet %s unregistered for web application in %s", servlet, webapp);
        }
    }

    @ServiceDependency(required = false, removed = "filterRemoved")
    protected void filterRegistered(Filter filter)
    {
        WebApplicationImpl webapp = getWebApp(filter.getClass(), true);
        webapp.addFilter(filter);
        _logger.debug("Filter %s registered for web application in %s", filter, webapp);
        register(webapp);
    }

    protected void filterRemoved(Filter filter)
    {
        WebApplicationImpl webapp = getWebApp(filter.getClass(), false);
        if (webapp != null)
        {
            webapp.removeFilter(filter);
            _logger.warn("Filter %s unregistered for web application in %s", filter, webapp);
        }
    }

    @ServiceDependency(required = false, removed = "listenerRemoved")
    protected void listenerRegistered(ServletContextListener listener)
    {
        WebApplicationImpl webapp = getWebApp(listener.getClass(), true);
        webapp.addListener(listener);
        _logger.debug("Listener %s registered for web application in %s", listener, webapp);
        register(webapp);
    }

    protected void listenerRemoved(ServletContextListener listener)
    {
        WebApplicationImpl webapp = getWebApp(listener.getClass(), false);
        if (webapp != null)
        {
            webapp.removeListener(listener);
            _logger.warn("Listener %s unregistered for web application in %s", listener, webapp);
        }
    }

    // ---------------------- Private methods -------------------------------------------------

    private synchronized void register(WebApplicationImpl webapp)
    {
        register(null, webapp);
    }

    private synchronized void register(String contextPath, WebApplicationImpl webapp)
    {
        if (!webapp.isReady())
        {
            _logger.debug("webapp %s is not yet ready to be deployed", webapp);
            return;
        }

        _logger.debug("deploying webapp %s, alias=%s", webapp, contextPath);
        try
        {
            // Now, register a new Web Application instance into the OSGi registry:

            // First, create a configuration for the new web application instance. In this configuration,
            // we possibly add the context-path, because the configuration is propagated to 
            // the OSGi service properties.
            Dictionary<String, Object> instanceConfig = new Hashtable<String, Object>();
            if (contextPath != null)
            {
                if (_webappsByContextPath.get(contextPath) != null)
                {
                    throw new IllegalStateException("can't deploy webapp " + webapp + ", contextpath="
                            + contextPath + "): webapp already deployed");
                }

                instanceConfig.put("context-path", contextPath);
            }

            // TODO: why do we need to register the WebApplication ?
            ServiceRegistration<?> reg = _bc.registerService(WebApplication.class, webapp, instanceConfig);
            _webappConfigs.put(webapp, reg);
            
            // At this point, the JettyWebContainer pushlet will be injected with a new WebApplication
            // service instance, and will then deploy it into the WebConnector. And once
            // expected webapps are loaded, then the JettyContainerPushlet will register itself
            // into the registry, in order to activate the http agent.

            // Keep track of the webapp if it has a context-path
            if (contextPath != null)
            {
                _webappsByContextPath.put(contextPath, webapp);
            }
        }
        catch (Throwable e)
        {
            _logger.warn("deploy failed for " + webapp + ", alias=" + contextPath, e);
        }
    }

    private synchronized void unregister(String contextPath, Bundle b)
    {
        _logger.debug("undeploying bundle %s, alias=%s", b.getLocation(), contextPath);
        if (contextPath != null)
        {
            WebApplicationImpl webapp = _webappsByContextPath.remove(contextPath);
            if (webapp != null)
            {
                ServiceRegistration<?> reg = _webappConfigs.remove(webapp);
                if (reg != null)
                {
                	try {
                		reg.unregister();
                	} catch (Exception e) {}
                }
            }
	}

        if (b != null)
        {
            WebApplicationImpl webapp = _webappsByBundleId.remove(b.getBundleId());
            if (webapp != null)
            {
                // Now we have the Web Application, lookup the corresponding webapp instance configuration.
                ServiceRegistration<?> reg = _webappConfigs.remove(webapp);
                if (reg != null)
                {
		    try {
			reg.unregister();
		    } catch (Exception e) {}
                }
            }
        }
    }

    private boolean isWAR(Bundle b)
    {
        String contextPath = (String) b.getHeaders().get(AbstractContextDeployer.CONTEXT_PATH_OSGI_RFC66);
        if (contextPath == null) {
        	return false;
        }
        return (b.getEntry("WEB-INF/web.xml") != null || b.getEntry("WEB-INF/WEB.xml") != null);
    }

    /**
     * returns true if bundle is NOT using AppService and can be deployed right away
     */
    private boolean isAutoDeployable(Bundle b)
    {
        String imports = (String) b.getHeaders().get(IMPORT_PACKAGE);
        if (imports != null && imports.indexOf(AppService.class.getPackage().getName()) != -1)
        {
            String name = b.getSymbolicName();
            _logger.debug("bundle "
                    + name
                    + " imports "
                    + AppService.class.getPackage()
                    + ": It must register/unregister itself via the deprecated AppService.registerApplication() method."
                    + " This application won't be deployed/undeployed for now.");
            return false;
        }
        return true;
    }

    /**
     * returns true if bundle has at least one valid "export-servlets" header
     */
    private boolean isExportingServletOrFilterOrListener(Bundle b)
    {
        Object exports = b.getHeaders().get(EXPORT_SERVLET);
        if (exports != null)
        {
            String name = b.getSymbolicName();
            if (exports.toString().contains("*"))
            {
                _logger.warn("bundle " + name + " asterisk is not supported in " + EXPORT_SERVLET);
                return false;
            }
            else
            {
                _logger.debug("bundle " + name + " exports " + exports
                        + ": It will be deployed when the exported servlets/filters will be registered."
                        + " This application won't be deployed/undeployed for now.");
                return true;
            }
        }

        exports = b.getHeaders().get(EXPORT_FILTER);
        if (exports != null)
        {
            String name = b.getSymbolicName();
            if (exports.toString().contains("*"))
            {
                _logger.warn("bundle " + name + " asterisk is not supported in " + EXPORT_FILTER);
                return false;
            }
            else
            {
                _logger.debug("bundle " + name + " exports " + exports
                        + ": It will be deployed when the exported filters will be registered."
                        + " This application won't be deployed/undeployed for now.");
                return true;
            }
        }

        exports = b.getHeaders().get(EXPORT_LISTENER);
        if (exports != null)
        {
            String name = b.getSymbolicName();
            if (exports.toString().contains("*"))
            {
                _logger.warn("bundle " + name + " asterisk is not supported in " + EXPORT_LISTENER);
                return false;
            }
            else
            {
                _logger.debug("bundle " + name + " exports " + exports
                        + ": It will be deployed when the exported listeners will be registered."
                        + " This application won't be deployed/undeployed for now.");
                return true;
            }
        }

        return false;
    }

    private WebApplicationImpl getWebApp(Class<?> clazz, boolean init)
    {
        return getWebApp(_packageAdmin.getBundle(clazz), init);
    }

    private WebApplicationImpl getWebApp(Bundle b, boolean init)
    {
        WebApplicationImpl webapp = _webappsByBundleId.get(b.getBundleId());
        if (webapp == null && init)
        {
            _logger.debug("Creating web app for bundle %s", b.getSymbolicName());
            webapp = new WebApplicationImpl(b);
            _webappsByBundleId.put(b.getBundleId(), webapp);
            final WebApplicationImpl $webapp = webapp;
            _mainExecutor.schedule(new Runnable()
            {
                public void run()
                {
                    if (!$webapp.isReady())
                    {
                        _logger.warn($webapp + " is NOT READY to be deployed after " + COUNTDOWN
                                + "ms. Awaiting : " + $webapp.awaiting() + ". Service diagnostics summary : "
                                + _diagnostics.notAvail());
                    }
                    else if (!$webapp.isInitDone())
                    {
                        _logger.warn($webapp + " is ready but NOT DEPLOYED after " + COUNTDOWN
                                + "ms. Service diagnostics summary : " + _diagnostics.notAvail());
                    }
                }
            }, COUNTDOWN, TimeUnit.MILLISECONDS);
        }
        return webapp;
    }

    private void logDeprecated()
    {
        _logger.warn("DEPRECATED: the webapp should use " + EXPORT_SERVLET + "/" + EXPORT_FILTER + "/"
                + EXPORT_LISTENER + " to resolve OSGi dependencies");
    }

    private void logForbidden()
    {
        _logger.error("FORBIDDEN: a webapp using " + EXPORT_SERVLET + "/" + EXPORT_FILTER + "/"
                + EXPORT_LISTENER + " cannot be deployed through the AppService");
    }
}
