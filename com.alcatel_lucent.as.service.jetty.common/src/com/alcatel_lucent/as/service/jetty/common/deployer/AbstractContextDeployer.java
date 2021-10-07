package com.alcatel_lucent.as.service.jetty.common.deployer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.management.JMException;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.osgi.framework.Bundle;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel_lucent.as.service.jetty.common.webapp.WebAppClassLoader;
import com.alcatel_lucent.as.service.jetty.common.utils.JettyUtils;
import com.alcatel_lucent.as.service.jetty.common.webapp.AbstractWebAppContext;

public abstract class AbstractContextDeployer extends AbstractLifeCycle implements ComponentStateListener {
  
  public final static String CONTEXT_PATH_ATTR_NAME = "Bundle-ContextPath";
  public final static String CONTEXT_PATH_OSGI_RFC66 = "Web-ContextPath";
  public final static String ROOT_NAME = "root";

  private ContextHandlerCollection contexts;
  private Map<String, ContextHandler> currentDeployments;
  private final static String WEBAPP_FACTORY_KEY = "ServletContext";
  private String protocol;
  protected final static String CONVERGENT_HEADER = "X-ConvergentApplication-name";
  private static AbstractContextDeployer _instance;
  private URL servletContextMbeanUrl;
  private File _overrideDescriptor;
  
  protected abstract ApplicationMBeanFactory getApplicationMBeanFactory();  
  protected abstract void deploy(Bundle bundle, String contextPath, File tmpDirBase,
                                 Map<String, HttpServlet> servlets, Map<String, Filter> filters, Map<String, ServletContextListener> listeners) throws IOException;

  public AbstractContextDeployer(String protocol) {
    super();
    this.protocol = protocol;
    servletContextMbeanUrl = this.getClass().getClassLoader().getResource(AbstractWebAppContext.class.getPackage().getName().replace('.', '/') 
                                                                          + "/mbeans-descriptors.xml");
    _instance = this;
  }

  public AbstractContextDeployer instance() {
    return _instance;
  }
  /**
   * @return the contexts
   */
  public ContextHandlerCollection getContexts() {
    return contexts;
  }

  /**
   * @param contexts the contexts to set
   */
  public void setContexts(ContextHandlerCollection contexts) {
    this.contexts = contexts;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStart()
   */
  @Override
  protected void doStart() throws Exception {
    super.doStart();
    currentDeployments=new HashMap<String, ContextHandler>();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStop()
   */
  @Override
  protected void doStop() throws Exception {
    super.doStop();
  }

  public void deploy(File file, String context) throws IOException {
    try {
      // get webapp name
      String contextName = getWebappName(file);
      // Is context already deployed ?
      if (currentDeployments.containsKey(contextName)) {
        throw new IOException("file already deployed:" + contextName);
      }
      XmlConfiguration xmlConfiguration = new XmlConfiguration(context);
      ContextHandler contextHandler = (ContextHandler) xmlConfiguration.configure();
      // deploy the context
      Log.getRootLogger().warn("Deploy file " + file.getName() + " as webapp -> " + contextHandler
                               + " context=\n" + context);
      contexts.addHandler(contextHandler);
      currentDeployments.put(contextName, contextHandler);
      if (contexts.isStarted()) {
        contextHandler.start();
      }
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  public int getDeployments() {
    return currentDeployments.size();
  }

  protected synchronized void doDeploy(AbstractWebAppContext webAppContext, Bundle bundle, String contextPath, File tmpDirBase) 
  throws IOException {
    ClassLoader wcCl = this.getClass().getClassLoader();
    ClassLoader tCl = null;
    try {
      Log.getRootLogger().debug("deploy webapp - parent CL: " + wcCl + " isConvergent: " + isConvergentWebapp(bundle));
      // get webapp name
      String webAppName = webAppContext.getWebAppName();
      // if context null : set context path
      if (contextPath == null) {
        // is context defined in the manifest ?
        Dictionary<?, ?> dico = bundle.getHeaders();
        contextPath = (String) dico.get(CONTEXT_PATH_OSGI_RFC66);
        if (contextPath == null) {          
          contextPath = (String) dico.get(CONTEXT_PATH_ATTR_NAME);
          if (contextPath != null) {
            Log.getRootLogger().warn("webapp " + webAppName + ": " + CONTEXT_PATH_ATTR_NAME + 
                                     " is deprecated. You should use " + CONTEXT_PATH_OSGI_RFC66 + " instead.");
          }
        }
        if (contextPath == null) {
          // if not, get the file name
          if (webAppName.compareToIgnoreCase(ROOT_NAME) == 0) {
            contextPath = "/";
          } else {
            contextPath = "/" + webAppName;
          }
        } else {
          contextPath = contextPath.trim();
        }
      }            
      // Is context already deployed ?
      if (currentDeployments.containsKey(webAppName)) {
        throw new IOException("bundle already deployed: " + webAppName);
      }

      String fileLocation = null;
      File tmpFile = null;

      // copy the bundle from its framework cache to a temp file.

      tmpFile = new File (tmpDirBase, webAppName);
      if (tmpFile.exists()) {
        tmpFile.delete();
      }
      tmpFile.deleteOnExit();
      copyBundle(bundle, tmpFile);
      fileLocation = tmpFile.getAbsolutePath();

      // Initialize the WebAppContext
      //      WebAppContext webAppContext = new WebAppContext(webAppName);
      webAppContext.setContextPath(contextPath);
      webAppContext.setWar(fileLocation);
      // set a URLCLassLoader
      WebAppClassLoader webAppClassLoader = new WebAppClassLoader(wcCl, webAppContext, bundle);
      webAppContext.setClassLoader(webAppClassLoader);
      // Set tmp dir
      webAppContext.setTempDirectory(tmpDirBase, bundle.getBundleId(), webAppName, contextPath);
      // change the TCL
      tCl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(wcCl);      
      // JMX stuff
      ApplicationMBeanFactory factory = getApplicationMBeanFactory();
      if (factory != null) {
        if (servletContextMbeanUrl != null)
        {
          factory.loadDescriptors(webAppName, servletContextMbeanUrl);
          // - Add a mbean for the webapp
          factory.registerObject(webAppName, protocol, webAppContext, WEBAPP_FACTORY_KEY, 1, 0);
        }
        // - and load the mbeans descriptors
        // --- old xml file
        URL url = bundle.getResource("META-INF/mbeans-descriptors.xml");
        if (url != null) {
          factory.loadDescriptors(webAppName, url);
        }
        // --- new style coming from annotations
        @SuppressWarnings("rawtypes")
        Enumeration mdbs = bundle.findEntries("META-INF", "*.mbd", false);
        if (mdbs != null)
        {
          while (mdbs.hasMoreElements())
          {
            factory.loadDescriptors(webAppName, (URL) mdbs.nextElement());
          }
        }
      }
      // deploy the context
      contexts.addHandler(webAppContext);
      currentDeployments.put(webAppName, webAppContext);
      if (contexts.isStarted()) {
        // start the webapp context
    	  if (_overrideDescriptor != null) {
              String absolutePath = _overrideDescriptor.getAbsolutePath();
			  Log.getRootLogger().info(webAppName + " : overriding descriptor with " + absolutePath);
    		  webAppContext.setOverrideDescriptor(absolutePath);
    	  }
        webAppContext.start();
        // delete local copy of the war
        if (tmpFile != null) {
          tmpFile.delete();
        }
      }
    } 

    catch (IOException e) {
      Log.getRootLogger().debug(e);
      throw e;
    }
    catch (Exception e) {
      Log.getRootLogger().debug(e);
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe;
    }
    finally {
      if (tCl != null) {
        Thread.currentThread().setContextClassLoader(tCl);                
      }
    }

  }

  public void undeploy(File file) throws IOException {
    try {
      // get webapp name
      String contextName = getWebappName(file);
      doUndeploy(contextName);
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
  }

  public void undeploy(Bundle bundle) throws IOException {
    try {
      // get webapp name
      String contextName = getWebappName(bundle);
      doUndeploy(contextName);
    } 
    catch (IOException e) {
      throw e;
    }
    catch (Exception e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe;
    }
  }

  public void undeploy(String contextPath) throws IOException {
    try {
      Iterator<String> it = currentDeployments.keySet().iterator();
      while (it.hasNext()) {
        // Get key
        String contextName = (String) it.next();
        if (currentDeployments.get(contextName).getContextPath().compareTo(contextPath) == 0) {
          doUndeploy(contextName);
          break;
        }
      }        
    } catch (Exception e) {
      Log.getRootLogger().warn("Cannot undeploy " + contextPath, e);
      throw new IOException(e.getMessage());
    }
  }

  public void undeployAll() {
    Log.getRootLogger().info("Undeploy all the webapps");
    if (currentDeployments != null) {
      Object[] contexts = currentDeployments.keySet().toArray();
      for (int i = 0; i < contexts.length; i++) {
        try {
          doUndeploy((String) contexts[i]);
        } catch (Exception ignored) {
          Log.getRootLogger().debug("Cannot undeploy " + contexts[i], ignored);
        }
      }            
    }
  }

  private void doUndeploy(String contextName) throws Exception {
    // undeploy the context
    ContextHandler context = currentDeployments.get(contextName);
    if (context!=null) {
      // unregister mbeans
      ApplicationMBeanFactory factory = getApplicationMBeanFactory();
      try {
        if (factory != null) {
          // remove webapp mbean
          try {
            factory.unregisterObject(contextName, protocol, WEBAPP_FACTORY_KEY, 1, 0);
          } catch (JMException ignored) {
          }
          // remove servlet mbeans
          ServletHolder[] holders = ((AbstractWebAppContext) context).getServletHandler().getServlets();
          if (holders != null) {
            for (int i = 0; i < holders.length; i++) {
              try {
                if ((holders[i].getHeldClass() != null) && (holders[i].getServlet() != null)) {
                  factory.unregisterServlet(contextName, protocol, holders[i].getServlet());
                }
              } catch (JMException ignored) {
              }
            }
          }
          // remove mbeans descriptors
          factory.unloadDescriptors(contextName);
        }
      } catch (ServletException ignored) {
      }           
      // stop the context
      Log.getRootLogger().warn("Undeploy " + contextName + " as webapp <- " + context);
      context.stop();
      contexts.removeHandler(context);
      currentDeployments.remove(contextName);
    }
  }

  private String getWebappName(String webappFile) throws Exception {
    String webappName = new File(webappFile).getName();
    int dotPosition = webappName.lastIndexOf('.');
    if (dotPosition > 0) {
      webappName = webappName.substring(0, dotPosition);
    } 
    return webappName;
  }

  private String getWebappName(File file) throws Exception {
    return getWebappName(file.getName());
  }

  protected String getWebappName(Bundle bundle) throws Exception {
    String bname = (String) bundle.getHeaders().get("Bundle-Name");
    return bname.replace(' ', '-').replace('_', '-');
  }

  @SuppressWarnings("rawtypes")
  private void copyBundle(Bundle bundle, File f) throws IOException {
    int bufSize=16*1024;
    if (Log.getRootLogger().isDebugEnabled()) {
      Log.getRootLogger().debug("copying bundle " + bundle.getSymbolicName() + " to " + f);
    }
    byte[] buf = new byte[bufSize];
    JarOutputStream out = new JarOutputStream(new FileOutputStream(f));
    out.setLevel(0);
    Enumeration e = bundle.findEntries("/", "*", true);
    while (e.hasMoreElements()) {
      URL entry = (URL) e.nextElement();
      if (Log.getRootLogger().isDebugEnabled()) {
        Log.getRootLogger().debug("\tentry: " + entry.getPath());
      }
      if (! entry.getPath().endsWith("/")) {
        String entryName = entry.getPath().substring(1);
        out.putNextEntry(new JarEntry(entryName));
        InputStream in = new BufferedInputStream(entry.openStream(), bufSize);
        int n;
        while ((n = in.read(buf)) != -1) {
          out.write(buf, 0, n);
        }
        in.close();
        out.closeEntry();
      }
    }
    out.close();
  }

  protected boolean isConvergentWebapp(Bundle bundle) {
    for(Enumeration<?> keys = bundle.getHeaders().keys(); keys.hasMoreElements();) {
      String tmp = ((String) keys.nextElement());
      if(CONVERGENT_HEADER.equalsIgnoreCase(tmp)) {
        return true;
      }
    }
    return false;
  }

  /** 
   * Internal class to store deployment data to give to the webapp context
   *
   */
  public static class WebAppDeplCtx{
    Bundle bundle;
    String contextPath;
    File tmpDirBase;

    public WebAppDeplCtx(Bundle bundle, String contextPath, File tmpDirBase) {
      this.bundle = bundle;
      this.contextPath = contextPath;
      this.tmpDirBase = tmpDirBase;
    }
  }


  //------------------- ServiceStateListener interface ---------------------------------------
  // Used by OSGi framework to informs about webapp state 

  public void changed(Component c, ComponentState state) {
      switch (state) {
      case TRACKING_OPTIONAL:
    	  started(c);
    	  break;
      case INACTIVE:
          stopped(c);
          break;
      default:
      }
}
  
  /**
   * Once the service is started, we have to finish the deployment of the webapp
   */
  public void started(Component service) {
    AbstractWebAppContext webAppContext = (AbstractWebAppContext) service.getInstance();
    Log.getRootLogger().debug("Service is started - WebAppContext:" + webAppContext);
    WebAppDeplCtx ctx = webAppContext.getDplCtx();

    try {
      doDeploy(webAppContext, ctx.bundle, ctx.contextPath, ctx.tmpDirBase);
    } catch (IOException e) {
      Log.getRootLogger().warn(e);
    }
  }

  public void stopped(Component service) {
    AbstractWebAppContext webAppContext = (AbstractWebAppContext) service.getInstance();
    Log.getRootLogger().debug("Service is stopped - WebAppContext:" + webAppContext);
    WebAppDeplCtx ctx = webAppContext.getDplCtx();
    try {
      // get webapp name
      String webAppName = getWebappName(ctx.bundle);
      doUndeploy(webAppName);
    } 
    catch (IOException e) {
      //throw e;
    }
    catch (Exception e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      //throw ioe;
    }
  }
  
  public File getOverrideDescriptor() {
    return _overrideDescriptor;
  }
	
  public void setOverrideDescriptor(File _overrideDescriptor) {
    this._overrideDescriptor = _overrideDescriptor;
  }
}
