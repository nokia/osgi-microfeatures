package com.alcatel.as.management.blueprint;

import java.util.Set;
import java.net.URL;

public interface Blueprint {
  /** original OBR (ldap) filter used to create this Blueprint */
  String getFilter() ;

  /** categories used for automatic deployments (io,agent,database,...) */
  Set<String> getCategories();
  /** creates a new Blueprint instance with the given categories */
  Blueprint setCategories(Set<String> cats);
  /** cardinality used for automatic deployments: nb of instances per host
   * (OR -1 for automatic)
   */
  int getCardinality();
  /** creates a new Blueprint instance with the given cardinality */
  Blueprint setCardinality(int c);

  /** list Blueprint's resources */
  Set<URL> getRequiredResources() ;
  Set<URL> getOptionalResources() ;
  Set<URL> getAllResources() ;

  /** used for serialization (ascii) 
   * see BlueprintFactory to reload */
  String toString() ;
}

