// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;
import java.util.Arrays;

import com.alcatel_lucent.as.management.annotation.config.MonconfVisibility;
import com.alcatel_lucent.as.management.annotation.config.Scope;
import com.alcatel_lucent.as.management.annotation.config.Visibility;

import aQute.bnd.osgi.Annotation;
import aQute.service.reporter.Reporter;

public class PropertyBean {
  enum Type {
    ADDRESS, BOOLEAN, EXTERNAL, INTEGER, MSELECT, ORDERED, SELECT, STRING, FILEDATA
  }
  
  private final Type _type;
  private final String _fieldName;
  private final String _fieldValue;
  private String _title;
  private boolean _required;
  private boolean _dynamic;
  private String _help;
  private String _helpPath;
  private String _snmpName;
  private Integer _oid;
  private String _section;
  private String _valid;
  private Object _defval;
  private int _min;
  private int _max;
  private String[] _range = new String[0];
  private final Annotation _annotation;
  private final Reporter _reporter;
  private final String _visibility;
  private final String _scope;
  private final String _displayPage;
  private final String _blueprintEditor;
  private final MonconfVisibility _monconfVisibility;

  public PropertyBean(Annotation annotation, Type type, String fieldName, String fieldValue, Reporter reporter) {
    _annotation = annotation;
    _type = type;
    _fieldName = fieldName;
    _fieldValue = fieldValue;
    _reporter = reporter;
    _title = annotation.get("title");
    _required = Utils.get(annotation, "required", Boolean.FALSE);
    _dynamic = Utils.get(annotation, "dynamic", Boolean.FALSE);
    _help = Utils.get(annotation, "help", "Description of the property " + _fieldValue);
    _helpPath = annotation.get("helpPath");
    _snmpName = Utils.get(annotation, "snmpName", null);
    _oid = Utils.get(annotation, "oid", null);
    _section = annotation.get("section");
    _valid = annotation.get("validation");
    _visibility = Utils.get(annotation, "visibility", Visibility.BASIC.toString().toUpperCase());
    _scope = Utils.get(annotation, "scope", Scope.ANY.toString().toUpperCase());
    _displayPage = annotation.get("displayPage");
    _blueprintEditor = annotation.get("blueprintEditor");
    String monconfVisibility = Utils.get(annotation, "monconfVisibility", MonconfVisibility.PUBLIC.toString());
    _monconfVisibility = MonconfVisibility.valueOf(monconfVisibility);

    if (_valid == null)
    {
      String clazz = annotation.get("valid");
      _valid = clazz != null
        ? Utils.parseClassAttrValue(clazz)
        : null;
    }
    
    switch (_type) {
    case ADDRESS:
      _defval = (String) Utils.get(annotation, "defval", "");
      break;
    
    case BOOLEAN:
      _defval = Boolean.valueOf((boolean) Utils.get(annotation, "defval", false));
      break;
    
    case EXTERNAL:
      _defval = annotation.get("className");
      if (_defval == null)
        _defval = annotation.get("externalClass");
      break;
    
    case INTEGER:
      _defval = (Integer) Utils.get(annotation, "defval", -1);
      _min = Utils.get(annotation, "min", Integer.MIN_VALUE);
      _max = Utils.get(annotation, "max", Integer.MAX_VALUE);
      break;
    
    case ORDERED:
    case SELECT:
    case MSELECT:
      Object[] defvals = annotation.get("defvals");
      _defval = defvals != null
        ? mkString(defvals)
        : (String) Utils.get(annotation, "defval", "");
      Object[] range = annotation.get("range");
      _range = Arrays.asList(range).toArray(new String[range.length]);
      break;
    
    case STRING:
      _defval = (String) Utils.get(annotation, "defval", "");
      break;
    
    case FILEDATA:
      _defval = (String) annotation.get("fileData");
      break;
    
    default:
      throw new IllegalArgumentException("type not supported: " + _type);
    }
  }
  
  String getFieldName() {
    return _fieldName;
  }
  
  String getFieldValue() {
    return _fieldValue;
  }
  
  public void setDefaultSection(String section) {
    if (_section == null) {
      _section = section;
    }
  }
  
  /**
   * Validate a @Property annotation.
   * @return true if the annotation contains some snmp informations, false if not.
   * @throws IllegalArgumentException if the annotation contains inconsistent attributes.
   */
  public boolean validate(boolean hasRootOid) {
    // If the property is required, then check if there is a default value
    if (_required && (_defval == null || _defval.toString().length() == 0)) {
      throw new IllegalArgumentException("the property " + _fieldValue
          + " is required and must have a non empty default value");
    }
    
    // Check if a section is specified.
    if (_section == null) {
      throw new IllegalArgumentException("missing section attribute in annotation " + _annotation.getName()
          + " on field " + _fieldValue);
    }
    
    // Check snmp params
    if (_snmpName != null && _oid == null) {
      throw new IllegalArgumentException("missing oid attribute in annotation " + _annotation.getName()
          + " on field " + _fieldValue);
    }
    if (_snmpName == null && _oid != null) {
      throw new IllegalArgumentException("missing snmpName attribute in annotation " + _annotation.getName()
          + " on field " + _fieldValue);
    }
    
    /* Check external type
    switch (_type) {
    case EXTERNAL:
        _defval = Utils.parseClass(_defval.toString(), Utils.CLASS, 1);
        break;

    default:
    }*/       
    
    return _snmpName != null && _oid != null;
  }
  
  public void print(PrintWriter pw) {
    Utils.print(pw, "      <attribute name=\"", _fieldValue, "\">\n", "         <descriptor>\n",
                "            <field name=\"section\" value=\"", _section, "\"/>\n",
                "            <field name=\"type\" value=\"", _type, "\"/>\n",
                "            <field name=\"title\" value=\"", _title, "\"/>\n",
                "            <field name=\"defaultValue\" value=\"", _defval, "\"/>\n",
                "            <field name=\"required\" value=\"", _required, "\"/>\n",
                "            <field name=\"level\" value=\"", _visibility, "\"/>\n",
                "            <field name=\"scope\" value=\"", _scope, "\"/>\n",
                "            <field name=\"dynamic\" value=\"", _dynamic, "\"/>\n");
    if (_helpPath == null) {
      Utils.print(pw, "            <field name=\"help\" value=\"", encodeToHTML(_help), "\"/>\n");
    } else {
      Utils.print(pw, "            <field name=\"helpPath\" value=\"", _helpPath, "\"/>\n");
    }
    if (_snmpName != null && _oid != null) {
      Utils.print(pw, "            <field name=\"snmpName\" value=\"", _snmpName, "\"/>\n",
                  "            <field name=\"oid\" value=\"", _oid, "\"/>\n");
    }
    
    if (_range != null && _range.length > 0) {
      Utils.print(pw, "            <field name=\"range\" value=\"", mkString(_range), "\"/>\n");
    }
    
    if (_valid != null) {
      Utils.print(pw, "            <field name=\"valid\" value=\"", _valid, "\"/>\n");
    }

    if (_displayPage != null) {
      Utils.print(pw, "            <field name=\"displayPage\" value=\"", _displayPage, "\"/>\n");
    }
    
    if (_blueprintEditor != null) {
        Utils.print(pw, "            <field name=\"blueprintEditor\" value=\"", _blueprintEditor, "\"/>\n");
      }

    switch (_type) {
    case INTEGER:
      Utils.print(pw, "            <field name=\"min\" value=\"", _min, "\"/>\n");
      Utils.print(pw, "            <field name=\"max\" value=\"", _max, "\"/>\n");
      break;
    
    default:
    }
    
    Utils.print(pw, "         </descriptor>\n", "      </attribute>\n");
  }
  
  public static String encodeToHTML(String s) {
    if (s == null)
      return null;
    int length = s.length();
    StringBuffer ret = new StringBuffer(length * 2);
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ')
        ret.append(c);
      else if (c == '&')
        ret.append("&amp;");
      else if (c == 34)
        ret.append("&quot;");
      else if (c == 39)
        ret.append("&acute;");
      else if (c == '<')
        ret.append("&lt;");
      else if (c == '>')
        ret.append("&gt;");
      else if (c == 160)
        ret.append("nbsp;");
      else if (c == 173)
        ret.append("&shy;");
      else if (c == '$')
        ret.append("$$");
      else {
        ret.append("&#");
        ret.append(Integer.toString(c));
        ret.append(";");
      }
    }
    return ret.toString();
  }

  public void print(MonconfProperty monconfProperties) {   
    String name = _fieldValue;
    String required = _required ? "Yes" : "No";
    String dynamic = _dynamic ? "Yes" : "No";
    String def = _defval.toString();
//    String desc = encodeToHTML(_help);
    String desc = _help;
    String valid = _valid;
    String title = _title;
    String oid = _oid != null ? _oid.toString() : null;
    
    String level;
    if (_visibility.equals(Visibility.BASIC.toString())) {
      level = "Basic";
    } else if (_visibility.equals(Visibility.ADVANCED.toString())) {
      level = "Advanced";
    } else if (_visibility.equals(Visibility.HIDDEN.toString())) {
      level = "Hidden";
    } else {
      throw new IllegalArgumentException("visibility: " + _visibility + " not supported.");
    }
        
    String type;
    switch (_type) {
    case ADDRESS:
      type = "Address";
      break;
    
    case BOOLEAN:
      type = "Boolean";
      break;
    
    case EXTERNAL:
      type = "External";      
      break;
    
    case INTEGER:
      type = "Integer " + _min + " " + _max;
      break;
    
    case SELECT:
      type = "Select";
      break;
      
    case MSELECT:
      type = "MSelect";
      break;
    
    case STRING:
      type = "String";
      break;
    
    case FILEDATA:
      type = "FileData";
      if (_defval != null) {
        def = def.toString().trim();      
        if (def.regionMatches(true, 0, "META-INF/", 0, "META-INF/".length())) {
          def = def.substring("META-INF/".length());
        } else if (def.regionMatches(true, 0, "META-INF", 0, "META-INF".length())) {
          def = def.substring("META-INF".length());
        }
      }        
      break;
    
    default:
      throw new IllegalArgumentException("type not supported: " + _type);
    }
    
    monconfProperties.setMetaData(type, name, required, dynamic, def, desc, valid, title, oid, _snmpName, level, _section, _monconfVisibility, _range);
  }

  private static String mkString(Object[] s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length; i++) {
      sb.append(s[i].toString());
      if (i < s.length - 1) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

}
