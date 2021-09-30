package com.nokia.casr.samples.spellchecker.proxylet;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpResponse;
import com.nokia.casr.samples.service.dictionary.DictionaryService;

/**
 * Our simple SpellChecker proxylet, which is registered in the osgi service
 * registry. The http proxylet container will wait for our proxylet before
 * starting, because we have a special "X-Exports-Http-Proxylets" manifest
 * header (see bnd.bnd)
 */
@Component
public class SpellCheckerProxylet implements BufferedHttpRequestProxylet {

	/**
	 * Keep track of all available services, which are added in our COW dictionary.
	 */
	private final Collection<DictionaryService> _dictionaries = new CopyOnWriteArrayList<>();

	private final Logger _log = Logger.getLogger(SpellCheckerProxylet.class);

	@ServiceDependency(removed="unbindDictionary")
	void bindDictionary(DictionaryService dictionary) {
		_log.warn("Bound dictionary: " + dictionary);
		_dictionaries.add(dictionary);
	}

	void unbindDictionary(DictionaryService dictionary) {
		_dictionaries.remove(dictionary);
	}

	@Override
	public void init(ProxyletConfig cnf) throws ProxyletException {
		_log.warn("SpellCheckerProxylet initialized: " + this);
	}

	@Override
	public void destroy() {
	}

	@Override
	public String getProxyletInfo() {
		return "SpellChecker";
	}

	@Override
	public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
		return ACCEPT; // no multi threading, else return ACCEPT_MAY_BLOCK
	}

	@Override
	public int doRequest(HttpRequest req) throws ProxyletException {
		String checked = check((String) req.getProlog().getURL().getParameterValue("word"));
		HttpResponse resp = req.getResponse();
		resp.getProlog().setStatus(200);
		resp.getHeaders().setHeader("Content-Type", "text/plain");
		resp.getBody().setContent(checked);
		return RESPOND_FIRST_PROXYLET; // return response, but run our response chain proxylet before.
	}

	/**
	 * Checks if the specified word is valid or not.
	 * 
	 * @param word
	 *            the word provided in the "word" query parameter
	 * @return the http response
	 */
	public String check(String word) {
		for (DictionaryService dictionary : _dictionaries) {
			if (dictionary.checkWord(word)) {
				return "Word " + word + " is correct";
			}
		}
		return "Word " + word + " is not correct";
	}

}
