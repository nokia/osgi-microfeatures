package com.alcatel_lucent.as.management.bnd;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import aQute.bnd.osgi.Annotation;
import aQute.service.reporter.Reporter;

public class ConfigBean {
  private String _pid;
  List<PropertyBean> _properties = new ArrayList<PropertyBean>();
  private String _fullClassName;
  private Reporter _reporter;
  private String _rootSnmpName; // root snmp name
  private Integer[] _rootOid; // root oid
  private String _monconfModule;
  private String _monconfAgent;
  
  public void reporter(Reporter reporter) {
    _reporter = reporter;
  }
  
  void fullClassName(String fullClassName) {
    _fullClassName = fullClassName;
  }
  
  /* Called before config(Annotation, fullClassName) */
  void add(PropertyBean property) {
    _properties.add(property);
  }
  
  /* Called after add(Property) */
  void config(Annotation annotation) {
    _pid = annotation.get("name");
    _monconfModule = (String) Utils.get(annotation, "monconfModule", null);
    _monconfAgent = (String) Utils.get(annotation, "monconfAgent", null);
    String section = annotation.get("section");
    if (section != null) {
      for (PropertyBean p : _properties) {
        p.setDefaultSection(section);
      }
    }
    _rootSnmpName = annotation.get("rootSnmpName");
    Object[] oids = annotation.get("rootOid");
    if (oids != null) {
      _rootOid = Arrays.asList(oids).toArray(new Integer[oids.length]);
    }
  }
  
  void validate() {
    // if no domain found from property, then use class name as the default pid.
    _pid = (_pid != null) ? _pid : _fullClassName;
    if (_rootSnmpName != null && _rootOid == null) {
      throw new IllegalArgumentException("Missing rootOid attribute in @ASConfig annotation");
    }
    if (_rootSnmpName == null && _rootOid != null) {
      throw new IllegalArgumentException("Missing rootSnmpName attribute in @ASConfig annotation");
    }
    boolean hasRootOid = (_rootSnmpName != null && _rootOid != null);
    
    // If the @ASRootOid is used, and has snmp info, then properties must also provide snmp info
    // Also check if all property names are unique.
    HashSet<String> properties = new HashSet<String>();
    for (PropertyBean p : _properties) {
      if (!properties.add(p.getFieldValue())) {
        throw new IllegalArgumentException("Property " + p.getFieldName() + " has not a unique field value: "
            + p.getFieldValue());
      }
      
      boolean propertyHasSnmpInfo = p.validate(hasRootOid);
      if (propertyHasSnmpInfo && !hasRootOid) {
        throw new IllegalArgumentException("Some snmp attributes have been defined in property "
            + p.getFieldValue() + ", but no snmp informations have been defined "
            + " in the @ASConfig annotation");
      }
    }
  }
  
  public boolean isPrintable() {
    return !_properties.isEmpty();
  }

  void print(PrintWriter pw) {
    if (_properties.size() == 0) {
      return;
    }
    Utils.print(pw, "   <mbean name=\"ProxyAppPropMBean\"\n");
    if (_rootSnmpName != null && _rootOid != null) {
      Utils.print(pw, "          description=\"snmpMapping='", _rootSnmpName, ":", Utils.getOid(_rootOid),
                  "'\"\n");
    }
    Utils.print(pw, "          domain=\"", _pid, "\">\n");
    for (PropertyBean p : _properties) {
      p.print(pw);
    }
    Utils.print(pw, "   </mbean>\n");
  }

  public void print(MonconfPropertiesBuilder builder) {
    if (_monconfModule != null && new File("monconf").exists()) {
      MonconfProperties monconfProperties = new MonconfProperties(_pid, _monconfModule, _monconfAgent);
      builder.addProperties(monconfProperties);
      for (PropertyBean p : _properties) {
        MonconfProperty monconfProperty = new MonconfProperty();        
        p.print(monconfProperty);
        monconfProperties.addProperty(monconfProperty);        
      }
    }
  }
}
