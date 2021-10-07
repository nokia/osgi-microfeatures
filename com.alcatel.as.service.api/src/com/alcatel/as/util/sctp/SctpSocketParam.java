package com.alcatel.as.util.sctp;

import java.io.Externalizable;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface SctpSocketParam extends Externalizable {

    public SctpSocketParam merge (SctpSocketParam other);

}
