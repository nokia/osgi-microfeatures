package alcatel.tess.hometop.gateways.reactor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import com.alcatel.as.util.sctp.SctpSocketOption;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An asynchronous SCTP channel.
 */
@ProviderType
public interface SctpChannel extends AsyncChannel {
  /**
   * Returns all of the socket addresses to which this channel's socket is bound
   * @return All the socket addresses that this channel's socket is bound to.
   * @throws IOException  If an I/O error occurs of ir the channel is closed.
   */
  Set<SocketAddress> getLocalAddresses() throws IOException;
  
  /**
   * Returns all of the remote addresses to which this channel's socket is connected.
   * If the channel is connected to a remote peer that is bound to multiple addresses 
   * then it is these addresses that the channel's socket is connected. 
   * @return  All of the remote addresses to which this channel's socket is connected
   * @throws IOException
   */
  Set<SocketAddress> getRemoteAddresses() throws IOException;
  
  /**
   * Returns the channel remote port number
   * @return the channel remote port number
   */
  int getRemotePort();
  
  /**
   * Returns the association on this channel's socket.
   * @return the association
   * @throws IOException
   */
  SctpAssociation getAssociation() throws IOException;
  
  /**
   * Removes the given address from the bound addresses for the channel's socket.
   * Removing addresses from a connected association is optional functionality. 
   * If the endpoint supports dynamic address reconfiguration then it may send the 
   * appropriate message to the peer to change the peers address lists. 
   * @param address The address to remove from the bound addresses for the socket.
   * @return This channel
   * @throws IOException on any io exception
   */
  SctpChannel unbindAddress(InetAddress address) throws IOException;
  
  /**
   * Sends a message via this channel.
   * 
   * @param copy true if the buffer must be copied in case the socket output becomes stuck.
   * If false, it means that the buffer will be stored in a delayed queue when the socket 
   * output is blocked, and it then must not be modified by the application after this 
   * method has returned.
   * @param addr the preferred destination of the message to be sent, or null
   * @param streamNumber the stream number that the message is to be sent on.
   * @param data the data message
   * @return this channel.
   */
  SctpChannel send(boolean copy, SocketAddress addr, int streamNumber, ByteBuffer ... data);
  
  /**
   * Sends a message via this channel.
   * 
   * @param copy true if the buffer must be copied in case the socket output becomes stuck.
   *         If false, it means that the buffer will be stored in a delayed queue when the socket 
   *         output is blocked, and it then must not be modified by the application after this 
   *         method has returned.
   * @param addr the preferred destination of the message to be sent, or null
   * @param complete Sets whether or not the message is complete
   * @param ploadPID the payload protocol Identifier, or 0 indicate an unspecified payload protocol 
   *        identifier.
   * @param streamNumber the stream number that the message is to be sent on.
   * @param timeToLive The time period that the sending side may expire the message if it 
   *        has not been sent, or 0 to indicate that no timeout should occur.
   * @param unordered true requests the un-ordered delivery of the message, false indicates that the message is ordered.
   * @param data the data message
   * @return this channel.
   */
  SctpChannel send(boolean copy, SocketAddress addr, boolean complete, int ploadPID, int streamNumber,
                   long timeToLive, boolean unordered, ByteBuffer ... data);
  
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
   public SctpChannel setSocketOption(SctpSocketOption option, Object param) throws IOException;
   
   
   /**
    * Controls the action taken when unsent data is queued on the socket and a method to close the socket is invoked. 
    * it represents a Long timeout value, in milliseconds, known as the linger interval. The linger interval is the timeout for the close method to complete 
    * while the operating system attempts to transmit the unsent data. Socket will be forcibly closed after the timeout expires or when all unsent data is flushed.
    * 0 means no linger interval is used. By default, the linger option is set to 5000 milliseconds.
    */
   void setSoLinger(long linger);
}
