package com.alcatel.as.service.spi.impl;

// Osgi
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;

import com.alcatel.as.util.cl.BundleClassLoader;

/**
 * This component uses the Extender pattern in order to ease the management of Service Provide
 * Interface management.
 */
public class SpiLoaderImpl implements SynchronousBundleListener
{
    private static final Logger _logger = Logger.getLogger("as.service.spi.SpiLoaderImpl");

    private BundleContext _bc; // injected by DepMgr

    static class SPIRegistration
    {
        SPIRegistration(ServiceRegistration sr, String service)
        {
            _sr = sr;
            _service = service;
        }

        ServiceRegistration _sr;

        String _service;
    }

    private Hashtable<String, List<SPIRegistration>> _services;

    // --------------- Component life cycle ---------------------------------------

    protected void start()
    {
        _services = new Hashtable<String, List<SPIRegistration>>();
        if (_logger.isInfoEnabled())
        {
            _logger.info("SpiLoader started");
        }
        Bundle[] bundles = _bc.getBundles();
        for (Bundle b : bundles)
        {
            if (b.getState() == b.ACTIVE)
            {
                registerSpiBundle(b);
            }
        }
        _bc.addBundleListener(this);
    }

    protected void stop()
    {
        if (_logger.isInfoEnabled())
        {
            _logger.info("SpiLoader stopped");
        }
        _bc.removeBundleListener(this);
        unregisterSpiBundles();
    }

    // ---------- BundleListener interface ---------------------------------------

    public void bundleChanged(BundleEvent event)
    {
        Bundle b = event.getBundle();
        Enumeration services = null;

        switch (event.getType()) {
        case BundleEvent.STARTED:
            registerSpiBundle(b);
            break;

        case BundleEvent.STOPPING:
            unregisterSpiBundle(b);
            break;

        default:
            // Nothing to do
            break;
        }
    }

    // ---------- Private methods ------------------------------------------------

    private Enumeration getSPI(Bundle b)
    {
        String location = b.getLocation();
        if (!location.endsWith(".jar"))
        {
            return null;
        }
        Dictionary h = b.getHeaders();
        if (h.get("Bundle-Activator") != null || h.get("Service-Component") != null
                || h.get("Fragment-Host") != null)
        {
            return null;
        }

        if ("ignore".equals(h.get("Spi-Loader")))
        {
            return null;
        }
        return b.getEntryPaths("/META-INF/services/");
    }

    private String getServiceInterface(String service)
    {
        return service.substring("META-INF/services/".length());
    }

    private void registerSpiBundle(Bundle b)
    {
        String symname = b.getSymbolicName();
        if (symname == null)
            throw new NullPointerException("bundle " + b.getLocation() + " has no SymbolicName");

        List<SPIRegistration> oldList = _services.remove(symname);
        try
        {
            Enumeration services = getSPI(b);
            if (services == null)
            {
                return;
            }

            List<SPIRegistration> list = new ArrayList<SPIRegistration>();
            _services.put(symname, list);

            while (services.hasMoreElements())
            {
                String service = (String) services.nextElement();
                try
                {
                    URL url = b.getEntry(service);
                    if (url != null)
                    {
                        Hashtable serviceProperties = new Hashtable();
                        String factoryClassName = getServiceFactory(url, serviceProperties);
                        String serviceInterface = getServiceInterface(service);

                        if ("false".equals(serviceProperties.get("osgi")))
                        {
                            if (_logger.isDebugEnabled())
                            {
                                _logger.debug("Ignoring non-OSGi SPI " + url + " from bundle "
                                        + b.getLocation());
                            }
                            continue;
                        }

                        if (_logger.isInfoEnabled())
                        {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Registering factory \"" + factoryClassName + "\" for service "
                                    + serviceInterface + " from bundle " + symname);
                            if (serviceProperties.size() > 0)
                            {
                                sb.append(" with properties " + serviceProperties);
                            }
                            _logger.info(sb.toString());
                        }

                        ServiceRegistration sr =
                                _bc.registerService(serviceInterface,
                                                    loadClass(b, factoryClassName, serviceProperties),
                                                    serviceProperties);
                        list.add(new SPIRegistration(sr, serviceInterface));
                    }
                    else
                    {
                        throw new IOException("could not load " + service);
                    }
                }

                catch (Throwable t)
                {
                    _logger.error("Could not register service factory for " + service + " from bundle "
                            + symname, t);
                }
            }
        }

        finally
        {
            if (oldList != null)
            {
                for (int i = 0; i < oldList.size(); i++)
                {
                    SPIRegistration old = oldList.get(i);
                    if (_logger.isInfoEnabled())
                    {
                        _logger.info("Unregistering " + old._sr);
                    }
                    old._sr.unregister();
                }
            }
        }
    }

    private void unregisterSpiBundle(Bundle b)
    {
        List<SPIRegistration> list = _services.remove(b.getSymbolicName());
        if (list == null)
        {
            return;
        }
        for (int i = 0; i < list.size(); i++)
        {
            SPIRegistration r = list.get(i);
            if (_logger.isInfoEnabled())
            {
                _logger.info("Unregistering service " + r._service);
            }
            r._sr.unregister();
        }
    }

    private void unregisterSpiBundles()
    {
        Enumeration<String> e = _services.keys();
        while (e.hasMoreElements())
        {
            String symname = e.nextElement();
            List<SPIRegistration> list = _services.get(symname);
            if (list == null)
            {
                return;
            }
            for (int i = 0; i < list.size(); i++)
            {
                SPIRegistration r = list.get(i);
                if (_logger.isInfoEnabled())
                {
                    _logger.info("Unregistering service " + r._service);
                }
                r._sr.unregister();
            }
        }
        _services.clear();
    }

    private String getServiceFactory(URL u, Hashtable serviceProperties) throws IOException
    {
        InputStream in = u.openStream();

        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF8"));
            String factoryClassName = br.readLine();
            if (factoryClassName == null)
            {
                throw new IllegalArgumentException("file is empty: " + u);
            }
            // add split("#") to ignore comments
            factoryClassName = factoryClassName.split("#")[0].trim();

            // If the class name has the syntax: <classname>;param1=value1,param2=value2 ...", then
            // extract parameters and store them into the service properties.
            // Notice that parameter "useCCL" is specific to our spi loader and specifies that
            // thread context class loader must be set before instantiating spi impl ...

            int semi = factoryClassName.indexOf(";");
            if (semi != -1)
            {
                String parameters = factoryClassName.substring(semi + 1).trim();
                if (parameters.length() > 0)
                {
                    StringTokenizer tokens = new StringTokenizer(parameters, ";");
                    while (tokens.hasMoreTokens())
                    {
                        String token = tokens.nextToken().trim();
                        int eq = token.indexOf("=");
                        if (eq != -1)
                        {
                            String name = token.substring(0, eq);
                            String val = token.substring(eq + 1);
                            serviceProperties.put(name, val);
                        }
                    }
                }
                factoryClassName = factoryClassName.substring(0, semi);
            }

            return factoryClassName;
        }
        finally
        {
            in.close();
        }
    }

    private Object loadClass(Bundle b, String factoryClassName, Hashtable serviceProperties) throws Exception
    {
        Thread currThread = Thread.currentThread();
        ClassLoader currCL = currThread.getContextClassLoader();

        try
        {
            ClassLoader CCL = null;
            String useCCL = (String) serviceProperties.get("useCCL");
            if (useCCL != null && Boolean.parseBoolean(useCCL))
            {
                CCL = new BundleClassLoader(b);
                if (_logger.isDebugEnabled())
                {
                    _logger.debug("Loading service impl " + factoryClassName
                            + " with Thread Context Class Loader");
                }
            }
            currThread.setContextClassLoader(CCL);
            return b.loadClass(factoryClassName).newInstance();
        }

        finally
        {
            currThread.setContextClassLoader(currCL);
        }
    }
}
