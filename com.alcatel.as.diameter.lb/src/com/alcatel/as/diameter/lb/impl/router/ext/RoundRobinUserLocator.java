// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.router.ext;

import java.util.*;
import org.apache.log4j.Logger;
import java.util.concurrent.atomic.*;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.impl.router.*;

import org.osgi.service.component.annotations.*;
import com.alcatel_lucent.as.management.annotation.config.*;

@Component(service={UserLocator.class}, property={"locator.id=round-robin"}, configurationPolicy=ConfigurationPolicy.OPTIONAL)
public class RoundRobinUserLocator extends UserLocator {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.locator.round-robin");
    private int _nbGroups;
    private String _toString= "RoundRobinUserLocator";
    private AtomicInteger _count = new AtomicInteger (0);

    @Override
    public String toString (){ return _toString;}

    @Reference(target="(service.pid=com.alcatel.as.diameter.lb.impl.router.ext.ByUserDiameterRouter)", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void setConf (Dictionary<String, String> conf){
	_nbGroups = Integer.parseInt (conf.get (ByUserDiameterRouter.CONF_NB_GROUPS));
	_toString = "RoundRobinUserLocator["+_nbGroups+"]";
	LOGGER.warn (this+" : ready");
    }
    public void unsetConf (Dictionary<String, String> conf){}

    @Activate
    public void init (){
    }
    
    public void getLocation (byte[] key, int keyOff, int keyLen, java.util.function.Consumer<String> cb){
	int i = _count.getAndIncrement ();
	int group = 1 + ((i & 0x7FFFFFFF) % _nbGroups);
	cb.accept (new StringBuilder ().append ("group-").append (group).toString ());
    }
    
}
