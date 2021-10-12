// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.diagnostics.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;

/**
 * Class used to browse dependency manager components.
 */
class DMComponents {
  /**
   * Sorter used to sort components.
   */
  private static final DependencyManagerSorter SORTER = new DependencyManagerSorter();
  
  void getComponentDeclaration(List<ComponentDeclaration> list) {
    // lookup all dependency manager service components
    List<DependencyManager> managers = DependencyManager.getDependencyManagers();
    Collections.sort(managers, SORTER);
    Iterator<DependencyManager> iterator = managers.iterator();
    while (iterator.hasNext()) {
      DependencyManager manager = iterator.next();
      List<Component> complist = manager.getComponents();
      Iterator<Component> componentIterator = complist.iterator();
      while (componentIterator.hasNext()) {
        Component component = componentIterator.next();
        final org.apache.felix.dm.ComponentDeclaration decl = (org.apache.felix.dm.ComponentDeclaration) component;
        list.add(new ComponentDeclaration() {
          @Override
          public String toString() {
            return decl.toString();
          }
          
          @Override
          public int getState() {
            return decl.getState();
          }
          
          @Override
          public String getName() {
            String name = decl.getName();
            if (name.endsWith("()")) {
              return name.substring(0, name.length() - 2).trim();
            }
            return name;
          }
          
          @Override
          public ComponentDependencyDeclaration[] getComponentDependencies() {
            final org.apache.felix.dm.ComponentDependencyDeclaration[] cdds = decl.getComponentDependencies();
            if (cdds == null) {
              return null;
            }
            ComponentDependencyDeclaration[] wrapped = new ComponentDependencyDeclaration[cdds.length];
            for (int i = 0; i < wrapped.length; i++) {
              final org.apache.felix.dm.ComponentDependencyDeclaration cdd = cdds[i];
              wrapped[i] = new ComponentDependencyDeclaration() {
                @Override
                public String getType() {
                  return cdd.getType();
                }
                
                @Override
                public int getState() {
                  return cdd.getState();
                }
                
                @Override
                public String getName() {
                  return cdd.getName();
                }
              };
            }
            return wrapped;
          }
          
          @Override
          public Bundle getBundle() {
            return decl.getBundleContext().getBundle();
          }
        });
      }
    }
  }
  
  public static class DependencyManagerSorter implements Comparator<DependencyManager> {
    public int compare(DependencyManager dm1, DependencyManager dm2) {
      long id1 = dm1.getBundleContext().getBundle().getBundleId();
      long id2 = dm2.getBundleContext().getBundle().getBundleId();
      return id1 > id2 ? 1 : -1;
    }
  }
  
}
