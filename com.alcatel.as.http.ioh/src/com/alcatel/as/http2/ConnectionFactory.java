package com.alcatel.as.http2;

import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;
import alcatel.tess.hometop.gateways.reactor.*;


@Component(service=ConnectionFactory.class)
public class ConnectionFactory {

    protected static ConnectionFactory INSTANCE;
    protected TimerService _timerS;

    @Reference(target="(strict=false)")
    public void setTimerService (TimerService ts){
	_timerS = ts;
    }
    @Activate
    public void activate (){
	INSTANCE = this;
    }

    public Connection newServerConnection (ConnectionConfig conf, TcpChannel channel, Http2RequestListener reqListener){
	return new Connection (true, conf, channel, reqListener);
    }

    public Connection newClientConnection (ConnectionConfig conf, TcpChannel channel){
	return new Connection (false, conf, channel, null);
    }
    
}
