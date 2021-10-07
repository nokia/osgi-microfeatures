package alcatel.tess.hometop.gateways.reactor.impl;

import alcatel.tess.hometop.gateways.reactor.SctpAssociation;

import com.sun.nio.sctp.Association;

public class SctpAssociationImpl implements SctpAssociation {
  private final Association _assoc;
  
  public SctpAssociationImpl(Association association) {
    _assoc = association;
  }
  
  @Override
  public int associationID() {
    return _assoc.associationID();
  }
  
  @Override
  public int maxInboundStreams() {
    return _assoc.maxInboundStreams();
  }
  
  @Override
  public int maxOutboundStreams() {
    return _assoc.maxOutboundStreams();
  }
  
  @Override
  public String toString() {
    return _assoc.toString();
  }
}
