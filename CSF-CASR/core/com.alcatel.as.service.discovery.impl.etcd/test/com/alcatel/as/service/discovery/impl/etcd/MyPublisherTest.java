package com.alcatel.as.service.discovery.impl.etcd;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.util.config.ConfigConstants;

@Component(provides=Object.class)
@Property(name=CommandProcessor.COMMAND_SCOPE, value="asr.publisher.etcd")
@Property(name=CommandProcessor.COMMAND_FUNCTION, value="stop")
public class MyPublisherTest {

	@Inject
	BundleContext _bc;
	
	private final int MODULE_ID = 12;
	
	private Set<String> _moduleNameList = new HashSet<>();

	List<ServiceRegistration<Advertisement>> advertList = new LinkedList<>();
	
	@Start
	void start() {
		Integer nbServices = 3;
		for (int i = 0; i < nbServices; i++) {
			Advertisement advert = generateAd();
			Hashtable<String, Object> options = generateOptions();
			advertList.add(_bc.registerService(Advertisement.class, advert, options));
		}
	}
	
	@Stop
	public void stop() {
		for (ServiceRegistration<Advertisement> advert : advertList){
			advert.unregister();	
		}
	}
	
	private Advertisement generateAd(){
		Random r = new Random();
		String address =  r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
		return new Advertisement(address, r.nextInt(9999));
	}
	
	private Hashtable<String, Object> generateOptions(){
		Random r = new Random();
		String moduleName = generateRandomString();
		
		Hashtable<String, Object> options = new Hashtable<>();
		options.put(ConfigConstants.MODULE_NAME, moduleName);
		options.put(ConfigConstants.MODULE_ID, MODULE_ID);
		options.put("type", "ioh");
		return options;
	}
	
	public String generateRandomString()
	{
	    String randomStrings = new String();
	    Random random = new Random();
        do {
		    char[] word = new char[random.nextInt(12)+3]; // words of length 3 through 10. (1 and 2 letter words are boring.)
	        for(int j = 0; j < word.length; j++)
	        {
	            word[j] = (char)('a' + random.nextInt(26));
	        }
	        randomStrings = new String(word);
        } while(_moduleNameList.contains(randomStrings));
	    return randomStrings;
	}
}
