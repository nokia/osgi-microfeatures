// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.proxylet.deployer.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Chain;
import com.nextenso.proxylet.admin.Listener;
import com.nextenso.proxylet.admin.Param;
import com.nextenso.proxylet.admin.Proxylet;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionParser;

/**
 * This class is part of the PxletDeployer service composition.
 * WARNING: the start() / stop() methods are called by the Activator.
 */
public class ProxyletApplicationImpl implements ProxyletApplication
{
  protected final static Logger _logger =
      Logger.getLogger("as.service.pxletdeployer.ProxyletApplicationImpl");
 
  private boolean _started; // tells if our PxletDeployer has been started (all required dependencies are available
  private boolean _initDone = false; // will be set to true by the agent when the application is successfully deployed
  Bearer _bearer;
  Dictionary _globalconf;
  Observer _observer;
  /**
   * Original GroupAdmin structure
   * ProxyletSet
   * 	\_ Bearer[] for each Protocol
   * 		\_ Map<id,Listener>
   * 				\_ type
   * 				|_ classname
   * 		|_ List<Chain>
   * 			  \_ type
   * 			  |_ List<Proxylet>
   * 			  |_ Map<id, Proxylet>
   * 			  		\_ type
   * 			  		|_ name
   * 			  		|_ classname
   *
   * classname is NOT an identifier.
   * ex : MyProxylet implements ContextListener, SessionListener, RequestPxlet, ResponsePxlet... 
   */

  /**
   * reorganized structure for the needs of the API
   * first key: object type (ctx listener/session listener/req pxlet/rsp pxlet..)
   * second key: class name
   * value: instance (Listener, Proxylet)
   * type[]
   * 	\_ name[]
   * 		\_ Descriptor
   * 			\_ Listener|Proxylet (GroupAdmin descriptor object)
   * 			|_ instance (actual application object)
   */
  protected Map<String, Map<String, Descriptor>> _tree;

  // wrapper for Listener/Proxylet groupadmin descriptor and instance
  protected class Descriptor
  {
    public Object desc;
    public Object inst;

    public String toString()
    {
      return (desc != null ? desc.toString() : null) + ", inst=" + inst;
    }
  }

  CriterionParser _cparser;

  /**
   * Application scope (null if app is unscoped)
 * @param _globalconf2 
 * @param criterionParser 
   */
  protected ProxyletApplicationImpl(CriterionParser criterionParser, Dictionary globalconf)
  {
	    _cparser = criterionParser;
	    _globalconf = globalconf;
  }

  public void init(Bearer br) throws Exception
  {
    _bearer = br;
    _tree = new LinkedHashMap<String, Map<String, Descriptor>>();

    if (_bearer == null)
      return;

    // sort everything by type and name
    // proxylets..
    loadProxylets();
    // and listeners..
    loadListeners();
    // register global criterions into the parser
    for (Iterator i = _bearer.getCriterions(); i.hasNext();)
    {
      // Just parse criterion; This will register criterion in parser
      _cparser.parseCriterionValue(((com.nextenso.proxylet.admin.Criterion) i.next()).getValue());
    }

    if (_logger.isDebugEnabled())
      _logger.debug("Initial bearer structure: " + _tree);
  }

  private void loadProxylets()
  {
    for (Iterator it = _bearer.getChains(); it.hasNext();)
    {
      Chain c = (Chain) it.next();
      Iterator pi = c.getProxylets();
      while (pi.hasNext())
      {
        final Proxylet p = (Proxylet) pi.next();
        String name = (p.getName() != null ? p.getName() : p.getClassName());
        if (!p.getIsActivated())
        {
          if (_logger.isInfoEnabled())
            _logger.info("[" + c.getType() + ":" + name + "] is declared inactive. Ignore.");
          continue;
        }
        if (_logger.isDebugEnabled())
          _logger.debug("adding [" + c.getType() + ":" + name + "] from \n" + p.toXML());
        initType(c.getType()).put(name, new Descriptor()
        {
          {
            desc = p;
          }
        });
      }
    }
  }

  private void loadListeners()
  {
    for (Iterator it = _bearer.getListeners(); it.hasNext();)
    {
      final Listener l = deReference((Listener) it.next());
      String name = (l.getGivenName() != null ? l.getGivenName() : l.getClassName());
      if (name == null)
        throw new IllegalArgumentException("Invalid proxylet descriptor: no name/class found for "
            + l);
      if (!l.getIsActivated())
      {
        if (_logger.isInfoEnabled())
          _logger.info("[" + l.getType() + ":" + name + "] is declared inactive. Ignore.");
        continue;
      }
      if (_logger.isDebugEnabled())
        _logger.debug("adding [" + l.getType() + ":" + name + "] from \n" + l.toXML());
      initType(l.getType()).put(name, new Descriptor()
      {
        {
          desc = l;
        }
      });
    }
  }

  // lookup reference to previously parsed descriptor
  // (proxylets parsed before listeners)
  private Listener deReference(Listener l)
  {
    String ref = l.getReference();
    if (ref != null)
    {
      for (String type : _tree.keySet())
      {
        for (String name : getNames(type))
        {
          if (name.equals(ref))
          {
            l.setGivenName(name);
            l.setClassName(getClassName(type, name));
            return l;
          }
        }
      }
    }
    return l;
  }

  public String toString()
  {
    return getClass().getSimpleName() + (_bearer == null ? "[no bearer]" : _tree);
  }

  public void initDone() { 
    _initDone = true; 
    if (_bearer != null) {
      _logger.info("All "+ _bearer.getProtocol() + " "+ " proxylets deployed and initialized.");
    }
  }
  public boolean isInitDone() { return _initDone; }

  // /////////////////////////////////////////////
  // ProxyletApplication implementation
  // /////////////////////////////////////////////

  public String[] getListeners(String type)
  {
    // cf Context.java (loadListeners/loadCommonListener/loadListener)
    return getNames(type);
  }

  public String getListenerReference(String type, String name)
  {
    // cf HttpProxyletContext.loadListeners()
    Descriptor d = getDescriptor(type, name);
    return (d != null ? ((Listener) d.desc).getReference() : null);
  }

  public Object getListener(String type, String name)
  {
    // cf HttpProxyletContext.loadListeners()
    return getProxylet(type, name);
  }

  public String getListenerGivenName(String type, String name)
  {
    // cf HttpProxyletContext.loadListeners()
    Descriptor d = getDescriptor(type, name);
    return (d != null ? ((Listener) d.desc).getGivenName() : null);
  }

  public ClassLoader getClassLoader(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    if (d == null || d.inst == null)
      return null;
    return d.inst.getClass().getClassLoader();
  }

  public String getClassName(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? (d.desc instanceof Proxylet ? ((Proxylet) d.desc).getClassName()
                                                   : ((Listener) d.desc).getClassName()) : null);
  }

  public String getProxyAppName(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? (d.desc instanceof Proxylet ? ((Proxylet) d.desc).getSetName()
                                                   : ((Listener) d.desc).getSetName()) : null);
  }

  public String getProxyAppVersion(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? (d.desc instanceof Proxylet ? ((Proxylet) d.desc).getSetVersion()
                                                   : ((Listener) d.desc).getSetVersion()) : null);
  }

  public String[] getProxylets(String type)
  {
    // see HttpProxyletContext.loadChains()
    return getNames(type);
  }

  public Object getProxylet(String type, String name)
  {
    // see HttpProxyletContext.loadChains()
    Descriptor d = getDescriptor(type, name);
    return (d != null ? d.inst : null);
  }

  public Dictionary getProxyletParams(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    if (d != null)
    {
      Hashtable h = null;
      for (Iterator i = ((Proxylet) d.desc).getParams(); i.hasNext();)
      {
        if (h == null)
          h = new Hashtable();
        Param p = (Param) i.next();
        h.put(p.getName(), p.getValue());
      }
      return h;
    }
    return null;
  }

  public String getProxyletName(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? ((Proxylet) d.desc).getName() : null);
  }

  public String getProxyletDesc(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? ((Proxylet) d.desc).getDescription() : null);
  }

  public String getProxyletCriterionName(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? ((Proxylet) d.desc).getCriterion().getName() : null);
  }

  public String getProxyletCriterionDesc(String type, String name)
  {
    Descriptor d = getDescriptor(type, name);
    return (d != null ? ((Proxylet) d.desc).getCriterion().getDescription() : null);
  }

  public Criterion getProxyletCriterionValue(String type, String name)
  {
    try
    {
      Descriptor d = getDescriptor(type, name);
      Criterion res =
          (d != null ? _cparser.parseCriterionValue(((Proxylet) d.desc).getCriterion()
              .getValue()) : null);
      if (_logger.isDebugEnabled())
        _logger.debug("parsing criterion '"
            + ((Proxylet) d.desc).getCriterion().getValue().getTagName() + "' with parser "
            + _cparser + " returns " + res);
      return res;
    }
    catch (Exception e)
    {
      _logger.warn("Error parsing proxylet criterion " + getProxyletCriterionName(type, name), e);
    }
    return null;
  }

  public Dictionary getProperties()
  {
    return _globalconf;
  }

  // /////////////////////////////////////////////
  // implementation specific methods
  // /////////////////////////////////////////////

  public void observeProperties(Observer propertyObserver)
  {
    _observer = propertyObserver;
  }

  public boolean isEmpty()
  {
    return _bearer == null || _tree.keySet().size() == 0;
  }

  public Set<String> getTypes()
  {
    return _tree.keySet();
  }

  public String[] getNames(String type)
  {
    Map res = _tree.get(type);
    return (res != null ? (String[]) res.keySet().toArray(new String[res.keySet().size()])
                       : new String[0]);
  }

  public void setInstance(String type, String name, Object o)
  {
    if (type == null || name == null) 
    {
      _logger.info("Missing 'type' or 'name' information for registered object "
          + "[" + type + ":" + name + ":" + o + "] (possibly an inactive proxylet) : ignoring");
      return;
    }

    Descriptor d = getDescriptor(type, name);
    if (d == null)
    {
      _logger.warn("Unrecognized [" + type + ":" + name + "] : ignoring");
      return;
    }
    if (d.inst != null && _logger.isDebugEnabled())
    {
      _logger.debug("[" + type + ":" + name + "] instance already set to " + d.inst
          + ". Override.");
    }
    d.inst = o;
    if (_logger.isDebugEnabled())
      _logger.debug("[" + type + ":" + name + "] instance set to " + o);
  }

  public boolean isReady()
  {
    List awaiting = awaiting();
    if (awaiting.isEmpty()) 
    {
      return true;
    } 
    else
    {
      if (_logger.isDebugEnabled()) _logger.debug("still waiting for "+awaiting);
      return false;
    }
  }
  public List awaiting()
  {
    Vector<String> res = new Vector<String>();
    for (String type : _tree.keySet())
    {
      for (String name : getNames(type))
      {
        Descriptor d = getDescriptor(type, name);
        if (d != null && d.inst == null)
        {
	  res.add(type+":"+name);
        }
      }
    }
    return res;
  }

  // /////////////////////////////////////////////
  // Helpers
  // /////////////////////////////////////////////

  public void updateConfig(Dictionary dic, String[] properties) { //see Activator
    _globalconf = dic;
    if (_started && _observer != null) {
    	_observer.update(null, properties);
    }
  }

  protected Descriptor getDescriptor(String type, String name)
  {
    Map<String, Descriptor> names = _tree.get(type);
    return names != null ? names.get(name) : null;
  }

  private Map initType(String type)
  {
    Map names = _tree.get(type);
    if (names == null)
    {
      names = new LinkedHashMap<String, Descriptor>();
      _tree.put(type, names);
    }
    return names;
  }

  public void start() { // DependencyManager lifecycle callback
    _started = true;
  }

  public String getScope() {
    return "1";
  }
  
}
