// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.util.osgi.weaving;

import java.util.List;

import org.aspectj.weaver.loadtime.DefaultWeavingContext;
import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.tools.WeavingAdaptor;
import org.osgi.framework.wiring.BundleWiring;

public class AspectContext extends DefaultWeavingContext {

	protected BundleWiring wiring;

	protected volatile List<Definition> definitionList;

	protected String rootConfig = "META-INF/aop.xml";

	public AspectContext(BundleWiring wiring) {
		super(wiring.getClassLoader());
		this.wiring = wiring;
	}

	@Override
	public String getId() {
		return getClassLoaderName();
	}

	@Override
	public String getClassLoaderName() {
		return wiring.getRevision().getSymbolicName();
	}

	@Override
	public List<Definition> getDefinitions(final ClassLoader loader, final WeavingAdaptor adaptor) {
		if (definitionList == null) {
			definitionList = AspectSupport.definitionList(loader, rootConfig);
		}
		return definitionList;
	}

}