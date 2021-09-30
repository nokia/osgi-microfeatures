package com.nokia.casr.samples.spellchecker.jaxrs;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;

/**
 * Gogo shell command used to test our SpellChecker web service under gogo shell
 */
@Component(provides=Object.class)
@Property(name=CommandProcessor.COMMAND_SCOPE, value="spellcheck")
@Property(name=CommandProcessor.COMMAND_FUNCTION, value="checkWord")
@Descriptor("Spell Checker")
public class Shell {
	
	@ServiceDependency
	volatile SpellChecker _spellChecker;
	
	@Descriptor("Checks if a word is crrect")
	public void checkWord(			
			@Descriptor("word") 
			String word) 
	{	
		System.out.println(_spellChecker.check(word));
	}
	
}
