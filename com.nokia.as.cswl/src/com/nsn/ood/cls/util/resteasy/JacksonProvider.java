package com.nsn.ood.cls.util.resteasy;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.apache.felix.dm.annotation.api.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.nsn.ood.cls.model.internal.json.InternalHalModule;
import com.nsn.ood.cls.model.json.HalModule;
import com.nsn.ood.cls.model.json.JodaModule;

@Component(provides = JacksonJaxbJsonProvider.class)
@Provider
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class JacksonProvider extends JacksonJaxbJsonProvider {
	
	private static ObjectMapper om = new ObjectMapper()
											.configure(SerializationFeature.INDENT_OUTPUT, true)
											.registerModules(new JodaModule(), new HalModule(), new InternalHalModule());

	public JacksonProvider() {
        super();
        setMapper(om);
    }
}
