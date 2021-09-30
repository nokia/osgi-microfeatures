package com.nextenso.proxylet.diameter.dictionary;

import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition.Dictionary;
import com.nextenso.proxylet.diameter.nasreq.NASApplicationConstants;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface DiameterDictionaryFactory {
	/**
	 * Thrown if an error occurred when reading or parsing the passed data
	 */
	public static class LoadingException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public LoadingException(String message, Throwable cause) {
			super(message, cause);
		}

		public LoadingException(String message) {
			super(message);
		}
	}

	/**
	 * Load a Diameter AVP dictionary from JSON read from the given Reader.
	 * 
	 * @param reader a Reader from which the JSON will be read
	 * @return a {@link DiameterAVPDictionary DiameterAVPDictionary}
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 */
	DiameterAVPDictionary loadAVPDictionaryFromJSON(Reader reader) throws LoadingException;
	
	/**
	 * Load an AVP Dictionary JSON declaration into the system dictionary
	 * 
	 * @param reader a Reader from which the JSON will be read
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 *  or if there is a duplicate declaration
	 */
	default void loadJSONIntoSystemAVPDictionary(Reader reader) throws LoadingException {
		DiameterAVPDictionary dic = loadAVPDictionaryFromJSON(reader);
		DiameterBaseConstants.init();
		NASApplicationConstants.init();
		
		for(DiameterAVPDefinition def : dic.getAVPDefList()) {
			try {
				Dictionary.registerDiameterAVPDefinition(def);
			} catch(Exception e) {
				throw new LoadingException("failed to register definition to the system dictionary", e);
			}
		}
	}
	
	/**
	 * Load a Diameter AVP dictionary from JSON contained in the passed String.
	 * @param str the JSON to read
	 * @return a {@link DiameterAVPDictionary DiameterAVPDictionary}
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 */
	default DiameterAVPDictionary loadAVPDictionaryFromJSON(String str) throws LoadingException {
		Objects.requireNonNull(str);
		return loadAVPDictionaryFromJSON(new StringReader(str));
	}
	/**
	 * Load a Diameter command dictionary from JSON read from the given Reader
	 * @param reader a Reader from which the JSON will be read
	 * @param dico the Diameter AVP dictionary used when retrieving the AVP definitions from their name
	 * @return a {@link DiameterCommandDictionary DiameterCommandDictionary}
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 */
	DiameterCommandDictionary loadCommandDictionaryFromJSON(Reader reader, 
			AbstractDiameterAVPDictionary dico) throws LoadingException;
	
	/**
	 * Load a Diameter command dictionary from JSON read from the given Reader. AVPs
	 * definition will be retrieved from the CJDIA system dictionary.
	 * @param reader a Reader from which the JSON will be read
	 * @param dico the Diameter AVP dictionary used when retrieving the AVP definitions from their name
	 * @return a {@link DiameterCommandDictionary DiameterCommandDictionary}
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 */
	default DiameterCommandDictionary loadCommandDictionaryFromJSON(Reader reader) throws LoadingException {
		return loadCommandDictionaryFromJSON(reader, 
				Dictionary.getBackingAVPDictionary());
	}
	/**
	 * Load a Diameter command dictionary from  JSON contained in the passed String. AVPs
	 * definition will be retrieved from the CJDIA system dictionary
	 * @param json the JSON to read
	 * @param dico the Diameter AVP dictionary used when retrieving the AVP definitions from their name
	 * @return a {@link DiameterCommandDictionary DiameterCommandDictionary}
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 */
	default DiameterCommandDictionary loadCommandDictionaryFromJSON(String json) throws LoadingException {
		Objects.requireNonNull(json);
		return loadCommandDictionaryFromJSON(new StringReader(json), 
				Dictionary.getBackingAVPDictionary());
	}
	
	/**
	 * Load a Diameter command dictionary from JSON read from the given Reader
	 * @param json the JSON to read
	 * @param avpDico the Diameter AVP dictionary used when retrieving the AVP definitions from their name
	 * @return a {@link DiameterCommandDictionary DiameterCommandDictionary}
	 * @throws LoadingException if an error occurred while reading or parsing the JSON
	 */
	default DiameterCommandDictionary loadCommandDictionaryFromJSON(String json, AbstractDiameterAVPDictionary avpDico) throws LoadingException {
		Objects.requireNonNull(json);
		return loadCommandDictionaryFromJSON(new StringReader(json), avpDico);
	}
}
