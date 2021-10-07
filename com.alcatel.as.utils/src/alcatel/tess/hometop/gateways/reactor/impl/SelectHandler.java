package alcatel.tess.hometop.gateways.reactor.impl;

import java.nio.channels.SelectionKey;

/**
 * Notify a socket listener about IO events.
 */
public interface SelectHandler {
  public void selected(SelectionKey key);
  
  public int getPriority();
}
