package alcatel.tess.hometop.gateways.tracer;

import java.io.IOException;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

/**
 * Standard Handler for log events.
 *
 */
public class StandardHandler implements Handler {
  
  /**
   * Creates a new <code>StandardHandler</code> instance.
   *
   * @param params an <code>Object[]</code> value
   * @exception Exception if an error occurs
   */
  public StandardHandler() {
  }
  
  public void init(Config cnf, String applInstance, String name) throws ConfigException {
    this.applInstance = applInstance.replace(' ', '_');
    this.name = name;
  }
  
  /**
   * Reloads changed properties.
   *
   * @param cnf a <code>Config</code> value
   * @param props a <code>String[]</code> value
   */
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
  }
  
  /**
   * Handles a log and redirect it to a file.
   *
   * @param le a <code>LogEvent</code> value
   * @return a <code>boolean</code> value
   * @exception IOException if an error occurs
   */
  public boolean handleLog(LogEvent le) {
    try {
      TracerBox.out.print(le.toString());
      TracerBox.out.flush();
    }
    
    catch (Throwable e) {
      return false;
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
    TracerBox.out.flush();
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
  }
  
  /**
   * Describe <code>toString</code> method here.
   *
   * @return a <code>String</code> value
   */
  public String toString() {
    return ("[StandardHandler]");
  }
  
  public String getName() {
    return (this.name);
  }
  
  private String applInstance;
  private String name;
}
