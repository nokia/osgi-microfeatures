package com.alcatel_lucent.as.service.jetty.common.deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;

public class WebApplicationImpl extends Observable implements WebApplication
{
    public final static String FACTORY = "com.alcatel_lucent.as.service.jetty.common.deployer";
    private final static Logger _logger = Logger.getLogger("jetty.common.deployer");

    private Bundle bundle;

    private Map<String, HttpServlet> servlets = new HashMap<String, HttpServlet>();
    private Map<String, Filter> filters = new HashMap<String, Filter>();
    private Map<String, ServletContextListener> listeners = new HashMap<String, ServletContextListener>();

    private boolean started;
    private volatile boolean initDone = false;

    private ArrayList<HttpServlet> tmpServlets;
    private ArrayList<Filter> tmpFilters;
    private ArrayList<ServletContextListener> tmpListeners;

    protected WebApplicationImpl()
    {
        tmpServlets = new ArrayList<HttpServlet>();
        tmpFilters = new ArrayList<Filter>();
        tmpListeners = new ArrayList<ServletContextListener>();
    }

    protected WebApplicationImpl(Bundle bundle)
    {
        this();
        init(bundle);
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    protected void init(Bundle bundle)
    {
        this.bundle = bundle;
        // Exported servlets
        Object exports = bundle.getHeaders().get(OSGiWebAppDeployer.EXPORT_SERVLET);
        if (exports != null)
        {
            for (String servlet : exports.toString().split(","))
            {
                servlets.put(servlet.trim(), null);
            }
            _logger.info(this + " exports servlets " + servlets.keySet());
        }
        // Exported filters
        exports = bundle.getHeaders().get(OSGiWebAppDeployer.EXPORT_FILTER);
        if (exports != null)
        {
            for (String servlet : exports.toString().split(","))
            {
                filters.put(servlet.trim(), null);
            }
            _logger.info(this + " exports filters " + filters.keySet());
        }
        // Exported listeners
        exports = bundle.getHeaders().get(OSGiWebAppDeployer.EXPORT_LISTENER);
        if (exports != null)
        {
            for (String listener : exports.toString().split(","))
            {
                listeners.put(listener.trim(), null);
            }
            _logger.info(this + " exports listeners " + listeners.keySet());
        }
    }

    public void started()
    {
        // Set started
        started = true;
        // Add already registered servlets, filters and listeners
        if (tmpServlets != null)
        {
            for (int i = 0; i < tmpServlets.size(); i++)
            {
                addServlet(tmpServlets.get(i));
            }
        }
        tmpServlets = null;
        if (tmpFilters != null)
        {
            for (int i = 0; i < tmpFilters.size(); i++)
            {
                addFilter(tmpFilters.get(i));
            }
        }
        tmpFilters = null;
        if (tmpListeners != null)
        {
            for (int i = 0; i < tmpListeners.size(); i++)
            {
                addListener(tmpListeners.get(i));
            }
        }
        tmpListeners = null;
        _logger.debug("WebApplication bundle started: " + toString());
    }

    public Map<String, HttpServlet> getServlets()
    {
        return servlets;
    }

    public Map<String, Filter> getFilters()
    {
        return filters;
    }

    public Map<String, ServletContextListener> getListeners()
    {
        return listeners;
    }

    protected void addServlet(HttpServlet servlet) throws IllegalStateException
    {
        addXX("Servlet", servlet, tmpServlets, servlets);
    }

    protected void addFilter(Filter filter) throws IllegalStateException
    {
        addXX("Filter", filter, tmpFilters, filters);
    }

    protected void addListener(ServletContextListener listener) throws IllegalStateException
    {
        addXX("Listener", listener, tmpListeners, listeners);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addXX(String type, Object xx, ArrayList tmp, Map map) throws IllegalStateException
    {
        if (!started)
        {
            tmp.add(xx);
        }
        else
        {
            if (isReady() && (map.size() > 0))
            {
                throw new IllegalStateException(getName() + ": Too many " + type + " added! ("
                        + xx.getClass().getName() + ")");
            }
            // Before adding this listener, check that this listener is declared "exported", and not yet added
            String key = xx.getClass().getName();
            if (map.containsKey(key))
            {
                if (map.get(key) == null)
                {
                    map.put(key, xx);
                }
                else
                {
                    throw new IllegalArgumentException(getName() + ": " + type + " already added! ("
                            + xx.getClass().getName() + ")");
                }
            }
            else
            {
                throw new IllegalArgumentException(getName() + ": " + type
                        + " is not in the exported list! (" + xx.getClass().getName() + ")");
            }
        }
    }

    protected void removeServlet(HttpServlet servlet)
    {
        String key = servlet.getClass().getName();
        servlets.remove(key);
        if (tmpServlets != null)
            tmpServlets.remove(key);
    }

    protected void removeFilter(Filter filter)
    {
        String key = filter.getClass().getName();
        filters.remove(key);
        if (tmpFilters != null)
            tmpFilters.remove(key);
    }

    protected void removeListener(ServletContextListener listener)
    {
        String key = listener.getClass().getName();
        listeners.remove(key);
        if (tmpListeners != null)
            tmpListeners.remove(key);
    }

    protected boolean isReady()
    {
        for (HttpServlet servlet : servlets.values())
        {
            if (servlet == null)
                return false;
        }
        for (Filter filter : filters.values())
        {
            if (filter == null)
                return false;
        }
        for (ServletContextListener listener : listeners.values())
        {
            if (listener == null)
                return false;
        }
        return started;
    }

    protected List<String> awaiting()
    {
        List<String> res = new ArrayList<String>();
        for (Map.Entry<String, HttpServlet> e : servlets.entrySet())
        {
            if (e.getValue() == null)
                res.add(e.getKey());
        }
        for (Map.Entry<String, Filter> e : filters.entrySet())
        {
            if (e.getValue() == null)
                res.add(e.getKey());
        }
        for (Map.Entry<String, ServletContextListener> e : listeners.entrySet())
        {
            if (e.getValue() == null)
                res.add(e.getKey());
        }
        return res;
    }

    public String getName()
    {
        if (bundle != null)
        {
            return bundle.getSymbolicName();
        }
        else
        {
            return "";
        }
    }

    public void initDone()
    {
        initDone = true;
        super.setChanged();
        super.notifyObservers();
    }

    protected boolean isInitDone()
    {
        return initDone;
    }

    @Override
    public String toString()
    {
        return bundle + "/" + getName();
    }

}
