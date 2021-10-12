// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.tools;

import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.*;
import alcatel.tess.hometop.gateways.reactor.*;

public class ChannelWriter {

    private AsyncChannel _channel;
    private SendBufferMonitor _buffMonitor;
    
    public ChannelWriter (AsyncChannel channel, SendBufferMonitor buffMonitor){
	_channel = channel;
	_buffMonitor = buffMonitor;
    }
    
    public AsyncChannel getChannel (){ return _channel;}

    // can be overriden for testing !
    public int getSendBufferSize (AsyncChannel channel) { return channel.getSendBufferSize ();}

    public boolean check (Object argument){
	return check (getSendBufferSize (_channel), _buffMonitor, argument);
    }
    public static boolean check (AsyncChannel channel, SendBufferMonitor buffMonitor, Object argument){
	return check (channel.getSendBufferSize (), buffMonitor, argument);
    }
    public static boolean check (int buffSize, SendBufferMonitor buffMonitor, Object argument){
	return buffMonitor.check (buffSize, argument);
    }

    public boolean send(byte[] msg, int off, int len, boolean copy, Object argument){
	return send (getSendBufferSize (_channel), _channel, _buffMonitor, msg, off, len, copy, argument);
    }
    public boolean send(ByteBuffer msg, boolean copy, Object argument){
	return send (getSendBufferSize (_channel), _channel, _buffMonitor, msg, copy, argument);
    }
    public boolean send(ByteBuffer[] msg, boolean copy, Object argument){
	return send (getSendBufferSize (_channel), _channel, _buffMonitor, msg, copy, argument);
    }

    public static boolean send(AsyncChannel channel, SendBufferMonitor buffMonitor, byte[] msg, int off, int len, boolean copy, Object argument){
	return send (channel.getSendBufferSize (), channel, buffMonitor, msg, off, len, copy, argument);
    }
    public static boolean send(AsyncChannel channel, SendBufferMonitor buffMonitor, ByteBuffer msg, boolean copy, Object argument){
	return send (channel.getSendBufferSize (), channel, buffMonitor, msg, copy, argument);
    }
    public static boolean send(AsyncChannel channel, SendBufferMonitor buffMonitor, ByteBuffer[] msg, boolean copy, Object argument){
	return send (channel.getSendBufferSize (), channel, buffMonitor, msg, copy, argument);
    }
    
    public static boolean send(int buffSize, AsyncChannel channel, SendBufferMonitor buffMonitor, byte[] msg, int off, int len, boolean copy, Object argument){
	if (check (buffSize, buffMonitor, argument)){
	    channel.send (msg, off, len, copy);
	    return true;
	} else
	    return false;
    }
    public static boolean send(int buffSize, AsyncChannel channel, SendBufferMonitor buffMonitor, ByteBuffer msg, boolean copy, Object argument){
	if (check (buffSize, buffMonitor, argument)){
	    channel.send (msg, copy);
	    return true;
	} else
	    return false;
    }
    public static boolean send(int buffSize, AsyncChannel channel, SendBufferMonitor buffMonitor, ByteBuffer[] msg, boolean copy, Object argument){
	if (check (buffSize, buffMonitor, argument)){
	    channel.send (msg, copy);
	    return true;
	} else
	    return false;
    }

    public static interface SendBufferMonitor {

	 boolean check (int buffSize, Object argument);
    }

    public static class BinarySendBufferMonitor implements SendBufferMonitor {
	public static final BinarySendBufferMonitor PASSING = new BinarySendBufferMonitor (true);
	public static final BinarySendBufferMonitor BLOCKING = new BinarySendBufferMonitor (false);
	private boolean _mode;
	private BinarySendBufferMonitor (boolean mode){ _mode = mode;}
	public boolean check (int buffSize, Object argument){ return _mode;}
    }

    public static class BoundedSendBufferMonitor implements SendBufferMonitor {
	private int _size;
	public BoundedSendBufferMonitor (int size){
	    if (size <= 0) size = Integer.MAX_VALUE; // set to 0 (or -1) to disable it.
	    _size = size;
	}
	public boolean check (int buffSize, Object argument){
	    return buffSize <= _size;
	}
    }

    public static class ProgressiveSendBufferMonitor implements SendBufferMonitor {
	protected int _lowWM, _highWM, _diffWM;
	public ProgressiveSendBufferMonitor (int lowWM, int highWM){
	    if (highWM <= 0) lowWM = Integer.MAX_VALUE; // set highWM to 0 (or -1) to disable it.
	    _lowWM = lowWM;
	    _highWM = highWM;
	    _diffWM = _highWM - _lowWM;
	}
	public boolean check (int buffSize, Object argument){
	    if (buffSize > _lowWM){
		if (buffSize > _highWM){
		    return false;
		}
		int overload = (100*(buffSize - _lowWM))/_diffWM;
		if (ThreadLocalRandom.current().nextInt (100) < overload){
		    return false;
		}
	    }
	    return true;
	}
    }

    public static class ProgressiveSendBufferMonitorWithPriorities implements SendBufferMonitor {
	private ProgressiveSendBufferMonitor[] _monitors;
	protected int _lowWM, _highWM;
	public ProgressiveSendBufferMonitorWithPriorities (int lowWM, int highWM, int nbPriorities){
	    if (highWM <= 0){ // set highWM to 0 (or -1) to disable it.
		_lowWM = Integer.MAX_VALUE;
		return;
	    }
	    _lowWM = lowWM;
	    _highWM = highWM;
	    _monitors = new ProgressiveSendBufferMonitor[nbPriorities];
	    int step = (highWM - lowWM) / nbPriorities;
	    for (int i=0; i<nbPriorities; i++){
		_monitors[i] = new ProgressiveSendBufferMonitor (lowWM, lowWM + step);
		lowWM += step;
	    }
	}
	public boolean check (int buffSize, Object argument){
	    if (buffSize > _lowWM){
		if (buffSize > _highWM){
		    return false;
		}
		return _monitors[((Integer) argument).intValue ()].check (buffSize, null);
	    }
	    return true;
	}
	public ProgressiveSendBufferMonitor getSendBufferMonitor (int priority){
	    return _monitors[priority];
	}
    }

}
