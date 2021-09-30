package com.nextenso.http.agent;

import static com.nextenso.proxylet.admin.http.HttpBearer.REQUEST_CHAIN;
import static com.nextenso.proxylet.admin.http.HttpBearer.RESPONSE_CHAIN;
import static com.nextenso.proxylet.admin.http.HttpBearer.SESSION_LISTENER;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.nextenso.http.agent.engine.criterion.HttpCriterionParser;
import com.nextenso.proxylet.admin.Bearer.Factory;
import com.nextenso.proxylet.admin.http.HttpBearer;
import com.nextenso.proxylet.engine.DeployerDescriptor;
import com.nextenso.proxylet.engine.criterion.CriterionParser;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.BufferedHttpRequestPushlet;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;
import com.nextenso.proxylet.http.event.HttpSessionListener;

@Component(property = { "protocol=http" })
public class HttpDeployerDescriptor implements DeployerDescriptor {
  protected volatile Dictionary<?, ?> _config;
  
  @Reference(policy = ReferencePolicy.DYNAMIC, target = "(service.pid=httpagent)")
  void bindConfig(Dictionary<?, ?> config) {
    _config = config;
  }

  void unbindConfig(Dictionary<?, ?> config) {
  }    
  
  @Override
  public String getProtocol() {
    return "http";
  }
  
  @Override
  public CriterionParser getParser() {
    return new HttpCriterionParser();
  }
  
  @Override
  public Factory getBearerFactory() {
    return new HttpBearer();
  }
  
  @Override
  public String getProxyletsConfiguration() {
    return (String) _config.get("httpagent.proxylets");
  }
  
  @SuppressWarnings({ "serial", "rawtypes" })
  @Override
  public Map<String, Class[]> getBindings() {
    return new Hashtable<String, Class[]>() {
      {
        put(REQUEST_CHAIN, new Class[] {
            //HttpRequestProxylet.class,
            BufferedHttpRequestProxylet.class, BufferedHttpRequestPushlet.class,
            StreamedHttpRequestProxylet.class });
        put(RESPONSE_CHAIN, new Class[] {
            //HttpResponseProxylet.class,
            BufferedHttpResponseProxylet.class, StreamedHttpResponseProxylet.class });
        put(SESSION_LISTENER, new Class[] { HttpSessionListener.class });
      }
    };
  }
}
