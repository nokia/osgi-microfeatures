package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;

import com.alcatel_lucent.as.management.annotation.stat.ConsolidationMode;

import aQute.bnd.osgi.Annotation;

class CounterBean {
  enum Type {
    COUNTER, GAUGE
  }
  
  private Type _type;
  private String _counterName;
  private String _snmpName;
  private Integer _oid;
  private String _desc;
  private String _consolidation;
  private final Integer _index;
  private final boolean _isMethod;
  private final String _fieldValue;
  private final Integer _defaultValue;
  
  CounterBean(Annotation annotation, Type type, String counterName, boolean isMethod, String fieldValue) {
    _type = type;
    _counterName = counterName;
    _snmpName = annotation.get("snmpName");
    _oid = (Integer) annotation.get("oid");
    _desc = annotation.get("desc");
    _consolidation = getConsolidation(annotation);
    _index = annotation.get("index");
    _isMethod = isMethod;
    _fieldValue = fieldValue;
    _defaultValue = annotation.get("defaultValue");
  }
  
  String getSnmpName() {
    return _snmpName;
  }
  
  int getOid() {
    return _oid == null ? -1 : _oid;
  }
  
  int getIndex() {
    return _index == null ? -1 : _index;
  }
  
  // Return true if this counter has snmp info, false if not.
  boolean validate(boolean hasRootOid) {
    if (_isMethod) {
      if (!_counterName.startsWith("get")) {
        throw new IllegalArgumentException("Invalid method for counter: " + _counterName
          + ": the method must start with the \"get\" prefix");
      }
      if (_counterName.length() < 4) {
        throw new IllegalArgumentException("Invalid method for counter: " + _counterName
          + ": the method name must be in the \"getXX\" form");
      }
    } else if (_fieldValue == null && _defaultValue == null) {
        throw new IllegalArgumentException("Invalid null field " + _counterName + " without a specified defaultValue attribute.");
    }

    if (_snmpName == null && _oid != null) {
      throw new IllegalArgumentException("Missing snmpName attribute on annotated method " + _counterName);
    }
    if (_snmpName != null && _oid == null) {
      throw new IllegalArgumentException("Missing oid attribute on annotated method " + _counterName);
    }
    if (_oid == null && _index == null) {
      throw new IllegalArgumentException("Missing index attribute on method " + _counterName + ": index is mandatory when no oid is specified");
    }
    
    if (_index != null && _index < 0) {
      throw new IllegalArgumentException("Index attribute on method " + _counterName + " can't be negative");
    }

    if (_isMethod) {
      _counterName = Character.toLowerCase(_counterName.charAt(3)) + _counterName.substring(4);
    }
    return _snmpName != null && _oid != null;
  }
  
  void print(PrintWriter pw) {
    Utils.print(pw, "      <attribute name=\"", _counterName, "\" description=\"", _desc,
                "\" type=\"int\">\n", "         <descriptor>\n", "            <field name=\"", _type
                    .toString().toLowerCase(), "\" value=\"true\"/>\n");
    if (_snmpName != null && _oid != null) {
      Utils.print(pw, "            <field name=\"snmp\" value=\"", _snmpName, "\"/>\n",
                      "            <field name=\"oid\" value=\"", _oid, "\"/>\n");
    }
    if (_consolidation != null) {
        Utils.print(pw, "            <field name=\"consolidation\" value=\"", _consolidation, "\"/>\n");
    }
    Utils.print(pw, "         </descriptor>\n", "      </attribute>\n");
  }
  
  /**
   * Converts the Consolidation mode as String
   * A null string means the default mode should be applied ( i.e the sum ).
   * @param annotation
   * @return The consolidation mode or null
   */
  private String getConsolidation(Annotation annotation) {
	  	// consolidation is optional!*
	  	Object obj = annotation.get("consolidation");
	  	if( obj == null)
	  		return Utils.consolidationModeToString(ConsolidationMode.SUM);
	  	
	    return Utils.consolidationModeToString(ConsolidationMode.valueOf((String) obj ));
  }
}
