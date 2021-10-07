package com.nextenso.diameter.agent;

import static com.nextenso.proxylet.engine.ProxyletApplication.REQUEST_CHAIN;
import static com.nextenso.proxylet.engine.ProxyletApplication.RESPONSE_CHAIN;
import static com.nextenso.proxylet.engine.ProxyletApplication.SESSION_LISTENER;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nextenso.diameter.agent.engine.criterion.DiameterCriterionParser;
import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.diameter.DiameterBearer;
import com.nextenso.proxylet.diameter.DiameterRequestProxylet;
import com.nextenso.proxylet.diameter.DiameterResponseProxylet;
import com.nextenso.proxylet.diameter.event.DiameterSessionListener;
import com.nextenso.proxylet.engine.DeployerDescriptor;
import com.nextenso.proxylet.engine.criterion.CriterionParser;

@SuppressWarnings("rawtypes")
@Component(properties=@Property(name="protocol", value="diameter"))
public class DiameterDeployerDescriptor implements DeployerDescriptor {

	@ServiceDependency(filter="(service.pid=diameteragent)")
	Dictionary _config; //injected
    
	Map<String, Class[]> _bindings = new HashMap<String, Class[]>();

    public DiameterDeployerDescriptor() {
        _bindings.put(REQUEST_CHAIN, new Class[] { DiameterRequestProxylet.class });
        _bindings.put(RESPONSE_CHAIN, new Class[] { DiameterResponseProxylet.class });
        _bindings.put(SESSION_LISTENER, new Class[] { DiameterSessionListener.class });
    }

    public String getProtocol() {
        return "diameter";
    }

    public CriterionParser getParser() {
        return new DiameterCriterionParser();
    }

    public Map<String, Class[]> getBindings() {
        return _bindings;
    }

    public String getProxyletsConfiguration() {
        return (String) _config.get("diameteragent.proxylets");
    }

    public Bearer.Factory getBearerFactory() {
        return new DiameterBearer();
    }
}
