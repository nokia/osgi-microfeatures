package com.alcatel.as.http.proxy;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.alcatel.as.http.parser.HttpMessage;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"plugin.id=def"})
public class DefHttpProxyPluginFactory implements HttpProxyPluginFactory {
    
    public Object newPluginConfig (Map<String, Object> props){
	return "";
    }

    public HttpProxyPlugin newPlugin (Object config){
	return INSTANCE;
    }

    private static DefHttpProxyPlugin INSTANCE = new DefHttpProxyPlugin ();

    private static class DefHttpProxyPlugin implements HttpProxyPlugin {
	public HttpMessage handle (ClientContext ctx, HttpMessage message){
	    return message;
	}

    }
}
