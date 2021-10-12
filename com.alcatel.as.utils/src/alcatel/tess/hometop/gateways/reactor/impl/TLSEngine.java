// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Class used to encode/decode TLS packet. This class implements a state machine, enabling easy
 * asynchronous TLS encoding/decoding. Here is a sample HTTPS server code which is using this
 * API, when decoding crypted http requests: <blockquote>
 * 
 * <pre>
 * class Server
 * {
 *     private TLSEngine _tlsm;
 * 
 *     handleSocketMessage(ByteBuffer cryptedMessage)
 *     {
 *         // Decode this encrypted message (which is either a handshake message, or a crypted http request)
 *         _tlsm.fillsDecoder(cryptedMessage);
 * 
 *         TLSEngine.Status status;
 *         while ((status = _tlsm.run()) != TLSEngine.Status.NEEDS_INPUT)
 *         {
 *             switch (status) {
 *             case DECODED:
 *                 // Display the decoded http request.
 * 
 *                 System.out.println(&quot;Receive http request: -------------------------------&gt;\n\n&quot;
 *                         + _charset.decode(_tlsm.getDecodedBuffer()));
 * 
 *                 // Reply with a simple http response:
 * 
 *                 _tlsm.fillsEncoder(_charset.encode(httpResponse));
 *                 break;
 * 
 *             case ENCODED:
 *                 // This message is either a handshake message, or our encoded http reply: sent it !
 *                 send(_tlsm.getEncodedBuffer());
 *                 break;
 *             }
 *         }
 *     }
 * 
 *     void send(ByteBuffer buf)
 *     {
 *         // send the encrypted http response back to the client
 *     }
 * }
 * 
 * </pre>
 * 
 * </blockquote>
 */
public interface TLSEngine {
  /**
   * The following status are returned by the <code>run</code> method.
   */
  public enum Status {
    /**
     * Means that the getEncodedBuffer method can be called in order to retrieve an encoded
     * message.
     */
    ENCODED,
    
    /**
     * Means that the getDecodedBuffer method can be called in order to retrieve a decoded
     * message.
     */
    DECODED,
    
    /**
     * Means that this state machine needs some input data in order to proceed with
     * encoding/decoding.
     */
    NEEDS_INPUT,
    
    /** The State machine is closed, the network connection can be closed */
    CLOSED
  }
  
  /**
   * Fills the tls machine with some data which must be encoded.
   * @param buf the message to be encoded by the engine
   */
  void fillsEncoder(ByteBuffer buf);
  
  /**
   * Fills the tls machine with some data which must be decoded.
   * @param buf the message to be decoded by the engine.
   */
  void fillsDecoder(ByteBuffer buf);
  
  /**
   * Fills the tls machine with some data which must be encoded.
   * @param buf the message to be encoded by the engine
   * @param attachment the attachment associated to the buffer
   */
  void fillsEncoder(ByteBuffer buf, Object attachment);
  
  /**
   * Fills the tls machine with some data which must be decoded.
   * @param buf the message to be decoded by the engine.
   * * @param attachment the attachment associated to the buffer
   */
  void fillsDecoder(ByteBuffer buf, Object attachment);
  
  /**
   * Run the tls machine.
   * 
   * @return <br>
   *         ENCODED</br> if a message has been encoded. If so, the getEncodedBuffer() method
   *         has to be called, and the encoded message has to be sent to the remote peer.
   *         <p>
   *         <br>
   *         DECODED</br> if a message has been fully decoded. If so, the getDecodedBuffer()
   *         method has to be called in order to handle the decoded message.
   *         <p>
   *         <br>
   *         NEEDS_INPUT</br> if some tls messages (handshake) have to be received before
   *         proceeding. When this status is returned, the run() method must not be called
   *         anymore, until we receive some crypted data from the network.
   * @throws IOException when a message could not be encoded/decoded.
   */
  Status run() throws IOException;
  
  /**
   * Get the decoded message. This method has to be called only when the run method returns
   * the DECODED status.
   * 
   * @return a decoded message which can be handled by the application.
   */
  ByteBuffer getDecodedBuffer();
  
  /**
   * Get the encoded message. This method has to be called only when the run method returns
   * the ENCODED status.
   * 
   * @return an encoded message ready to be sent out.
   */
  ByteBuffer getEncodedBuffer();
  
  /**
   * Get the attachment associated to the decoded buffer
   */
  Object getDecodedAttachment();
  
  /**
   * Get the attachment associated to the encoded buffer
   */
  Object getEncodedAttachment();
  
  /**
   * Close all internal resources.
   * 
   * @throws IOException
   */
  void close();
}
