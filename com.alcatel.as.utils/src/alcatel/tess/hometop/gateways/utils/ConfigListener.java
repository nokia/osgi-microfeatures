package alcatel.tess.hometop.gateways.utils;

public interface ConfigListener {
  public void propertyChanged(Config cnf, String propertyNames[]) throws ConfigException;
}
