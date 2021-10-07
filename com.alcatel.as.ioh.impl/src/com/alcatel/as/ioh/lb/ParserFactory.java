package com.alcatel.as.ioh.lb;

import java.util.Map;

public interface ParserFactory {

    public Object newParserConfig (Map<String, Object> props);

    public Parser newParser (Object parserConfig, int neededBuffer);

    public default Parser newClientParser (Object routerConfig, int neededBuffer){ return newParser (routerConfig, neededBuffer);}
    public default Parser newServerParser (Object routerConfig, int neededBuffer){ return newParser (routerConfig, neededBuffer);}
    
    
}
