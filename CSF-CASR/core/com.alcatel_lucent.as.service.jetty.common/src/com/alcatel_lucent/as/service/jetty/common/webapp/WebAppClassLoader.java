package com.alcatel_lucent.as.service.jetty.common.webapp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.Bundle;

import com.alcatel_lucent.as.service.jetty.common.utils.Log;

public class WebAppClassLoader extends org.eclipse.jetty.webapp.WebAppClassLoader {

  private final Bundle _bundle;
  private volatile static URLClassLoader _resourceCL;
  private final static Log _log = new Log(WebAppClassLoader.class);

  private final static String[] NOT_OVERRIDDEN_PKGS = {
    "javax/servlet",
    "javax/servlet/http",
    "javax/servlet/resources",
    "javax/servlet/jsp",
    "javax/servlet/jsp/el",
    "javax/servlet/jsp/resources",
    "javax/servlet/jsp/tagext",
    "javax/el",
    "javax/xml",
    "javax/xml/datatype",
    "javax/xml/namespace",
    "javax/xml/parsers",
    "javax/xml/transform",
    "javax/xml/transform/dom",
    "javax/xml/transform/sax",
    "javax/xml/transform/stream",
    "javax/xml/validation",
    "javax/xml/xpath",
    "org/xml/sax",
    "org/xml/sax/ext",
    "org/xml/sax/helpers",
    "org/w3c/dom",
    "org/w3c/dom/bootstrap",
    "org/w3c/dom/css",
    "org/w3c/dom/events",
    "org/w3c/dom/html",
    "org/w3c/dom/ls",
    "org/w3c/dom/ranges",
    "org/w3c/dom/stylesheets",
    "org/w3c/dom/traversal",
    "org/w3c/dom/views"
  };

  private final boolean[] _overridenPkgs = new boolean[NOT_OVERRIDDEN_PKGS.length];
  
  public static void setSharedWebAppResourceClassPath(String[] resourceDirs) throws MalformedURLException {
	  _log.info(() -> "Setting shared webapp resource directories: " + Arrays.toString(resourceDirs));
	  _resourceCL = initResourceClassLoader(resourceDirs);
  }

  private static URLClassLoader initResourceClassLoader(String[] resourceDirs) throws MalformedURLException {
	  if (resourceDirs == null || resourceDirs.length == 0) {
		  return null;
	  }
	  URL[] urls = Stream.of(resourceDirs).map(url -> toURL(url)).filter(Objects::nonNull).toArray(URL[]::new);
	  if (urls.length == 0) {
		  return null;
	  }
	  _log.info(() -> "Setting shared webapp resource urls: " + Arrays.toString(urls));
	  return new URLClassLoader(urls, null);
  }
  
  private static URL toURL(String url) {
	  try {
		  if (! url.endsWith("/")) {
			  url = url + "/"; // url ending with "/" means with are using a directory.
		  }
		  return new URL("file://" + url);
	  } catch (MalformedURLException e) {
		  throw new RuntimeException("Invalid webapp classpath resource path: " + url + " (check web agent configuration)");
	  }
  }

  /**
   * Creates a WebApp Class Loader
   * @param parent
   * @param context
   * @param bundle
   * @param resourceDirs the list of classpath resource dirs which can be accessed by the web app classloader 
   *        (null of empty in case no classpath resource dir is used)
   * @throws IOException
   */
  public WebAppClassLoader(ClassLoader parent, WebAppContext context, Bundle bundle) throws IOException {
	  super(parent, context);
	  _bundle = bundle;
  }

  public WebAppClassLoader(WebAppContext context) throws IOException {
    super(context);
    _bundle = null;
  }
  
  @Override
  public void addClassPath(String classPath) throws IOException {
    // Some webapps wrongly assumes that webapp class loader's getResource method returns
    // JarURLConnection: because of that we'll use our parent URLClassLoader, not our bundle class
    // loader, because the bundle class loader getResource method returns a URL which is NOT a 
    // JarURLConnection (jar://...) but rather a bundle URL (bundle://...)
    JarFile jarFile = null;
    try {
      if (!classPath.endsWith("/")) {
    	_log.debug(() ->  "addClassPath " + classPath);
        jarFile = new JarFile(new File(new URI(classPath)));
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String name = entry.getName();
          if (!entry.isDirectory()) {
            int sep = name.lastIndexOf("/");
            if (sep > 0) {
              String pkgName = name.substring(0, sep);
              for (int i = 0; i < NOT_OVERRIDDEN_PKGS.length; i++) {
                if (!_overridenPkgs[i]
                                   && (pkgName.compareTo(NOT_OVERRIDDEN_PKGS[i]) == 0)) {
                  _overridenPkgs[i] = true;
                  _log.warn(() -> "JSR154: webapp " + ((ContextHandler) getContext()).getContextPath()
                		  + " should not override " + pkgName + " (" + classPath + ")");
                }
              }
            }                    
          }
        }
      }
    } catch (Exception e) {
    	_log.debug(() -> "Exception while adding classpath", e);
    }
    finally {        
      super.addClassPath(classPath);
      if (jarFile != null) jarFile.close();
    }
  }

  @Override
  public void addJars(Resource lib) {
    // Some webapps wrongly assumes that webapp class loader's getResource method returns
    // JarURLConnection: because of that we'll use our parent URLClassLoader, not our bundle class
    // loader, because the bundle class loader's getResource method returns a URL which is NOT a 
    // JarURLConnection (jar://...) but rather a bundle URL (bundle://...)
    super.addJars(lib);
  }

  @Override
  public String toString() {
    if (_bundle != null) {
      return super.toString() + "/bid#" + _bundle.getBundleId();
    }
    return super.toString();
  }

  // Ensures we'll never load a class using the URLClassLoader ...
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected Class findClass(String name) throws ClassNotFoundException {
    return (_bundle != null) ? _bundle.loadClass(name) : super.findClass(name);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jetty.webapp.WebAppClassLoader#loadClass(java.lang.String, boolean)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // Class loaded ?
    Class c = findLoadedClass(name);

    if (c == null) {  
      // Try to find the class in the bundle
      if (_bundle != null) {
        try {
          c = _bundle.loadClass(name);
          if (getContext().isServerClass(c)) {
        	  c = null;
          } else {
        	  if(_log.debug()) _log.debug(" loaded " + c + " from bundle #"+ _bundle.getBundleId());
        	  if (resolve) {
        		  resolveClass(c);
        	  }
          }
        }
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {
        	_log.warn(() -> "Found class " + name + " from " + _bundle.getSymbolicName() + ", but the class could not be loaded", e);
        }
      } 

      if (c == null) {
        // then in the Jetty classloader hierarchy
        c = getParent().loadClass(name);
        if(_log.debug()) _log.debug("loaded " + name + "/class=" + c + " from parent CL");
      }
    }
    if (c == null) throw new ClassNotFoundException(name);
    return c;
  }

  // DCTPD00921540 : use the parent CL before the bundle CL (ICEfaces FaceLets-tutorial)
  // DCTPD01057310 : if not found, use the bundle CL (jersey)
  @Override
  public URL getResource(String name)
  {
		URL resource = super.getResource(name);
		if (resource == null) {
			// try resource classpath URL class loader
			if (_resourceCL != null) {
				resource = _resourceCL.getResource(name);
				if (resource != null) {
					if (_log.debug()) _log.debug(() -> "getResource \"" + name + "\" found from resource classpath");
					return resource;
				}
			}

			if (_bundle != null) {
				resource = _bundle.getResource(name);
				if (resource != null) {
					_log.debug(() -> "getResource " + name + " from bundle #" + _bundle.getBundleId());
					return resource;
				}
			}
		} else {
			_log.debug(() -> "getResource " + name + " from parent CL");
		}
		return resource;
	  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException
  {
		Enumeration<URL> resources = super.getResources(name);
		if (resources == null) {
			// try resource classpath URL class loader
			if (_resourceCL != null) {
				resources = _resourceCL.getResources(name);
				if (resources != null) {
					_log.debug(() -> "getResources \" + name + \" found from resource classpath");
					return resources;
				}
			}

			if (_bundle != null) {
				resources = _bundle.getResources(name);
				if (resources != null) {
					_log.debug(() -> "getResources " + name + " from bundle #" + _bundle.getBundleId());
					return resources;
				}
			}
		} else {
			_log.debug(() -> "getResources " + name + " from parent CL");
		}
		return resources;
	}
  
}
