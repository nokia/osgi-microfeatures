package com.alcatel.as.service.diagnostics.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Class used to browse declarative service components.
 */
class DSComponents {
  private final ScrService _scr;
  private final ConfigurationAdmin _cm;
  
  @SuppressWarnings("serial")
  private Set<String> ignoredProperties = new HashSet<String>() {
    {
      add("component.name");
      add("component.id");
      add("composite.name");
      add("component.factory");
      add("service.description");
      add("service.pid");
      add("service.vendor");
    }
  };
  
  DSComponents(ScrService scr, ConfigurationAdmin cm) {
    _scr = scr;
    _cm = cm;
  }
  
  void getComponentDeclaration(List<ComponentDeclaration> list) {
    Component[] components = _scr.getComponents();
    
    if (components != null && components.length > 0) {
      for (Component component : components) {
        final Component c = component;
        
        // Ignore disable components
        if (c.getState() == Component.STATE_DISABLED) {
          continue;
        }
        
        // Ignore any enabled factory component
        if (c.getState() == Component.STATE_FACTORY) {
          // this factory is enabled and active, no need to display it.
          continue;
        }
        
        // Wrap this DS component in our ComponentDeclaration interface.
        ComponentDeclaration cd = new ComponentDeclaration() {
          @Override
          public int getState() {
            return (c.getState() == Component.STATE_ACTIVE || c.getState() == Component.STATE_REGISTERED) ? ComponentDeclaration.STATE_REGISTERED
                : ComponentDeclaration.STATE_UNREGISTERED;
          }
          
          @Override
          public String getName() {
            // We must return same DM name format: "list of provided interface(service props)" or "implem class name"
            String[] services = c.getServices();
            StringBuilder sb = new StringBuilder();
            if (services != null) {
              for (String s : services) {
                sb.append(s);
                sb.append(",");
              }
              sb.setLength(sb.length() - 1);
              getComponentProperties(c, sb);
            } else {
              sb.append(c.getClassName());
            }
            return sb.toString();
          }
          
          /**
           * Wrap all the DS references of this component in our ComponentDependencyDeclaration interface.        
           * @throws InvalidSyntaxException 
           * @throws IOException 
           */
          @Override
          public ComponentDependencyDeclaration[] getComponentDependencies() throws IOException,
              InvalidSyntaxException {
            List<ComponentDependencyDeclaration> cdds = new ArrayList();
            
            // If the component is not active and has a required configuratin policy, we create
            // a ComponentDependencyDeclaration of type "configuration".
            if (c.getState() != Component.STATE_ACTIVE && "require".equals(c.getConfigurationPolicy())) {
              String pid = c.getName();
              if (_cm.listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")") == null) {
                ComponentDependencyDeclaration conf = new ComponentDependencyDeclaration() {
                  @Override
                  public String getType() {
                    return ServiceDiagnosticsImpl.CONFIGURATION;
                  }
                  
                  @Override
                  public int getState() {
                    return c.getState() == Component.STATE_ACTIVE ? ComponentDependencyDeclaration.STATE_AVAILABLE_REQUIRED
                        : ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED;
                  }
                  
                  @Override
                  public String getName() {
                    return c.getName(); // the component name is also the configuration pid.
                  }
                };
                cdds.add(conf);
              }
            }
            
            // Wrap this DS Reference in our ComponentDependencyDeclaration interface.
            Reference[] refs = c.getReferences();
            if (refs != null) {
              for (int i = 0; i < refs.length; i++) {
                final Reference ref = refs[i];
                
                cdds.add(new ComponentDependencyDeclaration() {
                  @Override
                  public String getType() {
                    return ServiceDiagnosticsImpl.SERVICE;
                  }
                  
                  @Override
                  public int getState() {
                    if (ref.isSatisfied()) {
                      return ref.isOptional() ? ComponentDependencyDeclaration.STATE_AVAILABLE_OPTIONAL
                          : ComponentDependencyDeclaration.STATE_AVAILABLE_REQUIRED;
                    }
                    return ref.isOptional() ? ComponentDependencyDeclaration.STATE_UNAVAILABLE_OPTIONAL
                        : ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED;
                  }
                  
                  @Override
                  public String getName() {
                    StringBuilder sb = new StringBuilder();
                    sb.append(ref.getServiceName());
                    if (ref.getTarget() != null) {
                      sb.append(" ");
                      sb.append(ref.getTarget());
                    }
                    return sb.toString();
                  }
                });
              }
            }
            
            return cdds.toArray(new ComponentDependencyDeclaration[cdds.size()]);
          }
          
          @Override
          public Bundle getBundle() {
            return c.getBundle();
          }
        };
        
        list.add(cd);
      }
    }
  }
  
  private String getComponentProperties(Component c, StringBuilder sb) {
    Dictionary props = c.getProperties();
    Enumeration<String> e = props.keys();
    if (props != null) {
      boolean first = true;
      int count = 0;
      
      while (e.hasMoreElements()) {
        String key = e.nextElement();
        if (ignoredProperties.contains(key)) {
          continue;
        }
        if (key.endsWith(".target")) {
          int i = key.lastIndexOf(".target");
          String s = key.substring(0, i);
          if (getReference(c, s) != null) {
            continue;
          }
        }
        if (first) {
          sb.append("(");
        } else {
          sb.append(",");
        }
        first = false;
        count ++;
        sb.append(key).append("=").append(props.get(key));
      }
      if (count > 0) {
        sb.append(")");
      }
    }
    return sb.toString();
  }
  
  private Reference getReference(Component c, String name) {
    Reference[] refs = c.getReferences();
    if (refs == null) {
      return null;
    }
    for (Reference ref : refs) {
      if (ref.getName().equals(name)) {
        return ref;
      }
    }
    return null;
  }

  public void diag(PrintStream out) {
    // Dump all components having a configuration-policy=require and no @Modified callbacks
    List<String> suspectsWithConfRequiredWithoutModified = new ArrayList<>();
    
    Component[] components = _scr.getComponents();
    
    if (components != null) {
      for (Component component : components) {
        if ("require".equals(component.getConfigurationPolicy())) {
          if (component.getModified() == null) {
            suspectsWithConfRequiredWithoutModified.add(component.getClassName());
          }
        }
      }
    }
    
    if (suspectsWithConfRequiredWithoutModified.size() == 0) {
      // out.println("No DS components found with configuration required and without @Modified callback (OK).");
    } else {
      out.println("Found " + suspectsWithConfRequiredWithoutModified.size()
          + " DS components with configuration required and without @Modified callback found");
      for (String className : suspectsWithConfRequiredWithoutModified) {
        out.println(" * " + className);
      }
    }
    
    // Dump all components having a *non-dynamic" Dependency on a Dictionary with a service-pid.
    Map<String, List<String>> suspectsWithNonDynamicDict = new HashMap<>();
    if (components != null) {
      for (Component component : components) {
        Reference[] refs = component.getReferences();
        if (refs != null) {
          for (Reference ref : refs) {
            String refTarget = null;
            if (ref.getServiceName().equals(Dictionary.class.getName()) && ref.isStatic()
                && ((refTarget = getTarget(component, ref)) != null) 
                && refTarget.indexOf("service.pid") != -1
                && refTarget.indexOf("(service.pid=system)") == -1) {
              List<String> list = suspectsWithNonDynamicDict.get(component.getClassName());
              if (list == null) {
                list = new ArrayList<>();
                suspectsWithNonDynamicDict.put(component.getClassName(), list);
              }
              
              list.add(ref.getName());
            }
          }
        }
      }
    }
    
    if (suspectsWithNonDynamicDict.size() == 0) {
      // out.println("No DS components found with a non-dynamic Reference on a Dictionary (OK).");
    } else {
      out.println("Found " + suspectsWithNonDynamicDict.size()
          + " DS components with a non-dynamic Reference on a Dictionary");
      for (Map.Entry<String, List<String>> e : suspectsWithNonDynamicDict.entrySet()) {
        out.println(" * " + e.getKey() + ":" + e.getValue());
      }
    }
    
    // Dump all components having a Dependency on a Dictionary, but without a target filter
    Map<String, List<String>> suspectsWithDictionaryNoTarget = new HashMap<>();
    if (components != null) {
      for (Component component : components) {
        Reference[] refs = component.getReferences();
        if (refs != null) {
          for (Reference ref : refs) {
            if (ref.getServiceName().equals(Dictionary.class.getName()) && getTarget(component, ref) == null) {
              List<String> list = suspectsWithDictionaryNoTarget.get(component.getClassName());
              if (list == null) {
                list = new ArrayList<>();
                suspectsWithDictionaryNoTarget.put(component.getClassName(), list);
              }
              
              list.add(ref.getName());
            }
          }
        }
      }
    }
    
    if (suspectsWithDictionaryNoTarget.size() == 0) {
      // out.println("No DS components found with a Ref on a Dictionary without a target (OK).");
    } else {
      out.println("Found " + suspectsWithDictionaryNoTarget.size()
          + " DS components found with a Ref on a Dictionary without a target");
      for (Map.Entry<String, List<String>> e : suspectsWithDictionaryNoTarget.entrySet()) {
        out.println(" * " + e.getKey() + ":" + e.getValue());
      }
    }
  }
  
  private String getTarget(Component component, Reference ref) {
    String target = ref.getTarget();
    if (target == null) {
      Dictionary props = component.getProperties();
      if (props != null) {
        String targetFilter = (String) props.get(ref.getName() + ".target");
        if (targetFilter != null) {
          return targetFilter;
        }
      }
    }
    return target;
  }
}
