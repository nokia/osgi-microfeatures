package com.nokia.as.k8s.sless;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface EventContext {
    
    public java.util.Map<String, Object> eventParameters ();

    public java.util.Map<String, Object> functionParameters ();
    
    public void log (String log);

    public boolean interrupted ();

    public long ttl ();

    public java.util.concurrent.Executor executor ();
    
}
