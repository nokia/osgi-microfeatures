package com.nextenso.proxylet.engine.criterion;

// JDK
import java.util.Enumeration;

import alcatel.tess.hometop.gateways.utils.Hashtable;

public class CriterionManager {
  
  private static Hashtable criteria = new Hashtable();
  
  public static void registerCriterion(String name, Criterion criterion) {
    criteria.put(name, criterion);
  }
  
  public static Criterion removeCriterion(String name) {
    return (Criterion) criteria.remove(name);
  }
  
  public static void removeCriteria() {
    criteria.clear();
  }
  
  public static Criterion getCriterion(String name) {
    return (Criterion) criteria.get(name);
  }
  
  public static Enumeration getCriteria() {
    return criteria.keys();
  }
}
