// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;

import com.alcatel_lucent.as.management.annotation.config.MonconfVisibility;

public class MonconfProperty {

  private String _type;
  private String _dynamic;
  private String _required;
  private String _def;
  private String _desc;
  private String _valid;
  private String _section;
  private String _name;
  private String _title;
  private String _oid;
  private String _level;
  private MonconfVisibility _monconfVisibility;
  private String _snmpName;
  private String[] _range;

  public void setMetaData(String type, String name, String required, String dynamic, String def, String desc,
    String valid, String title, String oid, String snmpName, String level, String section, MonconfVisibility monconfVisibility, String[] range)
  {
    _type = type;
    _name = name;
    _required = required;
    _dynamic = dynamic;
    _def = def;
    _desc = desc;
    _valid = valid;
    _section = section;
    _title = title;
    _oid = oid;
    _snmpName = snmpName;
    _level = level;
    _section = section;
    _monconfVisibility = monconfVisibility;
    _range = range;
  }

  public void write(PrintWriter out) throws Exception {
    switch (_monconfVisibility) {
    case PUBLIC:
    case PRIVATE:
      write(out, _monconfVisibility.toString().toUpperCase());
      break;
      
    case BOTH:
      write(out, MonconfVisibility.PUBLIC.toString().toUpperCase());
      out.println();
      write(out, MonconfVisibility.PRIVATE.toString().toUpperCase());
      break;
    }
  }
  
  private void write(PrintWriter out, String visibility) {
    out.printf("%-10s %s%n", "SECTION 1", _section);
    out.printf("%-10s%n", visibility);
    out.printf("%-10s %s%n", "NAME", _name);
    if (_oid != null && _snmpName != null)
      out.printf("%-10s %s %s%n", "OID", _snmpName, _oid);
    out.printf("%-10s %s%n", "LEVEL", _level);
    out.printf("%-10s %s%n", "TITLE", _title);
    _valid = _valid == null ? "None" : _valid;
    out.printf("%-10s %s%n", "VALID", _valid);
    out.printf("%-10s %s", "TYPE", _type);
    for (String range : _range) {
    	out.printf(" %s", range);
    }
    out.printf("%n");
    out.printf("%-10s %s%n", "REQUIRED", _required);
    out.printf("%-10s %s%n", "DYNAMIC", _dynamic);
    out.printf("%-10s %s%n", "DEFAULT", _def);
    out.printf(_desc);
    out.println();
  }

}
