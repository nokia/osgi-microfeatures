package com.nokia.casr.samples.dictionary.english;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import com.nokia.casr.samples.service.dictionary.DictionaryService;

/**
 * English dictionary implementation.
 */
@Component
@Property(name="lang", value="en")
public class EnglishDictionary implements DictionaryService {

	/**
	 * This is the english words loaded from our configuration.
	 */
	final List<String> _words = Arrays.asList("hello", "hi");

	/**
	 * Our logger
	 */
	final static Logger _log = Logger.getLogger(EnglishDictionary.class);

	/**
	 * Starts and configures our spell checker service using an optional
	 * configuration.
	 */
	@Start
	void start() {
		_log.warn("English Dictionay started: known words=" + _words);
	}

	@Override
	public boolean checkWord(String word) {
		_log.warn("checking word " + word + "(known words=" + _words + ")");
		return this._words.indexOf(word) != -1;
	}

}
