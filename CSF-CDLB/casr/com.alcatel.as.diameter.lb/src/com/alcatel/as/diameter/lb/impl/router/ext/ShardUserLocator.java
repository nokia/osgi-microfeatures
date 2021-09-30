package com.alcatel.as.diameter.lb.impl.router.ext;

import java.util.*;
import org.apache.log4j.Logger;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.impl.router.*;

import org.osgi.service.component.annotations.*;
import com.alcatel_lucent.as.management.annotation.config.*;

@Component(service={UserLocator.class}, property={"locator.id=shard"}, configurationPolicy=ConfigurationPolicy.OPTIONAL)
public class ShardUserLocator extends UserLocator {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.locator.shard");
    private int _nbGroups;
    private String _toString= "ShardUserLocator";

    @Override
    public String toString (){ return _toString;}
    
    @Reference(target="(service.pid=com.alcatel.as.diameter.lb.impl.router.ext.ByUserDiameterRouter)", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void setConf (Dictionary<String, String> conf){
	_nbGroups = Integer.parseInt (conf.get (ByUserDiameterRouter.CONF_NB_GROUPS));
	_toString = "ShardUserLocator["+_nbGroups+"]";
	LOGGER.warn (this+" : ready");
    }
    public void unsetConf (Dictionary<String, String> conf){}

    @Activate
    public void init (){
    }
    
    public void getLocation (byte[] key, int keyOff, int keyLen, java.util.function.Consumer<String> cb){
	int i = 0;
	int keyLimit = keyOff + keyLen;
	for (int k=keyOff; k < keyLimit; k++)
	    i += (int) key[k];
	int group = 1 + ((i & 0x7FFFFFFF) % _nbGroups);
	cb.accept (new StringBuilder ().append ("group-").append (group).toString ());
    }
    
}
