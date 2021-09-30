package com.nokia.casr.samples.spellchecker.proxy;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpRequestProlog;

/**
 * Proxies a given request to the appropriate spellchecker remote server.
 * We simply extract the "lang" request parameter and the request is then proxied
 * based on the language found in lang parameter.
 */
@Component
public class SpellCheckerProxy implements BufferedHttpRequestProxylet {

	private final Logger _log = Logger.getLogger(SpellCheckerProxy.class);
	volatile Map<String, String> _spellcheckers;
	
	@ConfigurationDependency
	void updated(SpellCheckerProxyConfiguration conf) {
		_spellcheckers = conf.getSpellcheckers();
		_log.warn("Initialized SpellCheckerProxy: conf=" + _spellcheckers);
	}

	@Override
	public void init(ProxyletConfig cnf) throws ProxyletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public String getProxyletInfo() {
		return "SpellCheckerProxy";
	}

	@Override
	public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
		return ACCEPT; // no multi threading, else return ACCEPT_MAY_BLOCK
	}

	@Override
	public int doRequest(HttpRequest req) throws ProxyletException {
		
		String lang = Optional.ofNullable((String) req.getProlog().getURL().getParameterValue("lang"))
				.orElse("en");
		String addr = _spellcheckers.get(lang);
		
		_log.warn("Forwarding request with lang " + lang  + " to " + addr);
		try {
			req.getProlog().getURL().setAuthority(addr);
		} catch (MalformedURLException e) {
			_log.error("Could not forward request " + req.getProlog().getURL(), e);
		}
		return NEXT_PROXYLET; // pass this request to the next proxylet in the chain
	}

}
