package com.nokia.casr.samples.dictionary.es;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;

import com.nokia.casr.samples.service.dictionary.DictionaryService;

/**
 * Spanish dictionary implementation.
 */
@Component
@Property(name="lang", value="es")
public class SpanishDictionary implements DictionaryService {

	/**
	 * This is the spanish words loaded from our configuration.
	 */
	final List<String> _words = Arrays.asList("hola");

	/**
	 * Our logger
	 */
	final static Logger _log = Logger.getLogger(SpanishDictionary.class);

	/**
	 * Starts and configures our spell checker service using an optional
	 * configuration.
	 */
	@Start
	void start() {
		_log.warn("Spanish Dictionay started: known words=" + _words);
	}

	@Override
	public boolean checkWord(String word) {
		_log.warn("checking word " + word + "(known words=" + _words + ")");
		return this._words.indexOf(word) != -1;
	}

}
