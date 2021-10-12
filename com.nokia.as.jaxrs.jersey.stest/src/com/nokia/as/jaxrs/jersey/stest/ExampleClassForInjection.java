// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.stest;

import org.apache.felix.dm.annotation.api.Component;

@Component(provides = ExampleClassForInjection.class)
public class ExampleClassForInjection {

	public String getInfo() {
		return "info";
	}
}