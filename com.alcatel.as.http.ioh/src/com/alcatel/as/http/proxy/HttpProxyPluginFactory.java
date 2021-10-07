package com.alcatel.as.http.proxy;

import java.util.Map;

public interface HttpProxyPluginFactory {
    
    public Object newPluginConfig (Map<String, Object> props);

    public HttpProxyPlugin newPlugin (Object config);
    
}
