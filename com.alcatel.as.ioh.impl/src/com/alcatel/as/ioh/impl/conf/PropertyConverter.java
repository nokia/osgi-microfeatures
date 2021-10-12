// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.impl.conf;

import java.util.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.*;

public class PropertyConverter implements Converter {

    public boolean canConvert (Class clazz){
	return clazz.equals (Property.class);
    }

    public void marshal(java.lang.Object target,com.thoughtworks.xstream.io.HierarchicalStreamWriter writer,com.thoughtworks.xstream.converters.MarshallingContext ctx){
	// not used
    }

    public Object unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader reader, com.thoughtworks.xstream.converters.UnmarshallingContext ctx){
	Property property = new Property ();
	property.name = reader.getAttribute ("name");
	property.agent = reader.getAttribute ("system.agent");
	property.value = reader.getValue ();
	if (property.value != null){
	    property.value = property.value.trim ();
	    if (property.value.length () == 0) property.value = null;
	}
	return property;
    }

}