package com.nokia.as.service.tlsexport;

import java.util.Map;

import javax.net.ssl.SSLEngine;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface TlsExportService {

    Map<String, Object> exportKey(SSLEngine engine, String asciiLabel, byte[] context_value, int length);
    
}
