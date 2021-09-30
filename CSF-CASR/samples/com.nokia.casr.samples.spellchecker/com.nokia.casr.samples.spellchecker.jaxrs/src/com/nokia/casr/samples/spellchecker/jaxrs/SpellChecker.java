package com.nokia.casr.samples.spellchecker.jaxrs;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;

import com.nokia.casr.samples.service.dictionary.DictionaryService;

/**
 * Our simple SpellChecker web service.
 */
@Component(provides = SpellChecker.class)
@Path("spellcheck")
public class SpellChecker {

	/**
	 * Keep track of all available services, which are added in our COW dictionary.
	 */
	private final Collection<DictionaryService> _dictionaries = new CopyOnWriteArrayList<>();
	
	/**
	 * Our logger.
	 */
	private final Logger _log = Logger.getLogger(SpellChecker.class);
	
	@ServiceDependency(removed="unbind")
	public void bind(DictionaryService dicitonary) {
		_dictionaries.add(dicitonary);
	}
	
	public void unbind(DictionaryService dicitonary) {
		_dictionaries.remove(dicitonary);
	}
	
	/**
	 * Checks if the specified word is valid or not.
	 * @param word the word provided in the "word" query parameter
	 * @return the http response
	 */
	@GET
	@Produces("text/plain")
	public String check(@QueryParam("word") String word) {
		_log.warn("Spell checking word " + word);
		for (DictionaryService dictionary : _dictionaries) {
			if (dictionary.checkWord(word)) {
				_log.warn("word is correct");
				return "Word " + word + " is correct";
			}
		}
		_log.warn("word is misspelled");
		return "Word " + word + " is misspelled";
	}
	
}
