package alcatel.tess.hometop.gateways.tracer;

import java.io.IOException;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

/**
 * Syslog Handler for log events.
 *
 */
public class SyslogHandler implements Handler {
  
  /**
   * Creates a new <code>SyslogHandler</code> instance.
   *
   * @param params an <code>Object[]</code> value
   * @exception Exception if an error occurs
   */
  public SyslogHandler() {
  }
  
  public SyslogHandler(String applInstance, String name, String host, int port, boolean tcp, long retryDelay,
                       String facility) {
    this.applInstance = applInstance.replace(' ', '_');
    this.name = name;
    this.host = host;
    this.port = port;
    this.tcp = tcp;
    this.retryDelay = retryDelay;
    this.facility = Syslog.getCode(facility);
  }
  
  public void init(Config cnf, String applInstance, String name) throws ConfigException {
    this.cnf = cnf;
    this.applInstance = applInstance.replace(' ', '_');
    this.name = name;
    
    propertyChanged(cnf, new String[] { "tracer.handler." + name + ".syslogHost",
        "tracer.handler." + name + ".syslogPort", "tracer.handler." + name + ".syslogTcp",
        "tracer.handler." + name + ".syslogRetryDelay", "tracer.handler." + name + ".facility", });
  }
  
  public void setFacility(String name) {
    this.facility = Syslog.getCode(name);
  }
  
  public void setFacility(int code) {
    this.facility = code;
  }
  
  /**
   * Reloads changed properties.
   *
   * @param cnf a <code>Config</code> value
   * @param props a <code>String[]</code> value
   */
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
    for (int i = 0; i < props.length; i++) {
      if (Debug.enabled)
        Debug.p(this, "propertyChanged", "prop=" + props[i]);
      
      if (props[i].equalsIgnoreCase("tracer.handler." + name + ".syslogHost")) {
        this.host = cnf.getString("tracer.handler." + name + ".syslogHost");
      } else if (props[i].equalsIgnoreCase("tracer.handler." + name + ".syslogPort")) {
        this.port = cnf.getInt("tracer.handler." + name + ".syslogPort");
      } else if (props[i].equalsIgnoreCase("tracer.handler." + name + ".syslogTcp")) {
        this.tcp = cnf.getBoolean("tracer.handler." + name + ".syslogTcp");
      } else if (props[i].equalsIgnoreCase("tracer.handler." + name + ".syslogRetryDelay")) {
        this.retryDelay = cnf.getLong("tracer.handler." + name + ".syslogRetryDelay");
      } else if (props[i].equalsIgnoreCase("tracer.handler." + name + ".facility")) {
        String s = cnf.getString("tracer.handler." + name + ".facility");
        this.facility = Syslog.getCode(s);
      }
    }
    
    if (syslog == null) {
      if (Debug.enabled)
        Debug.p(this, "propertyChanged", "connecting to syslog(" + host + "," + port + "," + tcp + ")");
      
      try {
        syslog = new Syslog(host, port, tcp);
      }
      
      catch (Throwable t) {
        inError = true;
      }
    }
  }
  
  /**
   * Handles a log and redirect it to a file.
   *
   * @param le a <code>LogEvent</code> value
   * @return a <code>boolean</code> value
   * @exception IOException if an error occurs
   */
  public boolean handleLog(LogEvent le) {
    if (inError == true) {
      if ((System.currentTimeMillis() - lastErrorTime) < retryDelay) {
        return (false);
      }
      
      if (syslog != null) {
        this.syslog.close();
      }
      
      try {
        this.syslog = new Syslog(host, port, tcp);
      }
      
      catch (Exception e) {
        lastErrorTime = System.currentTimeMillis();
        //e.printStackTrace (TracerBox.err);
        return (false);
      }
    }
    
    inError = false;
    String msg = null;
    
    if (le instanceof AccessLogEvent) {
      msg = le.toString();
    } else {
      msg = le.toString().replace('\n', ' ');
    }
    
    //
    // Find the facility.
    //
    TracerImpl t = (TracerImpl) le.getTracer();
    int facility = t.getFacility();
    
    if (facility == -1) {
      facility = this.facility;
    }
    
    try {
      this.syslog.log(facility, le.getLevel(), applInstance, msg);
    }
    
    catch (Exception e) {
      //
      // Try to send once again the syslog message.
      // If we fail again, then wait a little (using the retryDelay param)
      //
      if (syslog != null) {
        this.syslog.close();
      }
      
      try {
        this.syslog = new Syslog(host, port, tcp);
        this.syslog.log(facility, le.getLevel(), applInstance, msg);
      }
      
      catch (Exception e2) {
        if (this.syslog != null) {
          this.syslog.close();
        }
        
        inError = true;
        lastErrorTime = System.currentTimeMillis();
        //e2.printStackTrace (TracerBox.err);
        return (false);
      }
    }
    
    return (true);
  }
  
  /**
   * Describe <code>flush</code> method here.
   *
   * @param info an <code>int</code> value
   * @exception IOException if an error occurs
   */
  public void flush() {
    if (Debug.enabled)
      Debug.p(this, "flush", "flushing syslog sockets");
    
    if (syslog != null) {
      try {
        syslog.flush();
      } catch (IOException e) {
      }
    }
  }
  
  /**
   * Describe <code>flush</code> method here.
   *
   * @param info an <code>int</code> value
   * @exception IOException if an error occurs
   */
  public void clear() {
  }
  
  /**
   * Describe <code>close</code> method here.
   *
   */
  public void close() {
    if (syslog != null) {
      syslog.close();
    }
  }
  
  /**
   * Describe <code>toString</code> method here.
   *
   * @return a <code>String</code> value
   */
  public String toString() {
    return ("[SyslogHandler: " + "host=" + host + ", port=" + port + ", tcp=" + tcp + ", retryDelay="
        + retryDelay + ", facility=" + facility + "]");
  }
  
  public String getName() {
    return (this.name);
  }
  
  private String applInstance;
  private Config cnf;
  private String name;
  
  private static String host;
  private static int port;
  private static boolean tcp;
  private static Syslog syslog;
  private static long retryDelay;
  private int facility;
  private long lastErrorTime;
  private boolean inError;
}
