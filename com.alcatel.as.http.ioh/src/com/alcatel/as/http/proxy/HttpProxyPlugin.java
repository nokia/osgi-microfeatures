package com.alcatel.as.http.proxy;

import com.alcatel.as.http.parser.HttpMessage;

public interface HttpProxyPlugin {

    public HttpMessage handle (ClientContext ctx, HttpMessage message);
    
}
