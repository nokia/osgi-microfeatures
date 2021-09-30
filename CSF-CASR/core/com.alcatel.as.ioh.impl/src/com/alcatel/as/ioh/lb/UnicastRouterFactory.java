package com.alcatel.as.ioh.lb;

import java.util.Map;

public interface UnicastRouterFactory {

    public Object newUnicastRouterConfig (Map<String, Object> props);

    public UnicastRouter newUnicastRouter (Object routerConfig);
    
}
