package alcatel.tess.hometop.gateways.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.log4j.Logger;

/**
 * Class used to load properties using the Jndi interface.
 * If a Config object is found in the thread's context, then this config
 * will be used. If not, then the properties will be retrieved from the
 * class path.
 */
public class JndiConfig implements EventContext {
  
  /** Our logger */
  private final static Logger _logger = Logger.getLogger("as.util.jndi");
  
  /** 
   * Containers can set their config objects in the tread context using this key.
   */
  public final static Object THREAD_CONFIG = new Object();
  
  /**
   * Default property file name.
   */
  public final static String DEFAULT_FILE = "default.properties";
  
  private final static String JAVA_URL_PKG = "org.eclipse.jetty.jndi";
  private final static String NS_SEPARATOR = "/";
  private LocalNameParser parser;
  
  /** sub-context attributes */
  private Context parent;
  private String name;
  private Config bindings;
  
  /**
   * This method activate Our Jndi driver.
   */
  public static void setConfig(Config config) {
    if (System.getProperty("java.naming.factory.initial") == null) {
      System.setProperty("java.naming.factory.initial",
                         "alcatel.tess.hometop.gateways.utils.JndiConfig$Factory");
    }
    JndiConfig.config = config;
    try {
      NamingManager.setInitialContextFactoryBuilder(new FactoryBuilder());
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Constructor for the root context
   * @param env
   */
  public JndiConfig(Hashtable env) {
    this.name = "root";
    parser = new LocalNameParser();
    if (env != null) {
      this.env = (Hashtable) env.clone();
      String urlPkgPrefixes = (String) this.env.get(Context.URL_PKG_PREFIXES);
      if ((urlPkgPrefixes == null) || (urlPkgPrefixes.length() == 0)) {
        // set url package for java
        this.env.put(Context.URL_PKG_PREFIXES, JAVA_URL_PKG);
      } else {
        if (!urlPkgPrefixes.contains(JAVA_URL_PKG)) {
          // add url package for java
          this.env.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes + ":" + JAVA_URL_PKG);
        }
      }
    }
    throwNameNotFound = Boolean.parseBoolean(System.getProperty("alcatel_lucent.jndi.throwNameNotFound",
                                                                "true"));
  }
  
  /**
   * Constructor for sub-contexts
   * @param env
   * @param name
   * @param parent
   * @param parser
   */
  public JndiConfig(Hashtable env, String name, Context parent, LocalNameParser parser) {
    if (env == null) {
      this.env = new Hashtable();
    } else {
      this.env = new Hashtable(env);
    }
    this.name = name;
    this.parent = parent;
    this.parser = parser;
    this.bindings = new Config(new Properties(), name);
  }
  
  public String getName() {
    return this.name;
  }
  
  public JndiConfig getParent() {
    return (JndiConfig) this.parent;
  }
  
  // --------------------------------------------------------------------------------
  // javax.naming.Context impl.
  //
  // Notice that we have set an initial context factory builder in the NamingManager:
  // that's why all our methods perform a check for url passed in arguments in order to
  // eventually delegate method invocation to url context ...
  // --------------------------------------------------------------------------------
  
  public Object lookup(Name name) throws NamingException {
    return lookup(name.toString());
  }
  
  public Object lookup(String name) throws NamingException {
    if (_logger.isDebugEnabled()) {
      _logger.debug(this.name + " lookup " + name);
    }
    
    if (name == null) {
      throw new InvalidNameException("Cannot find to empty name");
    }
    
    if ("".equals(name)) {
      return new JndiConfig(this);
    }
    
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      return urlCtx.lookup(name);
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot lookup in domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = config;
      if (props == null) {
        props = getConfig(name);
      }
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 1) {
        // this is the ending name
        Object binding = getBinding(props, name);
        // bound ?
        if (binding != null) {
          // Remove the binding
          return binding;
        }
        if (throwNameNotFound) {
          throw new NameNotFoundException("Cannot find property " + name);
        } else {
          throw new InvalidNameException("Cannot find property " + name);
        }
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          return ((Context) ctx).lookup(parsedName.getSuffix(1));
        } else {
          _logger.debug(firstComponent + " is not a context");
          // WARNING: Cannot throw NotContextException for JETTY
          throw new NameNotFoundException(firstComponent + " is not a context");
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
  }
  
  /* Start of Write-functionality */
  public void bind(Name name, Object object) throws NamingException {
    bind(name.toString(), object);
  }
  
  public void bind(String name, Object object) throws NamingException {
    if (_logger.isDebugEnabled()) {
      _logger.debug(this.name + " bind " + name);
    }
    
    if ((name == null) || (name.length() == 0)) {
      throw new InvalidNameException("Invalid name " + name);
    }
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      urlCtx.bind(name, object);
      return;
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot bind in domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = getConfig(name);
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 1) {
        // this is the ending name
        // bind the object
        if (object instanceof String) {
          props.setProperty(parsedName.toString(), (String) object);
        } else {
          props.setObject(parsedName.toString(), object, true /* private */);
        }
        return;
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          ((Context) ctx).bind(parsedName.getSuffix(1), object);
          return;
        } else {
          throw new NotContextException(firstComponent);
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
  }
  
  public void rebind(Name name, Object object) throws NamingException {
    rebind(name.toString(), object);
  }
  
  public void rebind(String name, Object object) throws NamingException {
    bind(name, object);
  }
  
  public void unbind(Name name) throws NamingException {
    unbind(name.toString());
  }
  
  public void unbind(String name) throws NamingException {
    if (_logger.isDebugEnabled()) {
      _logger.debug(this.name + " unbind " + name);
    }
    
    if ((name == null) || (name.length() == 0)) {
      throw new InvalidNameException("Invalid name " + name);
    }
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      urlCtx.unbind(name);
      return;
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot unbind in domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = getConfig(name);
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 1) {
        // this is the ending name
        Object binding = getBinding(props, name);
        // bound ?
        if (binding != null) {
          // Remove the binding
          removeBinding(props, parsedName.toString());
        }
        return;
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          ((Context) ctx).unbind(parsedName.getSuffix(1));
          return;
        } else {
          _logger.debug(firstComponent + " is not a context");
          throw new NameNotFoundException(firstComponent);
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
  }
  
  public void rename(Name name, Name newname) throws NamingException {
    rename(name.toString(), newname.toString());
  }
  
  public void rename(String oldName, String newName) throws NamingException {
    Context urlCtx = checkURLContext(oldName);
    if (urlCtx != null) {
      urlCtx.rename(oldName, newName);
      return;
    }
    
    String oldVal = (String) lookup(oldName);
    unbind(oldName);
    bind(newName, oldVal);
  }
  
  public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
    return list(name.toString());
  }
  
  public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
    if (_logger.isDebugEnabled()) {
      _logger.debug(this.name + " list " + name);
    }
    
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      return urlCtx.list(name);
    }
    
    if (name == null) {
      throw new InvalidNameException("Cannot list null name");
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot lookup in domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = config;
      if (props == null) {
        props = getConfig(name);
      }
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 0) {
        // this is the context to list
        return new ListEnumerator("", props);
      } else if (parsedName.size() == 1) {
        Object obj = getBinding(props, name);
        if ((obj != null) && (obj instanceof Context)) {
          return ((Context) obj).list("");
        }
        return new ListEnumerator(parsedName.toString(), props);
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          return ((Context) ctx).list(parsedName.getSuffix(1));
        } else {
          _logger.debug(firstComponent + " is not a context");
          throw new NotContextException(firstComponent + " is not a context");
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
  }
  
  public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
    return listBindings(name.toString());
  }
  
  public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
    if (_logger.isDebugEnabled()) {
      _logger.debug(this.name + " listBindings " + name);
    }
    
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      return urlCtx.listBindings(name);
    }
    
    if (name == null) {
      throw new InvalidNameException("Cannot list null name");
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot lookup in domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = config;
      if (props == null) {
        props = getConfig(name);
      }
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 0) {
        // this is the context to list
        return new BindingEnumerator("", props);
      } else if (parsedName.size() == 1) {
        Object obj = getBinding(props, name);
        if ((obj != null) && (obj instanceof Context)) {
          return ((Context) obj).listBindings("");
        }
        return new BindingEnumerator(parsedName.toString(), props);
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          return ((Context) ctx).listBindings(parsedName.getSuffix(1));
        } else {
          _logger.debug(firstComponent + " is not a context");
          throw new NotContextException(firstComponent + " is not a context");
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
  }
  
  /* End of List functionality */
  
  public void destroySubcontext(Name name) throws NamingException {
    destroySubcontext(name.toString());
  }
  
  public void destroySubcontext(String name) throws NamingException {
    _logger.debug(this.name + " destroySubcontext " + name);
    
    if ((name == null) || (name.length() == 0)) {
      throw new InvalidNameException("Invalid name " + name);
    }
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      urlCtx.destroySubcontext(name);
      return;
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot delete sub-context for domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = config;
      if (props == null) {
        props = getConfig(name);
      }
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 1) {
        // this is the ending name
        Object binding = getBinding(props, name);
        // bound ?
        if (binding != null) {
          if (!(binding instanceof Context)) {
            _logger.debug(parsedName.toString() + " is not a context");
            throw new NotContextException(parsedName.toString());
          } else {
            // Remove the sub-context
            removeBinding(props, parsedName.toString());
          }
        }
        return;
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          ((Context) ctx).destroySubcontext(parsedName.getSuffix(1));
          return;
        } else {
          _logger.debug(firstComponent + " is not a context");
          throw new NameNotFoundException(firstComponent);
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
    
  }
  
  public Context createSubcontext(Name name) throws NamingException {
    return createSubcontext(name.toString());
  }
  
  public Context createSubcontext(String name) throws NamingException {
    _logger.debug(this.name + " createSubcontext " + name);
    
    if ((name == null) || (name.length() == 0)) {
      throw new InvalidNameException("Invalid name " + name);
    }
    
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      _logger.info("Delegate sub-ccontext creation to " + urlCtx);
      return urlCtx.createSubcontext(name);
    }
    
    String scheme = getURLScheme(name);
    if (scheme != null) {
      throw new NamingException("Cannot create sub-context for domain " + scheme);
    }
    Name parsedName = parser.parse(name);
    Config props;
    if (bindings == null) {
      props = config;
      if (props == null) {
        props = getConfig(name);
      }
    } else {
      props = bindings;
    }
    try {
      if (parsedName.size() == 1) {
        // this is the ending name
        // Already bound ?
        Object binding = getBinding(props, name);
        if (binding != null) {
          throw new NameAlreadyBoundException(parsedName.toString());
        }
        // Create and bind the sub-context
        Context ctx = new JndiConfig((Hashtable) env.clone(), parsedName.get(0), this, this.parser);
        props.setObject(parsedName.toString(), ctx, true /* private */);
        return ctx;
      } else {
        // go through the path, component per component
        Object ctx = null;
        String firstComponent = parsedName.get(0);
        if ("".equals(firstComponent)) {
          ctx = this;
        } else {
          ctx = getBinding(props, firstComponent);
        }
        if (ctx instanceof Context) {
          return ((Context) ctx).createSubcontext(parsedName.getSuffix(1));
        } else {
          throw new NotContextException(firstComponent);
        }
      }
    } catch (InvalidNameException e) {
      throw new NamingException(e.getMessage());
    }
  }
  
  public Object lookupLink(Name name) throws NamingException {
    return lookupLink(name.toString());
  }
  
  public Object lookupLink(String name) throws NamingException {
    return lookup(name);
  }
  
  public NameParser getNameParser(Name name) throws NamingException {
    return getNameParser(name.toString());
  }
  
  public NameParser getNameParser(String name) throws NamingException {
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      return urlCtx.getNameParser(name);
    }
    return parser;
  }
  
  public Name composeName(Name name, Name name2) throws NamingException {
    return getNameParser(name.toString()).parse(name2.toString());
  }
  
  public String composeName(String name, String prefix) throws NamingException {
    Name result = composeName(new CompositeName(name), new CompositeName(prefix));
    return result.toString();
  }
  
  public Object addToEnvironment(String name, Object object) throws NamingException {
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      return urlCtx.addToEnvironment(name, object);
    }
    if (this.env == null) {
      return null;
    } else {
      return this.env.put(name, object);
    }
  }
  
  public Object removeFromEnvironment(String name) throws NamingException {
    Context urlCtx = checkURLContext(name);
    if (urlCtx != null) {
      return urlCtx.removeFromEnvironment(name);
    }
    if (this.env == null) {
      return null;
    } else {
      return this.env.remove(name);
    }
  }
  
  public java.util.Hashtable getEnvironment() throws NamingException {
    if (this.env == null) {
      return new java.util.Hashtable();
    } else {
      return (java.util.Hashtable) this.env.clone();
    }
  }
  
  public void close() throws NamingException {
    env = null;
    
    if (listeners != null) {
      ArrayList arr = new ArrayList();
      Enumeration e = listeners.keys();
      while (e.hasMoreElements()) {
        NamingListener l = (NamingListener) e.nextElement();
        arr.add(l);
      }
      for (int i = arr.size(); --i >= 0;) {
        removeNamingListener((NamingListener) arr.get(i));
      }
    }
    listeners = null;
    cache = null;
  }
  
  public String getNameInNamespace() throws NamingException {
    Name nins = parser.parse("");
    
    JndiConfig ctx = this;
    while (ctx != null) {
      String name = ctx.getName();
      ctx = (JndiConfig) ctx.getParent();
      if ((ctx != null) && (name != null)) {
        nins.add(0, name);
      }
    }
    if (_logger.isDebugEnabled()) {
      _logger.debug("getNameInNamespace: " + nins.toString());
    }
    return nins.toString();
  }
  
  // --------------------------------------------------------------------------------
  //		javax.naming.event.EventContext
  // --------------------------------------------------------------------------------
  
  public void addNamingListener(Name target, int scope, NamingListener l) throws NamingException {
    addNamingListener(target.toString(), scope, l);
  }
  
  public void addNamingListener(String target, int scope, final NamingListener listener)
      throws NamingException {
    if (config == null) {
      // Operation not supported for now: We should used a watchdog thread 
      return;
    }
    
    if (!(listener instanceof ObjectChangeListener)) {
      return;
    }
    
    final Executor currThreadExecutor = createCurrentThreadExecutor();
    
    // Create a ConfigListener.
    
    ConfigListener cnfListener = new ConfigListener() {
      public void propertyChanged(final Config cnf, final String propertyNames[]) throws ConfigException {
        currThreadExecutor.execute(new Runnable() {
          public void run() {
            try {
              doPropertyChanged(cnf, propertyNames);
            } catch (ConfigException e) {
              e.printStackTrace();
            }
          }
        });
      }
      
      private void doPropertyChanged(Config cnf, String propertyNames[]) throws ConfigException {
        ObjectChangeListener l = (ObjectChangeListener) listener;
        
        try {
          for (int i = 0; i < propertyNames.length; i++) {
            l.objectChanged(new NamingEvent(JndiConfig.this, NamingEvent.OBJECT_CHANGED, new Binding(
                propertyNames[i], cnf.getString(propertyNames[i], null)), null, null));
          }
        }
        
        catch (Throwable t) {
          NamingException err = new NamingException("Got exception while handling property change");
          err.initCause(t);
          l.namingExceptionThrown(new NamingExceptionEvent(JndiConfig.this, err));
        }
      }
    };
    
    JndiListener jndiListener = (JndiListener) listeners.get(listener);
    
    if (jndiListener == null) {
      jndiListener = new JndiListener(listener);
      listeners.put(listener, jndiListener);
    }
    
    jndiListener.targets.add(target);
    jndiListener.cnfListeners.add(cnfListener);
    
    config.registerListener(cnfListener, target);
  }
  
  public void removeNamingListener(NamingListener listener) throws NamingException {
    if (config == null) {
      return;
    }
    
    JndiListener jndiListener = (JndiListener) listeners.remove(listeners);
    if (jndiListener == null) {
      return;
    }
    
    for (int i = jndiListener.cnfListeners.size(); --i >= 0;) {
      ConfigListener cnfListener = (ConfigListener) jndiListener.cnfListeners.get(i);
      String target = (String) jndiListener.targets.get(i);
      config.unregisterListener(cnfListener, target);
    }
  }
  
  public boolean targetMustExist() throws NamingException {
    return false;
  }
  
  // --------------------------------------------------------------------------------
  //		Inner classes
  // --------------------------------------------------------------------------------
  
  /**
   * List of Config.
   */
  private abstract class Enumerator<T> implements NamingEnumeration<T> {
    protected Config _props;
    protected Enumeration _values;
    protected T _currElement;
    
    Enumerator(String key, Config props) {
      _props = props;
      
      if (!key.endsWith("*")) {
        if (key.length() == 0) {
          key = "*";
        } else {
          if (key.charAt(key.length() - 1) == -1) {
            key += ".";
          }
          key += "*";
        }
      }
      
      _values = _props.getKeys(key);
    }
    
    public boolean hasMoreElements() {
      try {
        return hasMore();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
    
    public T nextElement() {
      try {
        return next();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
    
    public boolean hasMore() throws NamingException {
      if (_currElement == null) {
        if ((_currElement = nextEntry()) == null) {
          return false;
        }
      }
      
      return true;
    }
    
    public T next() throws NamingException {
      if (_currElement == null) {
        if ((_currElement = nextEntry()) == null) {
          throw new NamingException("No such naming element");
        }
      }
      
      T ret = _currElement;
      _currElement = null;
      return ret;
    }
    
    public void close() {
    }
    
    protected abstract T nextEntry() throws NamingException;
  }
  
  private class BindingEnumerator extends Enumerator<Binding> {
    BindingEnumerator(String key, Config props) {
      super(key, props);
    }
    
    protected Binding nextEntry() throws NamingException {
      String name = null;
      
      try {
        if (_values.hasMoreElements()) {
          name = (String) _values.nextElement();
          Object value = _props.getObject(name);
          return new Binding(name, value.getClass().getName(), value, false);
        }
        
        return null;
      }
      
      catch (ConfigException e) {
        NamingException ne = new NamingException("failed to get property " + name);
        ne.initCause(e);
        throw ne;
      }
    }
  }
  
  private class ListEnumerator extends Enumerator<NameClassPair> {
    ListEnumerator(String key, Config props) {
      super(key, props);
    }
    
    protected NameClassPair nextEntry() throws NamingException {
      String name = null;
      
      try {
        if (_values.hasMoreElements()) {
          name = (String) _values.nextElement();
          Object value = _props.getObject(name);
          return new NameClassPair(name, value.getClass().getName());
        }
        
        return null;
      }
      
      catch (ConfigException e) {
        NamingException ne = new NamingException("failed to get property " + name);
        ne.initCause(e);
        throw ne;
      }
    }
  }
  
  // --------------------------------------------------------------------------------
  //		Private methods.
  // --------------------------------------------------------------------------------
  
  private JndiConfig(JndiConfig that) {
    this(that.env);
    throwNameNotFound = Boolean.parseBoolean(System.getProperty("alcatel_lucent.jndi.throwNameNotFound",
                                                                "false"));
  }
  
  private static String getURLScheme(String str) {
    int colon_posn = str.indexOf(':');
    int slash_posn = str.indexOf('/');
    
    if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
      return str.substring(0, colon_posn);
    return null;
  }
  
  private Context checkURLContext(String name) throws NamingException {
    String scheme = getURLScheme(name);
    if (scheme != null) {
      Context ctx = NamingManager.getURLContext(scheme, this.env);
      if (ctx != null) {
        return ctx;
      }
    }
    return null;
  }
  
  private String getFile(String propertyName) {
    String file = DEFAULT_FILE;
    int firstDot = propertyName.indexOf(".");
    if (firstDot != -1) {
      file = propertyName.substring(0, firstDot) + ".properties";
    }
    return file;
  }
  
  private Config getConfig(String propertyName) throws NamingException {
    Config props = config;
    
    if (props == null) {
      String file = getFile(propertyName);
      props = (Config) cache.get(file);
      if (props == null) {
        try {
          props = new Config(file);
          cache.put(file, props);
        }
        
        catch (ConfigException e) {
          if (throwNameNotFound) {
            throw new NameNotFoundException("Cannot find " + file + " from classpath");
          } else {
            throw new InvalidNameException("Cannot find " + file + " from classpath");
          }
        }
      }
    }
    
    return props;
  }
  
  private Object getBinding(Config props, String name) {
    try {
      return props.getObject(name);
    } catch (ConfigException e) {
      return null;
    }
  }
  
  private Object removeBinding(Config props, String name) {
    try {
      return props.removeObject(name, true);
    } catch (ConfigException e) {
      return null;
    }
  }
  
  // --------------------------------------------------------------------------------
  //		Our Jndi Provider factory.
  // --------------------------------------------------------------------------------
  
  public static class FactoryBuilder implements InitialContextFactoryBuilder {
    public InitialContextFactory createInitialContextFactory(java.util.Hashtable<?, ?> env)
        throws NamingException {
      if (env != null) {
        String className = env != null ? (String) env.get(Context.INITIAL_CONTEXT_FACTORY) : null;
        if (className != null) {
          if (!className.equals(Factory.class.getName())) {
            try {
              ClassLoader cl = Thread.currentThread().getContextClassLoader();
              if (cl == null) {
                cl = getClass().getClassLoader();
              }
              return (InitialContextFactory) cl.loadClass(className).newInstance();
            }
            
            catch (Exception e) {
              NoInitialContextException ne = new NoInitialContextException("Cannot instantiate class: "
                  + className);
              ne.setRootCause(e);
              throw ne;
            }
          }
        }
      }
      
      return new Factory();
    }
  }
  
  public static class Factory implements InitialContextFactory {
    public Factory() {
    }
    
    public Context getInitialContext(java.util.Hashtable environment) {
      return new JndiConfig(environment);
    }
  }
  
  // --------------------------------------------------------------------------------
  //		A Jndi Listener.
  // --------------------------------------------------------------------------------
  
  private static class JndiListener {
    JndiListener(NamingListener listener) {
      this.listener = listener;
      this.targets = new ArrayList();
      this.cnfListeners = new ArrayList();
    }
    
    NamingListener listener;
    ArrayList cnfListeners;
    ArrayList targets;
  }
  
  /** The provider properties */
  private java.util.Hashtable env;
  
  /** The Config found in the thread context */
  private static Config config;
  
  /** Key=property file name. Value=Config */
  private Hashtable cache = new Hashtable();
  
  /** Key=NamingListener. Value=JndiListener */
  private Hashtable listeners = new Hashtable();
  
  /** which exception must be thrown when a lookup fails ? */
  private static boolean throwNameNotFound;
  
  private static Executor createCurrentThreadExecutor() {
    try {
      return (Executor) CURR_THREAD_EXECUTOR.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not create executor for RFC3263 Router", e);
    }
  }
  
  private final static Class CURR_THREAD_EXECUTOR;
  
  static {
    try {
      Utils.loadSystemProperties();
      CURR_THREAD_EXECUTOR = Class.forName(System.getProperty("com.alcatel.as.agent.CurrentThreadExecutor"));
    } catch (Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
  }
  
  private class LocalNameParser implements NameParser {
    
    Properties syntax = new Properties();
    
    public LocalNameParser() {
      syntax.put("jndi.syntax.direction", "left_to_right");
      syntax.put("jndi.syntax.separator", NS_SEPARATOR);
      syntax.put("jndi.syntax.ignorecase", "false");
    }
    
    /**
      * Parse a name into its components.
      * @param  name The non-null string name to parse.
      * @return A non-null parsed form of the name using the naming convention
      * of this parser.
      * @exception NamingException If a naming exception was encountered.
      */
    public Name parse(String name) throws NamingException {
      return new CompoundName(name, syntax);
    }
    
  }
  
}
