package com.nokia.casr.samples.service.dictionary;

/**
 * This is the contract of a simple Dictionary service. Multiple dictionaries
 * may be available at runtime, each one having a "lang" osgi service property.
 */
public interface DictionaryService {
	
	/**
	 * Checks if a word is correctly spelled.
	 * @param word
	 * @return true if the word is known
	 */
	boolean checkWord(String word);
}
