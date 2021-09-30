package com.nokia.as.cxf.stest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws22.spi.ProviderImpl;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.*;

/**
 * This component registers tpapps web services into cxf. 
 * mainly, The component does two things:
 * 
 * - it extends the CXFNonSpringServlet cxf servlet, which handles http requests. We register it in the http service
 * using the whiteboard pattern.
 * 
 * - it tracks any tpapps web services, and creates the corresponding cxf endpoint.
 * 
 * Important note: the CXFNonSpringServlet must have been called in init(ServletConfig sc) before any Endpoint is created.
 */
@Component(service={Servlet.class}, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class WebServiceExtender extends CXFNonSpringServlet {

    /**
     * Our logger.
     */
    private final static Logger log = Logger.getLogger(WebServiceExtender.class);
    
    /**
     * The factory needed to create a cxf endpoint.
     */
    private final ProviderImpl provider = new ProviderImpl();
    
    /**
     * List of created cxf endpoints which are created when a tpapps web service is discovered.
     */
    private final Map<Object, Endpoint> endpoints = new HashMap<>();
    
    /**
     * List of tpapps web services that are discovered from the osgi registry before the cxf servlet is initialized.
     * We'll register the web service *AFTER* the servlet has been called in its init method.
     */
    private final List<EarlyWebService> earlyWebServices = new ArrayList<>();
    
    /**
     * Flag telling if the servlet has been initialized.
     */
    private boolean servletInitialized;

    /**
     * Our servlet's alias.
     */
    private String alias;
    
    /**
     * Data structure which holds any web services that are discovered from the osgi registry before the cxf servlet is initialized.
     */
    static class EarlyWebService {
        final Object webService;
        final Map<String, String> properties;

        EarlyWebService(Object webService, Map<String, String> properties) {
            this.webService = webService;
            this.properties = properties;
        }
    }

    /**
     * Our component is stopped, unregister any previously created cxf endpoints.
     */
    @Activate
    void start(Map<String, String> cfg) {
	this.alias = cfg.get("alias");
    }

	/**
     * Our component is stopped, unregister any previously created cxf endpoints.
     */
    @Deactivate
    synchronized void stop() {
        log.info("stop ...");
        for (Map.Entry<Object, Endpoint> endpoint : endpoints.entrySet()) {
            endpoint.getValue().stop();
        }
    }

    /**
     * Our servlet has been initialized: we can now create endpoints for the web services we previously found from the osgi registry.
     */
    @Override
    public synchronized void init(ServletConfig sc) throws ServletException {
	log.warn("init ...");
        super.init(sc);
        this.servletInitialized = true;
        for (EarlyWebService earlyWebService : this.earlyWebServices) {
            doBindWebService(earlyWebService.webService, earlyWebService.properties);
        }
        this.earlyWebServices.clear();
    }

    /**
     * A tpapps web service is found from the osgi registry. We only create the corresponding cxf endpoint
     * if the service has already been initialized, else we store the web service in a temporary list, which will 
     * be scanned from the init method (later).
     * 
     * @param webService a tpapps web service
     * @param properties the osgi service properties (which must contain tpapps.ws  and tpapps.ws.path properties)
     */
    @Reference(target = "(tpapps.ws=true)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private synchronized void bindWebService(Object webService, Map<String, String> properties) {
        if (!servletInitialized) {
            earlyWebServices.add(new EarlyWebService(webService, properties));
            return;
        }
        doBindWebService(webService, properties);
    }
    
    /**
     * A tappps web service is being unregistered from the osgi registry: remove its corresponding cxf endpoint, if already created.
     * 
     * @param webService the unregistered tpapps web service
     * @param properties the osgi service properties (which must contain tpapps.ws  and tpapps.ws.path properties)
     */
    private synchronized void unbindWebService(Object webService, Map<String, String> properties) {
        Endpoint endpoint = endpoints.remove(webService);
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    private void _doBindWebService(Object webService, Map<String, String> properties) {
        String path = properties.get("tpapps.ws.path");
        BusFactory.setThreadDefaultBus(getBus());
        Endpoint endpoint = provider.createEndpoint(null, webService);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        log.warn("bind Web Service: " + webService + ", path=" + path + ", properties=" + properties);
        endpoint.publish(path);
        endpoints.put(webService, endpoint);        
    }

    private void doBindWebService(Object webService, Map<String, String> properties) {
		log.info("bind : " + webService + ", properties=" + properties);
		try {
			String path = properties.get("tpapps.ws.path");
			String rootPath = getRootPath(properties);
			BusFactory.setThreadDefaultBus(getBus());
			Endpoint endpoint = provider.createEndpoint(null, webService);
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			// complete endpoint path is created by concatenating rootpath with path
			path = rootPath + path;
			if (log.isDebugEnabled()) {
				log.info("publishing web service " + webService + " with path: " + path);
			}
			endpoint.publish(path);
			endpoints.put(webService, endpoint);
		} catch (Exception e) {
			log.error("Could not register web service: " + webService + ", properties=" + properties, e);
		}
    }

    /**
     * A tpapps web service is being registered with rootPath +path
     * EndPoint. Paths are picked from the property
     * rootPath (= "tpapps.ws.rootpath") + path( ="tpapps.ws.path")
     *
     * It returns rootpath to be registered. 
     *
     * if "tpapps.ws.rootpath" property is not present in web service component 
     * (@component) then rootpath is calculated like this:
     * - if the alias is not configured or is configured to "/", then the 
     *   rootpath value is by default set to "/soap"
     * - if the alias is configured and is not "/", then an empty rootpath is used
     * 
     * if "tpapps.ws.root" property is present, then it is returned prefixed with a /
     *
     * @param aInProperties web service properties
     * @return  the rootPath (= "/" + "tpapps.ws.rootpath", or "/soap")
     */
    private String getRootPath(Map<String, String> aInProperties)
    {
        String rootPath = aInProperties.get("tpapps.ws.rootpath");
        if (rootPath == null)
        {
	    if (alias != null && alias.equals("/")) {
		// servlet alias is set to "/": so, by default, we want to use "/soap" as the root path
		rootPath = "/soap";
	    } else {
		// servlet alias is set to something different than "/", so we just don't need to set a rootpath,
		// which is already set in the alias property from WebServiceExtender.cfg file.
		rootPath = "";
	    }
        }
        else
        {
            rootPath = rootPath.startsWith("/") ? rootPath : ("/" + rootPath);
        }
        return rootPath;
    }

}
