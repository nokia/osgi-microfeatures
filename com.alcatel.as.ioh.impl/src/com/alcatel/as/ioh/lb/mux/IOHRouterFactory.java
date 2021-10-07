package com.alcatel.as.ioh.lb.mux;

import java.util.Map;

public interface IOHRouterFactory {

    public Object newIOHRouterConfig (Map<String, Object> props);

    public IOHRouter newIOHRouter (Object routerConfig);
    
}
