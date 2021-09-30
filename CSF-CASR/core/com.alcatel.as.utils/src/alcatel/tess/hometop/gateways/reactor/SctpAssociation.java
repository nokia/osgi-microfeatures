package alcatel.tess.hometop.gateways.reactor;

/**
 * SCTP Association.
 * An association exists between two SCTP endpoints. Each endpoint is represented by a list 
 * of transport addresses through which that endpoint can be reached and from which it will 
 * originate SCTP messages. The association spans over all of the possible source/destination 
 * combinations which may be generated from each endpoint's lists of addresses.
 * Associations are identified by their Association ID. Association ID's are guaranteed to be 
 * unique for the lifetime of the association. An association ID may be reused after the 
 * association has been shutdown. An association ID is not unique across multiple SCTP channels. An Association's local and remote addresses may change if the SCTP implementation supports Dynamic Address Reconfiguration
 *
 */
public interface SctpAssociation {
  /**
   * Returns the associationID.
   * @return The association ID
   */
  int associationID();
  
  /**
   * Returns the maximum number of inbound streams that this association supports.
   * Data received on this association will be on stream number s, where 0 <= s < maxInboundStreams(). 
   * @return The maximum number of inbound streams
   */
  int maxInboundStreams();
  
  /**
   * Returns the maximum number of outbound streams that this association supports.
   * Data sent on this association must be on stream number s, where 0 <= s < maxOutboundStreams(). 
   * @return The maximum number of outbound streams
   */
  int maxOutboundStreams();
}
