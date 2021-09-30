package com.alcatel.as.ioh.engine;

import com.alcatel.as.service.concurrent.*;
import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.spi.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.recorder.*;
import com.alcatel.as.service.metering2.util.*;
import com.alcatel.as.ioh.server.ServerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.framework.*;
import org.osgi.service.event.*;

@Component(service={IOHServices.class}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class IOHServices {

    private PlatformExecutors _executors;
    private ReactorProvider _reactorProvider;
    private MeteringService _metering;
    private MeteringRegistry _meteringRegistry;
    private EventAdmin _eventAdmin;
    private ServerFactory _serverFactory;
    private RecorderService _recorderService;
    private ChannelListenerFactory _socks4Factory, _socks5Factory;

    @Reference
    public void setRecorderService (RecorderService recorder){
	_recorderService = recorder;
    }
    
    @Reference(target="(type=socks4)")
    public void setSocks4Factory (ChannelListenerFactory socks4Factory){
	_socks4Factory = socks4Factory;
    }

    @Reference(target="(type=socks5)")
    public void setSocks5Factory (ChannelListenerFactory socks5Factory){
	_socks5Factory = socks5Factory;
    }

    @Reference
    public void setReactorProvider (ReactorProvider provider){
	_reactorProvider = provider;
    }

    @Reference
    public void setPlatformExecutors (PlatformExecutors execs){
	_executors = execs;
    }

    @Reference
    public void setMeteringService (MeteringService metering){
	_metering = metering;
    }
    
    @Reference
    public void setMeteringRegistry (MeteringRegistry reg){
	_meteringRegistry = reg;
    }
    
    @Reference
    public void setEventAdmin (EventAdmin admin){
	_eventAdmin = admin;
    }

    @Reference
    public void setServerFactory (ServerFactory serverF){
	_serverFactory = serverF;
    }

    @Activate
    public void activate (){
    }

    public ReactorProvider getReactorProvider (){ return _reactorProvider;}
    public PlatformExecutors getPlatformExecutors (){ return _executors;}
    public MeteringService getMeteringService (){ return _metering;}
    public EventAdmin getEventAdmin (){ return _eventAdmin;}
    public MeteringRegistry getMeteringRegistry (){ return _meteringRegistry;}
    public ServerFactory getServerFactory (){ return _serverFactory;}
    public RecorderService getRecorderService (){ return _recorderService;}
    public ChannelListenerFactory getSocks4Factory (){ return _socks4Factory;}
    public ChannelListenerFactory getSocks5Factory (){ return _socks5Factory;}
    public ChannelListenerFactory getSocksFactory (int version){
	switch (version){
	case 4: return _socks4Factory;
	case 5: return _socks5Factory;
	default: return null;
	}
    }
}
