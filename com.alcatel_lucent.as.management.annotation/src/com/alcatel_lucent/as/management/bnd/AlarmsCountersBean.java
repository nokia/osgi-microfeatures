package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aQute.bnd.osgi.Annotation;

class AlarmsCountersBean {
  final String DUPLICATE = "__D";
  private String _fullClassName;
  private String _className;
  private String _rootSnmpName; // root snmp name
  private Integer[] _rootOid; // root oid
  private List<CounterBean> _counters = new ArrayList<CounterBean>();
  private List<AlarmBean> _alarms = new ArrayList<AlarmBean>();
  private List<CommandBean> _commands = new ArrayList<CommandBean>();
  
  void fullClassName(String fullClassName, String className) {
    _fullClassName = fullClassName;
    _className = className;
  }
  
  void stat(Annotation annotation) {
    if (_rootSnmpName != null && !_rootSnmpName.equals(annotation.get("rootSnmpName")))
      _rootSnmpName = DUPLICATE;
    else
      _rootSnmpName = annotation.get("rootSnmpName");
    Object[] oids = annotation.get("rootOid");
    if (oids != null) {
      Integer[] rootOid = Arrays.asList(oids).toArray(new Integer[oids.length]);
      if (_rootOid != null && !equals(_rootOid, rootOid))
        _rootOid = new Integer[0];
      else 
        _rootOid = rootOid;
    }
  }

  boolean equals(Integer[] a, Integer[] b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++)
      if (Integer.compare(a[i], b[i]) != 0) return false;
    return true;
  }
  
  void commands(Annotation annotation) {
    stat(annotation); //same code
  }
  
  void add(AlarmBean alarm) {
    _alarms.add(alarm); 
  }
  
  void add(CounterBean counter) {
    _counters.add(counter);
  }
  
  void add(CommandBean command) {
    _commands.add(command);
  }
  
  void validate() {
    // Validate alarms
    for (AlarmBean alarm : _alarms) {
      alarm.validate();
    }
    
    // Validate counters
    if (_rootSnmpName != null && _rootOid == null) {
      throw new IllegalArgumentException("Missing oid attribute in @Stat or @Command annotation");
    }
    if (_rootSnmpName == null && _rootOid != null) {
      throw new IllegalArgumentException("Missing snmpName attribute in @Stat or @Command annotation");
    }
    if (DUPLICATE.equals(_rootSnmpName)) {
      throw new IllegalArgumentException("Invalid duplicate snmpName attribute in @Stat and @Command annotations");
    }
    if (_rootOid != null && _rootOid.length == 0) {
      throw new IllegalArgumentException("Invalid duplicate rootOid attribute in @Stat and @Command annotations");
    }
    boolean hasRootOid = (_rootSnmpName != null && _rootOid != null);
    
    Set<Integer> indexes = new HashSet<Integer>();
    Set<Integer> oids = new HashSet<Integer>();
    
    int lastIndex = Integer.MIN_VALUE;

    for (CounterBean c : _counters) {
      // Check if indexes are unordered
      int index = c.getIndex();
      if (index != -1) {
        if (index < lastIndex) {
          throw new IllegalArgumentException("Detected unordered index in class " + _className + ":" + c.getIndex());
        }
        lastIndex = index;
      }
      
      // Check duplicated index, or oid
      if (index != -1 && ! indexes.add(c.getIndex())) {
        throw new IllegalArgumentException("Duplicated index found in class " + _className + ":" + c.getIndex());
      }
      
      int oid = c.getOid();
      if (oid != -1 && ! oids.add(c.getOid())) {
        throw new IllegalArgumentException("Duplicated oid found in class " + _className + ":" + c.getOid());
      }

      // If the counter has snmp info, then counters must also have rootOid info
      boolean counterHasSnmpInfo = c.validate(hasRootOid);
      if (counterHasSnmpInfo && !hasRootOid) {
        throw new IllegalArgumentException("Some snmp attributes have been defined in counters "
            + ", but no snmp informations have been defined " + " in the @Stat annotation");
      }
    }

    /* FIXME? skip Validate commands
    for (CommandBean c : _commands) {
      // If the command has snmp info, then commands must also have some snmp info
      boolean commandHasSnmpInfo = c.validate(hasRootOid);
      if (commandHasSnmpInfo && !hasRootOid) {
        throw new IllegalArgumentException("Some snmp attributes have been defined in commands "
            + ", but no snmp informations have been defined " + " in the @Commands annotation");
      }
    }*/
  }

  public boolean isPrintable() {
    return (_counters.size() > 0) || ( _alarms.size() > 0) || (_commands.size() > 0 );
  }
  
  void print(PrintWriter pw) {
    // if no counters defined, nothing to print
    if (_counters.size() == 0 && _alarms.size() == 0 && _commands.size() == 0) {
      return;
    }
    Utils.print(pw, "   <mbean name=\"", _className, "\" type=\"", _fullClassName);
    if (_rootSnmpName != null && _rootOid != null) {
      Utils.print(pw, "\" description=\"snmpMapping=\'", _rootSnmpName, ":", Utils.getOid(_rootOid), "'");
    }
    Utils.print(pw, "\">\n");
    for (CounterBean c : _counters) {
      c.print(pw);
    }
    for (AlarmBean alarm : _alarms) {
      alarm.print(pw);
    }
    for (CommandBean command : _commands) {
      command.print(pw);
    }
    Utils.print(pw, "   </mbean>\n");
  }
}
