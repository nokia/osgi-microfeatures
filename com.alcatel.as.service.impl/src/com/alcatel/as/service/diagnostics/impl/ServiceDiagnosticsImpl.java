package com.alcatel.as.service.diagnostics.impl;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.felix.scr.ScrService;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.WireAdmin;

import com.alcatel.as.service.diagnostics.ServiceDiagnostics;

/**
 * This service is a Gogo shell and allows to make some diagnostics about missing OSGI services.
 */
@Descriptor("ASR OSGi services Diagnostics")
public class ServiceDiagnosticsImpl implements ServiceDiagnostics
{
  /**
   * Max size of service properties to display. If string representing service props is greater than this specified size,
   * then the service properties are truncated. Use "allprops" option to display full service properties.
   */
  private final static int MAX_PROPERTIES_SIZE = 80;
  
  /**
   * Helper class used to browse dependency manager components.
   */
  private volatile DMComponents _dm;
  
  /**
   * Helper class used to browse dependency manager components.
   */
  private volatile DSComponents _ds;

  /**
   * Bundle context used to create OSGI filters.
   */
  private volatile BundleContext m_context;

  private volatile ScrService _scr;

  private volatile ConfigurationAdmin _cm;
    
  /**
   * Name of a specific gogo shell variable, which may be used to configure an OSGI filter, normally
   * passed to the "dm services" option. It is used to display only some service providing components
   * matching the given filter. The filter can contain an "objectClass" option.
   * Example: 
   *   g! dependencymanager.services="(protocol=http)"
   *   g! dependencymanager.services="(&(objectClass=foo.Bar)(protocol=http))"
   */
  private final static String ENV_SERVICES = "dependencymanager.services";
  
  /**
   * Name of a specific gogo shell variable, which may be used to configure a filter on the
   * component implementation class name.
   * The value of this shell variable may contain multiple regex (space separated), and each regex can
   * be negated using "!".
   * Example: g! dependencymanager.components="foo.bar.* ga.bu.zo.*"
   */
  private final static String ENV_COMPONENTS = "dependencymanager.components";
  
  /**
   * Name of a specific gogo shell variable, which may be used to configure "compact" mode.
   * Example: g! dependencymanager.compact=true
   */
  private final static String ENV_COMPACT = "dependencymanager.compact";
  
  /**
   * Constant used by the wtf command, when listing missing services.
   */
  public static final String SERVICE = "service";
  
  /**
   * Constant used by the wtf command, when listing missing configurations.
   */
  public static final String CONFIGURATION = "configuration";

  public void setScrService(ScrService scr) {
      _scr = scr;
  }
  
  public void setConfigAdmin(ConfigurationAdmin cm) {
      _cm = cm;
  }

  void start() {
    _dm = new DMComponents();
    _ds = new DSComponents(_scr, _cm);
  }
  
  @Descriptor("List ASR Service Components")
  public void list(CommandSession session,
                   
                   @Descriptor("Hides component dependencies") 
                   @Parameter(names = { "nodeps", "nd" }, presentValue = "true", absentValue = "false") 
                   boolean nodeps,
                   
                   @Descriptor("Displays components using a compact form") 
                   @Parameter(names = { "compact", "cp" }, presentValue = "true", absentValue = "") 
                   String compact,
                   
                   @Descriptor("Only displays unavailable components") 
                   @Parameter(names = { "notavail", "na" }, presentValue = "true", absentValue = "false") 
                   boolean notavail,
                   
                   @Descriptor("Displays components statistics") 
                   @Parameter(names = { "stats", "st" }, presentValue = "true", absentValue = "false") 
                   boolean stats,
                   
                   @Descriptor("OSGi filter used to filter some service properties") 
                   @Parameter(names = { "services", "s" }, absentValue = "") 
                   String services,
                   
                   @Descriptor("Regex(s) used to filter on component implementation class names (comma separated, can be negated using \"!\" prefix)") 
                   @Parameter(names = { "components", "c" }, absentValue = "") 
                   String components,
                   
                   @Descriptor("List of bundle ids or bundle symbolic names to display (comma separated)") 
                   @Parameter(names = {"bundleIds", "bid", "bi", "b" }, absentValue = "") 
                   String bundleIds,

                   @Descriptor("Display services with full service properties. By default, service properties are truncated if they are too large (> 80 chars)") 
                   @Parameter(names = { "allprops", "ap", }, presentValue = "true", absentValue = "false") 
                   boolean displayAllProps
          )
    {    
      boolean comp = Boolean.parseBoolean(getParam(session, ENV_COMPACT, compact));
      services = getParam(session, ENV_SERVICES, services);
      String[] componentsRegex = getParams(session, ENV_COMPONENTS, components);
      list(System.out, nodeps, comp, notavail, stats, services, componentsRegex, bundleIds, displayAllProps);
    }
  
  private void list(PrintStream out, boolean nodeps, boolean comp, boolean notavail, boolean stats, String services,
                      String[] componentsRegex, String bundleIds, boolean displayAllProps      ) 
  {
    try {      
      ArrayList<String> bids = new ArrayList<String>(); // list of bundle ids or bundle symbolic names
      
      // Parse services fitler
      Filter servicesFilter = null;
      try {
        if (services != null) {
          servicesFilter = m_context.createFilter(services);
        }
      } catch (InvalidSyntaxException e) {
        out.println("Invalid services OSGi filter: " + services);
        e.printStackTrace(out);
        return;
      }
      
      // Parse and check bundleIds option
      StringTokenizer tok = new StringTokenizer(bundleIds, ", ");
      while (tok.hasMoreTokens()) {
        bids.add(tok.nextToken());
      }
      
      // lookup all service components
      long numberOfComponents = 0;
      long numberOfDependencies = 0;
      long lastBundleId = -1;
      for (ComponentDeclaration sc : getComponentDeclarations()) {
        String name = sc.getName();
        // check if this component is enabled or disabled.
        if (!mayDisplay(sc, servicesFilter, componentsRegex)) {
          continue;
        }
        int state = sc.getState();
        Bundle bundle = sc.getBundle();
        if (matchBundle(bundle, bids)) {
          long bundleId = bundle.getBundleId();
          if (notavail) {
            if (sc.getState() != ComponentDeclaration.STATE_UNREGISTERED) {
              continue;
            }
          }
          
          numberOfComponents++;
          if (lastBundleId != bundleId) {
            lastBundleId = bundleId;
            if (comp) {
              out.println("[" + bundleId + "] " + compactName(bundle.getSymbolicName()));
            } else {
              out.println("[" + bundleId + "] " + bundle.getSymbolicName());
            }
          }
          
          if (comp) {
            out.print(" " + compactName(checkComponentNameSize(name, displayAllProps)) + " "
                + compactState(ComponentDeclaration.STATE_NAMES[state]));
          } else {
            out.println(" " + checkComponentNameSize(name, displayAllProps) + " " + ComponentDeclaration.STATE_NAMES[state]);
          }
          
          if (!nodeps) {
            ComponentDependencyDeclaration[] dependencies = sc.getComponentDependencies();
            if (dependencies != null && dependencies.length > 0) {
              numberOfDependencies += dependencies.length;
              if (comp) {
                out.print('(');
              }
              for (int j = 0; j < dependencies.length; j++) {
                ComponentDependencyDeclaration dep = dependencies[j];
                if (notavail && !isUnavailable(dep)) {
                  continue;
                }
                String depName = dep.getName();
                String depType = dep.getType();
                int depState = dep.getState();
                
                if (comp) {
                  if (j > 0) {
                    out.print(' ');
                  }
                  out.print(compactName(depName) + " " + compactState(depType) + " "
                      + compactState(ComponentDependencyDeclaration.STATE_NAMES[depState]));
                } else {
                  out.println("    " + depName + " " + depType + " "
                      + ComponentDependencyDeclaration.STATE_NAMES[depState]);
                }
              }
              if (comp) {
                out.print(')');
              }
            }
          }
          if (comp) {
            out.println();
          }
        }
      }
      
      if (stats) {
        out.println("Statistics:");
        out.println(" - Components: " + numberOfComponents);
        if (!nodeps) {
          out.println(" - Dependencies: " + numberOfDependencies);
        }
      }
    } catch (Throwable t) {
      t.printStackTrace(out);
    }
  }
  
  private String checkComponentNameSize(String compName, boolean allProps) {
    if (allProps) {
      return compName;
    }
    if (compName.length() > 50 || compName.indexOf("\n") != -1) {      
      int leftPar = compName.indexOf("(");
      if (leftPar != -1) {
        StringBuilder sb = new StringBuilder();
        sb.append(compName.substring(0, leftPar));
        String props = compName.substring(leftPar);
        int newline = props.indexOf("\n");
        boolean truncated = false;
        if (newline != -1) {
          props = props.substring(0, newline);
          truncated = true;
        }
        if (props.length() > MAX_PROPERTIES_SIZE) {
          props = props.substring(0, Math.min(props.length(), MAX_PROPERTIES_SIZE));
          truncated = true;
        }
        sb.append(props);
        if (truncated) {
          sb.append("...)");
        }
        return sb.toString();
      }
    }
    return compName;
  }

  private List<ComponentDeclaration> getComponentDeclarations() {
    // get DM and DS components
    List<ComponentDeclaration> l = new ArrayList();
    _dm.getComponentDeclaration(l);
    _ds.getComponentDeclaration(l);
    return l;
  }
  
  private boolean isUnavailable(ComponentDependencyDeclaration dep) {
    switch (dep.getState()) {
    case ComponentDependencyDeclaration.STATE_UNAVAILABLE_OPTIONAL:
    case ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED:
      return true;
    }
    return false;
  }
  
  private boolean matchBundle(Bundle bundle, List<String> ids) {
    if (ids.size() == 0) {
      return true;
    }
    
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i);
      try {
        Long longId = Long.valueOf(id);
        if (longId == bundle.getBundleId()) {
          return true;
        }
      } catch (NumberFormatException e) {
        // must match symbolic name
        if (id.equals(bundle.getSymbolicName())) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  /**
  * Returns the value of a command arg parameter, or from the gogo shell if the parameter is not passed to
  * the command.
  */
  private String getParam(CommandSession session, String param, String value) {
    if (value != null && value.length() > 0) {
      return value;
    }
    Object shellParamValue = session.get(param);
    return shellParamValue != null ? shellParamValue.toString() : null;
  }
  
  /**
  * Returns the value of a command arg parameter, or from the gogo shell if the parameter is not passed to
  * the command. The parameter value is meant to be a list of values separated by a blank or a comma. 
  * The values are split and returned as an array.
  */
  private String[] getParams(CommandSession session, String name, String value) {
    String values = null;
    if (value == null || value.length() == 0) {
      value = (String) session.get(name);
      if (value != null) {
        values = value;
      }
    } else {
      values = value;
    }
    if (values == null) {
      return new String[0];
    }
    return values.trim().split(", ");
  }
  
  /**
  * Checks if a component can be displayed. We make a logical OR between the three following conditions:
  * 
  *  - the component service properties are matching a given service filter ("services" option)
  *  - the component implementation class name is matching some regex ("components" option)
  *  - the component declaration name is matching some regex ("names" option)
  *  
  *  If some component ids are provided, then the component must also match one of them.
  */
  private boolean mayDisplay(ComponentDeclaration component, Filter servicesFilter, String[] components) {
    if (servicesFilter == null && components.length == 0) {
      return true;
    }
    
    // Check component service properties
    boolean servicesMatches = servicesMatches(component, servicesFilter);
    
    // Check components regexs, which may match component implementation class name
    boolean componentsMatches = componentMatches(component, components);
    
    // Logical OR between service properties match and component service/impl match.
    return servicesMatches || componentsMatches;
  }
  
  /**
  * Checks if a given filter is matching some service properties possibly provided by a component
  */
  private boolean servicesMatches(ComponentDeclaration component, Filter servicesFilter) {
    boolean match = false;
    if (servicesFilter != null) {
      String serviceNames = component.getName();

      // See if the service is a class implementation (in this case, skip this test)
      if (serviceNames.startsWith("class ")) {
        return false;
      }
      // Remove service properties, if any.
      int cuttOff = serviceNames.indexOf("(");
      if (cuttOff != -1) {
        serviceNames = serviceNames.substring(0, cuttOff).trim();
      }

      String[] services = serviceNames.split(",");
      Dictionary props = parseProperties(component.getName());

      if (services != null) {
        Dictionary properties = parseProperties(component.getName());
        if (properties == null) {
          properties = new Properties();
        }
        if (properties.get(Constants.OBJECTCLASS) == null) {
          properties.put(Constants.OBJECTCLASS, services);
        }
        match = servicesFilter.match(properties);
      }
    }
    return match;
  }
  
  /**
  * Checks if the component implementation class name (or some possible provided services) are matching
  * some regular expressions.
  */
  private boolean componentMatches(ComponentDeclaration component, String[] names) {
    String description = component.toString();
    if (names.length > 0) {
      int leftBracket = description.indexOf("[");
      if (leftBracket == -1) {
        return false;
      }
      int space = description.indexOf(" ", leftBracket + 1);
      if (space == -1) {
        return false;
      }
      int rightBracket = description.indexOf("]", space + 1);
      if (rightBracket == -1) {
        return false;
      }
      description = description.substring(space + 1, rightBracket).trim();
      if (description.startsWith("class ") && description.indexOf("@") != -1) {
          int at = description.indexOf("@");
          description = description.substring("class ".length()+1, at);
      }
    }
    
    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      boolean not = false;
      if (name.startsWith("!")) {
        name = name.substring(1);
        not = true;
      }
      boolean match = false;

      if (description.matches(name)) {
        match = true;
      }
      
      if (not) {
        match = !match;
      }
      
      if (match) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
  * Compact names that look like state strings. State strings consist of
  * one or more words. Each word will be shortened to the first letter,
  * all letters concatenated and uppercased.
  */
  private String compactState(String input) {
    StringBuffer output = new StringBuffer();
    StringTokenizer st = new StringTokenizer(input);
    while (st.hasMoreTokens()) {
      output.append(st.nextToken().toUpperCase().charAt(0));
    }
    return output.toString();
  }
  
  /**
  * Compacts names that look like fully qualified class names. All packages
  * will be shortened to the first letter, except for the last one. So
  * something like "org.apache.felix.MyClass" will become "o.a.f.MyClass".
  */
  private String compactName(String input) {
    StringBuffer output = new StringBuffer();
    int lastIndex = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
      case '.':
        output.append(input.charAt(lastIndex));
        output.append('.');
        lastIndex = i + 1;
        break;
      case ' ':
      case ',':
        if (lastIndex < i) {
          output.append(input.substring(lastIndex, i));
        }
        output.append(c);
        lastIndex = i + 1;
        break;
      }
    }
    if (lastIndex < input.length()) {
      output.append(input.substring(lastIndex));
    }
    return output.toString();
  }
    
  // --------------------------------- WTF command -----------------------------------------------------------
  
  @Descriptor("Make some root cause failure diagnostics")
  public void diag() {
    diag(System.out);
  }
  
  @Descriptor("Make some root cause failure diagnostics specifics to Declarative Service")
  public void dsdiag() {
    try {
      _ds.diag(System.out);
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }
  
  private void diag(PrintStream out) {
    try {
      List<ComponentDeclaration> downComponents = getDependenciesThatAreDown();
      if (downComponents.isEmpty()) {
        out.println("No missing dependencies found.");
      } else {
        out.println(downComponents.size() + " missing dependencies found (Use the command \"asr.service:list na\" to list them).");
        out.println("-----------------------------------------------------------------------------------");
      }
      listResolvedBundles(out);
      listInstalledBundles(out);
      Set<ComponentId> downComponentsRoot = getTheRootCouses(downComponents, out);
      listAllMissingConfigurations(downComponentsRoot, out);
      listAllMissingServices(downComponents, downComponentsRoot, out);
      _ds.diag(System.out);
    } catch (Throwable t) {
      t.printStackTrace(out);
    }
  }
  
  private Set<ComponentId> getTheRootCouses(List<ComponentDeclaration> downComponents, PrintStream out) throws IOException, InvalidSyntaxException {
    Set<ComponentId> downComponentsRoot = new TreeSet<ComponentId>();
    for (ComponentDeclaration c : downComponents) {
      List<ComponentId> root = getRoot(downComponents, c, new ArrayList<ComponentId>(), out);
      downComponentsRoot.addAll(root);
    }
    return downComponentsRoot;
  }
  
  @SuppressWarnings("unchecked")
  private List<ComponentDeclaration> getDependenciesThatAreDown() {
    List<ComponentDeclaration> downComponents = new ArrayList<ComponentDeclaration>();
    List<ComponentDeclaration> components = new ArrayList();
    _dm.getComponentDeclaration(components);
    _ds.getComponentDeclaration(components);
    // first create a list of all down components
    for (ComponentDeclaration c : components) {
      if (c.getState() == ComponentDeclaration.STATE_UNREGISTERED) {
        downComponents.add(c);
      }
    }
    
//    out.println("DownList:");
//    for (ComponentDeclaration decl : downComponents) {
//      out.println(decl.getName());
//      ComponentDependencyDeclaration[] cddecl;
//      try {
//        cddecl = decl.getComponentDependencies();
//        if (cddecl != null) {
//          for (int j = 0; j<cddecl.length; j ++) {
//            out.println("\t" + cddecl[j].getName());
//          }
//        }
//      } catch (IOException | InvalidSyntaxException e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace(out);
//      }
//    }

    return downComponents;
  }
  
  private void listResolvedBundles(PrintStream out) {
    List<Bundle> list = new ArrayList();
    for (Bundle b : m_context.getBundles()) {
      if (b.getState() == Bundle.RESOLVED && !isFragment(b)) {
        Dictionary<String, String> h = b.getHeaders();
        if (h.get(BUNDLE_ACTIVATOR) != null // OSGi Activator
            || h.get("Service-Component") != null // Declarative Service
            || h.get("DependencyManager-Component") != null) // DependencyManager
        {
          list.add(b);
        }
      }
    }
    if (list.size() > 0) {
      out.println("Please note that the following bundles are in the RESOLVED state and may contain some services:");
      for (Bundle b : list) {
        out.println(" * [" + b.getBundleId() + "] " + b.getSymbolicName());
      }
    }
  }
  
  private void listInstalledBundles(PrintStream out) {
    boolean areResolved = false;
    for (Bundle b : m_context.getBundles()) {
      if (b.getState() == Bundle.INSTALLED) {
        areResolved = true;
        break;
      }
    }
    if (areResolved) {
      out.println("Please note that the following bundles are in the INSTALLED state:");
      MissingImportInfo mirc = new MissingImportInfo(m_context);

      for (Bundle b : m_context.getBundles()) {
        if (b.getState() == Bundle.INSTALLED) {
        	out.print(" * [" + b.getBundleId() + "] " + b.getSymbolicName());
        	mirc.displayMissingImports(b);
        	out.println();
        }
      }
    }
    
    SplitPackage sp = new SplitPackage(m_context);
    sp.detectSplitPackages();
  }
  
  private boolean isFragment(Bundle b) {
    @SuppressWarnings("unchecked")
    Dictionary<String, String> headers = b.getHeaders();
    return headers.get("Fragment-Host") != null;
  }
  
  private void listAllMissingConfigurations(Set<ComponentId> downComponentsRoot, PrintStream out) {
    if (hasMissingType(downComponentsRoot, CONFIGURATION)) {
      out.println("The following configuration(s) are missing: ");
      for (ComponentId s : downComponentsRoot) {
        if (CONFIGURATION.equals(s.getType())) {
          out.println(" * " + s.getName() + " for bundle " + s.getBundleName());
        }
      }
    }
  }
  
  private void listAllMissingServices(List<ComponentDeclaration> downComponents,
                                      Set<ComponentId> downComponentsRoot, PrintStream out) throws IOException, InvalidSyntaxException {
    if (hasMissingType(downComponentsRoot, SERVICE)) {
      out.println("The following service(s) are missing: ");
      for (ComponentId s : downComponentsRoot) {
        if (SERVICE.equals(s.getType())) {
          out.print(" * " + s.getName());
          ComponentDeclaration component = getComponentDeclaration(s.getName(), downComponents);
          if (component == null) {
            out.println(" is not found in the service registry");
          } else {
            ComponentDependencyDeclaration[] componentDependencies = component.getComponentDependencies();
            out.println(" and needs:");
            for (ComponentDependencyDeclaration cdd : componentDependencies) {
              if (cdd.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED) {
                out.println(cdd.getName());
              }
            }
            out.println(" to work");
          }
        }
      }
    }
  }
  
  private boolean hasMissingType(Set<ComponentId> downComponentsRoot, String type) {
    for (ComponentId s : downComponentsRoot) {
      if (type.equals(s.getType())) {
        return true;
      }
    }
    return false;
  }
  
  private List<ComponentId> getRoot(List<ComponentDeclaration> downComponents, ComponentDeclaration c,
                                    List<ComponentId> backTrace, PrintStream out) throws IOException, InvalidSyntaxException {
    ComponentDependencyDeclaration[] componentDependencies = c.getComponentDependencies();
    int downDeps = 0;
    List<ComponentId> result = new ArrayList<ComponentId>();
    for (ComponentDependencyDeclaration cdd : componentDependencies) {
      if (cdd.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED) {
        downDeps++;
        // Detect missing configuration dependency
        if (CONFIGURATION.equals(cdd.getType())) {
          String bsn = c.getBundle().getSymbolicName();
          result.add(new ComponentId(cdd.getName(), cdd.getType(), bsn));
          continue;
        }
        
        // Detect if the missing dependency is a root cause failure
        ComponentDeclaration component = getComponentDeclaration(cdd.getName(), downComponents);
        if (component == null) {
          result.add(new ComponentId(cdd.getName(), cdd.getType(), null));
          continue;
        }
        // Detect circular dependency
        ComponentId componentId = new ComponentId(cdd.getName(), cdd.getType(), null);
        if (backTrace.contains(componentId)) {
          // We already got this one so its a circular dependency
          out.print("Circular dependency found:\n *");
          for (ComponentId cid : backTrace) {
            out.print(" -> " + cid.getName() + " ");
          }
          out.println(" -> " + componentId.getName());
          result.add(new ComponentId(c.getName(), SERVICE, c.getBundle().getSymbolicName()));
          continue;
        }
        backTrace.add(componentId);
        return getRoot(downComponents, component, backTrace, out);
      }
    }
    if (downDeps > 0 && result.isEmpty()) {
      result.add(new ComponentId(c.getName(), SERVICE, c.getBundle().getSymbolicName()));
    }
    return result;
  }
  
  private ComponentDeclaration getComponentDeclaration(final String fullName, List<ComponentDeclaration> list) {
    String simpleName = getSimpleName(fullName);
    Properties props = parseProperties(fullName);
    for (ComponentDeclaration c : list) {
      String serviceNames = c.getName();
      int cuttOff = serviceNames.indexOf("(");
      if (cuttOff != -1) {
        serviceNames = serviceNames.substring(0, cuttOff).trim();
      }
      for (String serviceName : serviceNames.split(",")) {
        if (simpleName.equals(serviceName.trim()) && doPropertiesMatch(props, parseProperties(c.getName()))) {
          return c;
        }
      }
    }
    return null;
  }
  
  private boolean doPropertiesMatch(Properties need, Properties provide) {
    for (Entry<Object, Object> entry : need.entrySet()) {
      Object prop = provide.get(entry.getKey());
      if (prop == null || !prop.equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }
  
  private String getSimpleName(String name) {
    int cuttOff = name.indexOf("(");
    if (cuttOff != -1) {
      return name.substring(0, cuttOff).trim();
    }
    return name.trim();
  }
  
  private Properties parseProperties(String name) {
    Properties result = new Properties();
    int cuttOff = name.indexOf("(");
    if (cuttOff != -1) {
      String propsText = name.substring(cuttOff + 1, name.indexOf(")"));
      String[] split = propsText.split(",");
      for (String prop : split) {
        String[] kv = prop.split("=");
        if (kv.length == 2) {
          result.put(kv[0], kv[1]);
        }
      }
    }
    return result;
  }
  
  
  // ----------- ServiceDiagnostics interface ----------------------------------------------
  
  @Override
  public String allServices() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    list(ps, true, false, false, false, null, null, null, false);
    return baos.toString();
  }

  @Override
  public void allServices(PrintStream out) {
    list(out, true, false, false, false, null, null, null, false);
  }

  @Override
  public String notAvail() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    diag(ps);
    return baos.toString();
  }

  @Override
  public void notAvail(PrintStream out) {
    diag(out);
  }
}
