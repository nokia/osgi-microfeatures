package alcatel.tess.hometop.gateways.tracer;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.Hashtable;

/**
 * Base class for all log events. A log event may regroups all
 * log informations (for example: string message, date, stacktrace, etc...).
 */
class ConfigEvent extends LogEvent {
  
  ConfigEvent(Config cnf, String propertyChanged[]) {
    this.cnf = cnf;
    this.propertyChanged = propertyChanged;
  }
  
  Config getConfig() {
    return (cnf);
  }
  
  String[] getPropertyChanged() {
    return (propertyChanged);
  }
  
  Tracer getTracer() {
    return (null);
  }
  
  int getLevel() {
    return (0);
  }
  
  public String toString(Hashtable contexts) {
    return (toString());
  }
  
  public String toString() {
    return (ConfigEvent.class.getName());
  }
  
  public void release() {
  }
  
  private Config cnf;
  private String propertyChanged[];
}
