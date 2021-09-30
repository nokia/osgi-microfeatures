package com.nokia.casr.samples.spellchecker.proxy;

import java.util.Map;

public interface SpellCheckerProxyConfiguration {	
	/**
	 * Returns the spell checkers Map. Keys=lang, value=spellchecker address to use
	 */
	Map<String, String> getSpellcheckers();
}
