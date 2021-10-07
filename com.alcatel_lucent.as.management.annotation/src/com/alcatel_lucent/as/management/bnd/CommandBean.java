package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;

import aQute.bnd.osgi.*;

class CommandBean {
  
  private String _methodName;
  private String _snmpName;
  private Integer _oid;
  private Integer _code;
  private String _desc;
  
  CommandBean(Annotation annotation, String methodName) {
    _methodName = methodName;
    _snmpName = annotation.get("snmpName");
    _oid = (Integer) annotation.get("oid");
    _code = (Integer) annotation.get("code");
    _desc = annotation.get("desc");
  }
  
  String getSnmpName() {
    return _snmpName;
  }
  
  int getOid() {
    return _oid;
  }
  
  // Return true if this command has snmp info, false if not.
  boolean validate(boolean hasRootOid) {
    // if the @Commands has snmp info, then we also must have snmp info
    if (_snmpName == null && (_oid != null || hasRootOid)) {
      throw new IllegalArgumentException("Missing snmpName attribute on annotated method " + _methodName);
    }
    if (_oid == null && (_snmpName != null || hasRootOid)) {
      throw new IllegalArgumentException("Missing oid attribute on annotated method " + _methodName);
    }
    return _snmpName != null && _oid != null;
  }
  
  void print(PrintWriter pw) {
    Utils.print(pw, 
        "      <operation id=\"", _code, "\" name=\"", _methodName, "\" description=\"", _desc, "\" >\n");
    if (_snmpName != null && _oid != null) {
      Utils.print(pw, 
        "         <descriptor>\n", 
        "            <field name=\"snmp\" value=\"", _snmpName, "\"/>\n",
        "            <field name=\"oid\" value=\"", _oid, "\"/>\n",
        "         </descriptor>\n");
    }
    Utils.print(pw, 
        "      </operation>\n");
  }
}
