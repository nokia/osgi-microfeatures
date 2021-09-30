package com.nextenso.proxylet.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Proxylet Chain class.
 */

public class Chain extends AdminObject implements XMLable {
  String _type = null;
  List _orderedProxylets = null;
  Map _proxylets = null;
  
  /**
   * Builds a new param.
   * @param type The type of the parameter
   */
  public Chain(String type) {
    super();
    setType(type);
    _orderedProxylets = new ArrayList();
    _proxylets = new HashMap();
    //	log ("Constructor: new Chain=" + type);
  }
  
  private Chain() {
  };
  
  /**
   * Gets the type of this param.
   * @return The type.
   */
  public final String getType() {
    return _type;
  }
  
  /**
   * Sets the type.
   * @param type The type.
   */
  public final void setType(String type) {
    _type = type;
  }
  
  /**
   * Gets the proxylet list of this chain.
   * @return The proxylet list of this chain.
   */
  public Iterator getProxylets() {
    Iterator it = _orderedProxylets.iterator();
    return it;
  }
  
  /**
   * Gets a proxylet by its name.
   * @param identifier The identifier of the searched proxylet
   * @return The Proxylet or null if not found.
   */
  public Proxylet getProxylet(String identifier) {
    Proxylet res = null;
    if (identifier != null)
      res = (Proxylet) _proxylets.get(identifier);
    return res;
  }
  
  /**
   *
   */
  public void proxyletUp(String identifier) {
    Proxylet p = getProxylet(identifier);
    if (p == null)
      return;
    int i = _orderedProxylets.indexOf(p);
    if (i > 0) {
      Object o = _orderedProxylets.get(i - 1);
      _orderedProxylets.set(i - 1, p);
      _orderedProxylets.set(i, o);
    }
  }
  
  /**
   *
   */
  public void proxyletDown(String identifier) {
    Proxylet p = getProxylet(identifier);
    if (p == null)
      return;
    int i = _orderedProxylets.indexOf(p);
    if (i >= 0 && i < _orderedProxylets.size() - 1) {
      Object o = _orderedProxylets.get(i + 1);
      _orderedProxylets.set(i + 1, p);
      _orderedProxylets.set(i, o);
    }
  }
  
  /**
   * Adds a proxylets at the end of the chain.
   * @param proxylet The proxylet to be added.
   */
  public void addProxylet(Proxylet proxylet) throws NullPointerException {
    if (proxylet == null || proxylet.getIdentifier() == null)
      throw new NullPointerException("empty proxylet");
    
    if (_proxylets.get(proxylet.getIdentifier()) != null) {
      return; //ignore pxlet already present
    }
    
    _orderedProxylets.add(proxylet);
    _proxylets.put(proxylet.getIdentifier(), proxylet);
  }
  
  /**
   * Removes a proxylets of the chain.
   * @param proxylet The proxylet to be removed.
   */
  public void removeProxylet(Proxylet proxylet) {
    if (proxylet == null || proxylet.getIdentifier() == null)
      return;
    
    _orderedProxylets.remove(proxylet);
    _proxylets.remove(proxylet.getIdentifier());
  }
  
  /**
   * Removes Proxylet Set Elements of given setId.
   * @param setId the set identifier
   */
  public void removeProxyletSetElements(String setId) {
    Iterator proxyletIt = getProxylets();
    while (proxyletIt.hasNext()) {
      Proxylet aProxylet = (Proxylet) proxyletIt.next();
      if ((aProxylet.getSetID() != null) && (aProxylet.getSetID().equals(setId))) {
        proxyletIt.remove();
        removeProxylet(aProxylet);
      }
    }
  }
  
  /**
   * Adds Proxylet Set Elements of given chain.
   * @param chain the chain containing proxylet set elements to add.
   */
  public void addProxyletSetElements(Chain chain) {
    Iterator proxyletAddIt = chain.getProxylets();
    while (proxyletAddIt.hasNext()) {
      Proxylet aProxyletToAdd = (Proxylet) proxyletAddIt.next();
      addProxylet(aProxyletToAdd);
    }
  }
  
  /**
   * Set Proxylet Set Elements to the given setId.
   * @param setId the set identifier
   */
  public void setProxyletSetElements(String setId) {
    Iterator proxyletIt = getProxylets();
    while (proxyletIt.hasNext()) {
      Proxylet aProxylet = (Proxylet) proxyletIt.next();
      aProxylet.setSetID(setId);
    }
  }
  
  /**
   * Gets a new instance of Proxylet.
   * <BR>This method can be redefined by inherited classes if needed.
   * @return The new proxylet object.
   */
  public Proxylet getProxyletInstance() {
    return new Proxylet();
  }
  
  /**
   * Returns the index of a proxylet in the _orderedProxylets list.
   * @param The Proxylet instance
   * @return The index of this Proxylet instance in the _orderedProxylets list.
   */
  public int indexOf(String proxyletName) {
    Proxylet proxylet = (Proxylet) _proxylets.get(proxyletName);
    return _orderedProxylets.indexOf(proxylet);
  }
  
  /**
   * Sets the (ordered) proxylets list.
   * @param  The proxylet list
   */
  public void setPxletsList(List pxletsList) {
    _orderedProxylets = pxletsList;
  }
  
  /**
   * Gets the (ordered) proxylets list.
   * @return  The proxylet list
   */
  public List getPxletsList() {
    return _orderedProxylets;
  }
  
  /**
   * Apply constraints to this chain.
   */
  public void applyConstraints() throws ConstraintException {
    Iterator pxletsIt = getProxylets();
    ArrayList appliedConstr = new ArrayList();
    
    while (pxletsIt.hasNext()) {
      Proxylet pxlet = (Proxylet) pxletsIt.next();
      
      Iterator constrIt = pxlet.getConstraints();
      while (constrIt.hasNext()) {
        Constraint constr = (Constraint) constrIt.next();
        
        if (!(appliedConstr.contains(constr))) {
          if (constr.check(this)) {
            appliedConstr.add(constr);
          } else {
            //We apply the constraint.
            constr.apply(this);
            pxletsIt = getProxylets();
            
            //We check that all constraints in appliedConstr list are still applied.
            boolean test = true;
            Iterator appliedConstrIt = appliedConstr.iterator();
            
            while (appliedConstrIt.hasNext()) {
              test = ((Constraint) appliedConstrIt.next()).check(this);
              if (!test) {
                throw new ConstraintException(
                    ConstraintException.IMPOSSIBLE_CONSTRAINT,
                    "*** Chain *** applyConstraints() *** A circular constraint has been detected. Use the Proxlet Developer GUI to remove the inapplicable constraint.");
              }
            }
            
            //Then we add the recently applied constraint to the appliedConstr list.
            appliedConstr.add(constr);
          }
        }
      }
    }
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      String nn = n.getNodeName().trim();
      if ("proxylet".equals(nn)) {
        Proxylet px = getProxyletInstance();
        px.setNode(n);
        addProxylet(px);
      }
    }
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("\n  <" + getTagName() + ">\n");
    
    Iterator it = getProxylets();
    while (it.hasNext()) {
      XMLable p = (XMLable) it.next();
      res.append(p.toXML());
    }
    res.append("\n  </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return getType();
  }
  
}
