package alcatel.tess.hometop.gateways.reactor;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

import com.sun.nio.sctp.SctpStandardSocketOptions;

import com.alcatel.as.util.sctp.SctpSocketOption;

@ProviderType
public interface SctpServerChannel extends Channel {
  Set<SocketAddress> getAllLocalAddresses() throws IOException;
  
  /**
   * Enable tcp channel reading mode. Call this method if you have configured the sctp server channel
   * with the @link {@link ReactorProvider.SctpServerOption#ENABLE_READ} parameter set to false.
   */
  void enableReading();

  /**
   * Is this channel secure (i.e. using DTLS)?
   *
   * @return whether the channel is secure or not
   */
   public boolean isSecure();  
   
   /**
	 * Gets the specified socket option from a SctpChannel </br>
	 * At the moment, the only option that uses 'extra' is 
	 * SCTP_GET_PEER_ADDR_INFO, which takes an InetSocketAddress
	 * for the peer which we want the info
	 * @param option the option to set
	 * @param extra some options may need an extra parameter
	 * @return an object as defined in {@link SctpSocketOption}
	 */
  public <T> T getSocketOption(SctpSocketOption option, Object extra) throws IOException;
  
  /**
	 * Sets the specified socket option to a SctpChannel
	 * @param option the option to set
	 * @param param the parameter to give to the option, as defined in {@link SctpSocketOption}
	 * @return itself for chaining
	 */
  public SctpServerChannel setSocketOption(SctpSocketOption option, Object param) throws IOException;
  
  /**
   * Updates security parameters for this channel. The new parameters will be applied only for newly accepted
   * connections, not for existing secured connections.
   * @param security the security parameters to set
   * @throws IllegalStateException if the channel is unsecured.
   */
  void updateSecurity(Security security);
}
