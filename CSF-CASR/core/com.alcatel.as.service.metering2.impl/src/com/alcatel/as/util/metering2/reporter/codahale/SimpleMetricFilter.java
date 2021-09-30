package com.alcatel.as.util.metering2.reporter.codahale;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class SimpleMetricFilter implements MetricFilter {
  private volatile String _filters;   
  
  public void update(String filters) {
    if (filters != null) {
      filters = filters.trim();
    }
    _filters = filters;
  }
  
  @Override
  public boolean matches(String name, Metric meter) {
    String filters = _filters;
    
    if (filters == null || filters.equals("*") || filters.length() == 0) {
      return true;
    }
    
    for (String filter : filters.split(",")) {
      filter = filter.trim();      
      int semicolon = filter.indexOf(";");
      if (semicolon == -1) {
        return name.startsWith(filter);
      } else {
        String prefix = filter.substring(0, semicolon);
        String suffix = filter.substring(semicolon+1);
        return name.startsWith(prefix) && name.indexOf(suffix) != -1;              
      }
    }
    
    return false;
  }
}
