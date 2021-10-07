package com.nextenso.proxylet.admin;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class DefaultBearer extends Bearer {
  
  public DefaultBearer(Protocol protocol) {
    super(protocol);
    setName("");
    setDescription("");
  }
  
  /**
   * Gets the list of supported chain types.
   * @return An iterator to an empty Set
   */
  public Iterator getChainTypes() {
    Set s = new TreeSet();
    return s.iterator();
  }
  
  /**
   * Gets the list of supported listener types.
   * @return An iterator to an empty Set
   */
  public Iterator getListenerTypes() {
    Set s = new TreeSet();
    return s.iterator();
  }
  
  /**
   * Gets the list of supported criterion types.
   * @return The list of supported listener types (Strings).
   */
  public Iterator getCriterionTypes() {
    Set s = new TreeSet();
    return s.iterator();
  }
  
}
