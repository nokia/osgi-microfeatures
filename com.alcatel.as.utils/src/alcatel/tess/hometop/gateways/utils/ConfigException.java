package alcatel.tess.hometop.gateways.utils;


/**
 * Class declaration
 *
 *
 * @author
 */
public class ConfigException extends Exception {
  /**
   * Constructor declaration
   *
   * @param resource
   * @param key
   */
  public ConfigException(String msg) {
    super(msg);
  }
  
  /**
   * Constructor declaration
   *
   * @param resource
   * @param key
   */
  public ConfigException(String propertyFile, String why) {
    super(why + " (" + propertyFile + ")");
  }
  
  /**
   * Constructor declaration
   *
   * @param propertyFile
   * @param key
   */
  public ConfigException(String msg, Throwable t) {
    super(msg);
    initCause(t);
  }
  
  /**
   * Constructor declaration
   *
   * @param resource
   * @param key
   */
  public ConfigException(String propertyFile, String entry, String why) {
    super("Error in " + propertyFile + "(" + entry + "): " + why);
  }
  
  /**
   * Constructor declaration
   *
   * @param resource
   * @param key
   */
  public ConfigException(String propertyFile, String entry, String why, Throwable t) {
    super("Error in " + propertyFile + " (" + entry + ") " + why);
    initCause(t);
  }
  
  /**
   * Constructor declaration
   *
   * @param propertyFile
   * @param key
   */
  public ConfigException(String propertyFile, String entry, Throwable t) {
    super("Error in " + propertyFile + " (" + entry + ")");
    initCause(t);
  }
}
