/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Client <-> String
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "client")
@Property(name = "to", value = "string")
public class Client2StringConverter implements Converter<Client, String> {

	@Override
	public String convertTo(final Client client) {
		if (client == null) {
			throw new CLSIllegalArgumentException("Client must not be null");
		}

		return new DescriptionBuilder()//
				.append("clientId", client.getClientId())//
				.append("keepAliveTime", client.getKeepAliveTime())//
				.append("targetType", client.getTargetType())//
				.build();
	}

	@Override
	public Client convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
