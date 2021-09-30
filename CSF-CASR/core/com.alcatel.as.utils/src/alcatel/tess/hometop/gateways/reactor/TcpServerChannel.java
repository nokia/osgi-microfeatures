package alcatel.tess.hometop.gateways.reactor;

// Jdk
import java.net.InetSocketAddress;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Server Channel Acceptor. This class accepts tcp connections asynchronously.
 */
@ProviderType
public interface TcpServerChannel extends Channel {
  /**
   * Return the local listened address.
   * @return the local listened address.
   */
  public InetSocketAddress getLocalAddress();
  
  /**
   * Indicates if this server channel is secure or not.
   * @return true if this channel is secure or not
   */
  boolean isSecure();
  
  /**
   * Enable tcp channel reading mode. Call this method if you have configured the tcp server channel
   * with the @link {@link ReactorProvider.TcpServerOption#ENABLE_READ} parameter set to false.
   */
  void enableReading();
  
  /**
   * Updates security parameters for this channel. The new parameters will be applied only for newly accepted
   * connections, not for existing secured connections.
   * @param security the security parameters to set
   * @throws IllegalStateException if the channel is unsecured.
   */
  void updateSecurity(Security security);
}
