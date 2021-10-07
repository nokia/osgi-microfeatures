package com.alcatel.as.service.mbeanparser.impl;

import java.util.Map;

import com.alcatel.as.service.metatype.MetaData;
import com.alcatel.as.service.metatype.PropertiesDescriptor;
import com.alcatel.as.service.metatype.CounterDescriptor;
import com.alcatel.as.service.metatype.AlarmDescriptor;
import com.alcatel.as.service.metatype.CommandDescriptor;

public class MetaDataImpl implements MetaData
{
  private Map<String, PropertiesDescriptor> _properties;
  private Map<String, CounterDescriptor> _counters;
  private Map<String, AlarmDescriptor> _alarms;
  private Map<String, CommandDescriptor> _commands;

  final String _bn, _bsn, _bv;

  public MetaDataImpl(String bn, String bsn, String bv)
  {
    _bn = bn;
    _bsn = bsn;
    _bv = bv;
  }

  public String getBundleName() { return _bn; }
  public String getBundleSymbolicName() { return _bsn; }
  public String getBundleVersion() { return _bv; }

  public boolean isEmpty() 
  {
    return (_properties == null || _properties.isEmpty())
        && (_counters == null || _counters.isEmpty())
        && (_alarms == null || _alarms.isEmpty())
        && (_commands == null || _commands.isEmpty());
  }

  public void setProperties(Map<String, PropertiesDescriptor> props)
  {
    _properties = props;
  }
  public void setCommands(Map<String, CommandDescriptor> commands)
  {
    _commands = commands;
  }
  public void setCounters(Map<String, CounterDescriptor> counters)
  {
    _counters = counters;
  }
  public void setAlarms(Map<String, AlarmDescriptor> alarms)
  {
    _alarms = alarms;
  }

  public Map<String, PropertiesDescriptor> getProperties()
  {
    return _properties;
  }

  public Map<String, CommandDescriptor> getCommands()
  {
    return _commands;
  }

  public Map<String, CounterDescriptor> getCounters()
  {
    return _counters;
  }

  public Map<String, AlarmDescriptor> getAlarms() 
  {
    return _alarms;
  }
}
