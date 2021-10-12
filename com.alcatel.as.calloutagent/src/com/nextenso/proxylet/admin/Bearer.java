// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Bearer class.
 */
@SuppressWarnings("rawtypes")
public abstract class Bearer extends AdminObject implements XMLable {
  
  public interface Factory {
    Bearer newBearer(Node n) throws Exception;
    
    Protocol getProtocol();
  }
  
  public final static String ALL = "all";
  public final static String AND = "and";
  public final static String DATE = "date";
  public final static String DAY = "day";
  public final static String FROM = "from";
  public final static String MESSAGE_ATTR = "message-attr";
  public final static String MONTH = "month";
  public final static String NOT = "not";
  public final static String OR = "or";
  public final static String REFERENCE = "reference";
  public final static String TIME = "time";
  public final static String UNTIL = "until";
  
  public final static String CONTEXT_LISTENER = "context-listener";
  
  private Protocol _protocol = null;
  
  private Map _listeners = null;
  private Map _chains = null;
  private List _orderedChains = null;
  private Map _criterions = null;
  private List<Criterion> _orderedCriterions = null;
  private String _name = null;
  private String _description = null;
  private String _groupName = null;
  
  /**
   * Builds a new bearer.
   * @param protocol The protocol supported by this bearer.
   * @param groupName The group where this bearer is deployed.
   */
  protected Bearer(Protocol protocol) {
    super();
    _protocol = protocol;
    _listeners = new HashMap();
    
    _chains = new HashMap();
    _orderedChains = new ArrayList();
    Iterator chIt = getChainTypes();
    while (chIt.hasNext()) {
      addChain(getChainInstance((String) chIt.next()));
    }
    
    _criterions = new HashMap();
    _orderedCriterions = new ArrayList<Criterion>();
  }
  
  /**
   * Gets the name.
   * @return The name.
   */
  public final String getName() {
    return _name;
  }
  
  /**
   * Sets the name.
   * @param name The name.
   */
  public final void setName(String name) {
    _name = name;
  }
  
  /**
   * Gets the description.
   * @return The description.
   */
  public final String getDescription() {
    return _description;
  }
  
  /**
   * Sets the description.
   * @param description The description.
   */
  public final void setDescription(String description) {
    if (description == null || "".equals(description.trim()))
      _description = null;
    else
      _description = description;
  }
  
  /**
   * Gets the supported protocol by this bearer.
   * @return The protocol.
   */
  public final Protocol getProtocol() {
    return _protocol;
  }
  
  /**
     Gets the name of the group where this bearer is deployed.
     @return the group name.
  */
  public String getGroupName() {
    return _groupName;
  }
  
  /**
     Sets the name fo the group where this bearer is deployed.
  */
  public void setGroupName(String groupName) {
    _groupName = groupName;
  }
  
  /**
   * Gets the list of supported chain types.
   * @return The list of supported chain types (Strings).
   */
  public abstract Iterator getChainTypes();
  
  /**
   * Gets the list of supported listener types.
   * @return The list of supported listener types (Strings).
   */
  public abstract Iterator getListenerTypes();
  
  /**
   * Gets the list of supported criterion types.
   * @return The list of supported criterion types.
   */
  public Iterator getCriterionTypes() {
    Iterator it = null;
    return it;
  }
  
  /**
   * Gets a new instance of Criterion.
   * <BR>This method can be redefined by inherited classes if needed.
   * @param type The type of the criterion.
   * @return The new criterion object.
   */
  public Criterion getCriterionInstance() {
    return new Criterion();
  }
  
  /**
   * Gets the list of the criterions.
   * @return The list of the criterions.
   */
  public Iterator<Criterion> getCriterions() {
    return _orderedCriterions.iterator();
  }
  
  /**
   * Removes all criterions.
   */
  public void clearCriterions() {
    _orderedCriterions.clear();
    _criterions.clear();
  }
  
  /**
   * Removes a criterion.
   * @param criterion The criterion to be removed.
   */
  public void removeCriterion(Criterion criterion) {
    if (criterion == null || criterion.getIdentifier() == null)
      return;
    _orderedCriterions.remove(criterion);
    _criterions.remove(criterion.getIdentifier());
  }
  
  /**
   * Modify a criterion.
   * <BR>If the criterion does not exist, it is added. 
   * <BR>If the criterion value is null, it is removed.
   * @param criterion The criterion to be modified.
   */
  public void setCriterion(Criterion criterion) {
    if (criterion == null || criterion.getIdentifier() == null)
      return;
    
    String identifier = criterion.getIdentifier();
    Criterion c = (Criterion) _criterions.get(identifier);
    
    if (c != null && criterion.getValue() == null) {
      _criterions.put(identifier, null);
      try {
        _orderedCriterions.remove(c);
      } catch (Exception e) {
      }
    } else {
      _criterions.put(identifier, criterion);
      if (c != null) {
        int i = _orderedCriterions.indexOf(c);
        if (i >= 0)
          _orderedCriterions.set(i, criterion);
      } else {
        _orderedCriterions.add(criterion);
      }
    }
  }
  
  /**
   * Adds a listener.
   * @param listener The listener to be added.
   */
  public void addListener(Listener listener) {
    if (_listeners.get(listener.getIdentifier()) == null)
      _listeners.put(listener.getIdentifier(), listener);
  }
  
  /**
   * Removes a listener.
   * @param listener The listener to be removed.
   */
  public void removeListener(Listener listener) {
    _listeners.remove(listener.getIdentifier());
  }
  
  /**
   * Gets the list of the listeners.
   * @return The list of the listeners.
   */
  public Iterator getListeners() {
    return _listeners.values().iterator();
  }
  
  /**
   * Removes all listeners
   */
  public void clearListeners() {
    _listeners.clear();
  }
  
  /**
   * Gets the listener by its identifier.
   * @param id the identifier of the searched listener
   * @return The listener.
   */
  public Listener getListener(String id) {
    Listener res = (Listener) _listeners.get(id);
    return res;
  }
  
  /**
   * Gets a new instance of Listener.
   * <BR>This method can be redefined by inherited classes if needed.
   * @param type The type of the listener.
   * @return The new listener object.
   */
  public Listener getListenerInstance(String type) {
    return new Listener(type);
  }
  
  /**
   * Adds a chain.
   * @param chain The chain to be added.
   */
  public void addChain(Chain chain) {
    String type = chain.getType();
    Chain c = (Chain) _chains.get(type);
    
    _chains.put(type, chain);
    if (c != null) {
      int i = _orderedChains.indexOf(c);
      if (i >= 0) {
        _orderedChains.set(i, chain);
      }
    } else {
      _orderedChains.add(chain);
    }
  }
  
  /**
   * Gets the list of the chains.
   * @return The list of the chains.
   */
  public Iterator getChains() {
    return _orderedChains.iterator();
  }
  
  /**
   * Gets a new instance of Chain.
   * <BR>This method can be redefined by inherited classes if needed.
   * @param type The type of the chain.
   * @return The new chain object.
   */
  public Chain getChainInstance(String type) {
    return new Chain(type);
  }
  
  /**
   * Gets the chain by its type.
   * @param type the type of the searched chain
   * @return The chain.
   */
  public Chain getChain(String type) {
    Chain res = (Chain) _chains.get(type);
    return res;
  }
  
  /**
   * Removes Proxylet Set Elements of given setId.
   * @param setId the set identifier 
   */
  public void removeProxyletSetElements(String setId) {
    Iterator listenerIt = getListeners();
    while (listenerIt.hasNext()) {
      Listener aListener = (Listener) listenerIt.next();
      if ((aListener.getSetID() != null) && (aListener.getSetID().equals(setId))) {
        listenerIt.remove();
        removeListener(aListener);
      }
    }
    
    Iterator criterionIt = getCriterions();
    while (criterionIt.hasNext()) {
      Criterion aCriterion = (Criterion) criterionIt.next();
      if ((aCriterion.getSetID() != null) && (aCriterion.getSetID().equals(setId))) {
        criterionIt.remove();
        removeCriterion(aCriterion);
      }
    }
    
    Iterator chainIt = getChains();
    while (chainIt.hasNext()) {
      Chain aChain = (Chain) chainIt.next();
      aChain.removeProxyletSetElements(setId);
    }
  }
  
  /**
   * Adds Proxylet Set Elements of given bearer.
   * @param bearer the bearer containing proxylet set elements to add.
   */
  public void addProxyletSetElements(Bearer bearer) {
    Iterator listenerAddIt = bearer.getListeners();
    while (listenerAddIt.hasNext()) {
      Listener aListener = (Listener) listenerAddIt.next();
      this.addListener(aListener);
    }
    
    Iterator criterionAddIt = bearer.getCriterions();
    while (criterionAddIt.hasNext()) {
      Criterion aCriterion = (Criterion) criterionAddIt.next();
      this.setCriterion(aCriterion);
    }
    
    Iterator chainAddIt = bearer.getChains();
    
    while (chainAddIt.hasNext()) {
      Chain aChainToAdd = (Chain) chainAddIt.next();
      if (this.getChain(aChainToAdd.getType()) != null) {
        // if chain already exists, add the elements of the chain to the existing chain
        this.getChain(aChainToAdd.getType()).addProxyletSetElements(aChainToAdd);
      } else {
        addChain(aChainToAdd);
      }
    }
  }
  
  /**
   * Set Proxylet Set Elements of the bearer to the given setId.
   * @param setId the set identifier 
   */
  public void setProxyletSetElements(String setId) {
    
    Iterator listenerIt = getListeners();
    while (listenerIt.hasNext()) {
      Listener aListener = (Listener) listenerIt.next();
      aListener.setSetID(setId);
    }
    Iterator criterionIt = getCriterions();
    while (criterionIt.hasNext()) {
      Criterion aCriterion = (Criterion) criterionIt.next();
      aCriterion.setSetID(setId);
    }
    
    Iterator chainIt = getChains();
    while (chainIt.hasNext()) {
      Chain aChain = (Chain) chainIt.next();
      aChain.setProxyletSetElements(setId);
    }
  }
  
  /**
   * Apply constraints to this bearer. 
   */
  public void applyConstraints() throws ConstraintException {
    Iterator chainIt = getChains();
    Chain chain = null;
    
    while (chainIt.hasNext()) {
      chain = (Chain) chainIt.next();
      chain.applyConstraints();
    }
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    // gets the abstract parts
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      
      String nn = n.getNodeName().trim();
      String nv = getNodeValue(n);
      //	    log ("setNode: sub node=" + nn + " (" + nv + ")");
      
      // get name
      if ("name".equals(nn)) {
        setName(nv);
      } else if ("description".equals(nn)) {
        setDescription(nv);
      } else if ("criterion".equals(nn)) {
        Criterion cr = getCriterionInstance();
        cr.setNode(n);
        setCriterion(cr);
      }
      
      // get chains
      Iterator it = getChainTypes();
      while (it.hasNext()) {
        String chainName = (String) it.next();
        if (chainName.equals(nn)) {
          Chain chain = getChainInstance(nn);
          chain.setNode(n);
          addChain(chain);
        }
      }
      
      // get listeners
      it = getListenerTypes();
      while (it.hasNext()) {
        String listenerName = (String) it.next();
        if (listenerName.equals(nn)) {
          Listener listener = getListenerInstance(nn);
          listener.setNode(n);
          addListener(listener);
        }
      }
    }
  }
  
  /**
   * Gets the XML representation of this bearer.
   * @return The XML representation of this bearer.
   */
  public String toXML() {
    StringBuffer buffer = new StringBuffer();
    buffer.append(toXMLHeader());
    buffer.append(toXMLSpecific());
    buffer.append(toXMLFooter());
    return buffer.toString();
  }
  
  /**
   * Gets the specific part of XML representation.
   * @return The specific part of XML representation.
   */
  public String toXMLSpecific() {
    return "";
  }
  
  /**
   * Gets the XML header common to all bearers.
   * @return The XML header.
   */
  public String toXMLHeader() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
    buffer.append("<!DOCTYPE " + getTagName() + " PUBLIC \"" + getProtocol().getPublicID()
        + "\" \"http://www.nextenso.com/\">\n\n");
    buffer.append("<" + getTagName() + ">\n");
    buffer.append("  <name>" + getName() + "</name>\n");
    if (getDescription() != null)
      buffer.append("  <description>" + getDescription() + "</description>\n");
    
    // Criterions
    Iterator it = getCriterions();
    while (it.hasNext()) {
      XMLable cr = (XMLable) it.next();
      if (cr != null) {
        buffer.append(cr.toXML());
      }
    }
    
    // Chains
    it = getChains();
    while (it.hasNext()) {
      XMLable chain = (XMLable) it.next();
      if (chain != null) {
        buffer.append(chain.toXML());
      }
    }
    
    // Listeners
    it = getListeners();
    while (it.hasNext()) {
      XMLable listener = (XMLable) it.next();
      if (listener != null) {
        buffer.append(listener.toXML());
      }
    }
    
    return buffer.toString();
  }
  
  /**
   * Gets the XML footer common to all bearers.
   * @return The XML footer.
   */
  public String toXMLFooter() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("</" + getTagName() + ">\n");
    return buffer.toString();
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return "context";
  }
}
