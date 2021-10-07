package com.nokia.as.service.discovery.k8s.etcd;

import java.util.List;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(section = "Etcd")
public interface K8sEtcdConfiguration {
    
    public final static String DEF_SERVER_URL = "http://127.0.0.1:2379";

    public final static String DEF_NAMESPACE = "[casr]";
    
    @StringProperty(title = "Etcd server url", defval = DEF_SERVER_URL, help = "Etcd server url.", dynamic = false, required=false)
    public static final String SERVER_URL = "server.url";

    @StringProperty(title = "Namespace",defval = DEF_NAMESPACE, help = "Namespaces we're listening to.", dynamic = false, required=false)
    public static final String NAMESPACES = "namespaces";
    
    String getServerUrl();
    
    List<String> getNamespaces();
}
