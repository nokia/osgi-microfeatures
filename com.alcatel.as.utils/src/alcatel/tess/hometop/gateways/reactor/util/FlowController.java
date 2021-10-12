// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.util;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import com.alcatel.as.service.concurrent.*;

/**
 * This is a helper class to make flow control on a channel
 */
public class FlowController {
    
    protected int _flowControl, _flowControlLow, _flowControlHigh;
    protected boolean _readingDisabled;
    protected PlatformExecutor _flowControlExec;
    protected AsyncChannel _channel;
    protected Runnable _releaseFlowControlRunnable = new Runnable (){
	    public void run (){
		releaseNow ();
	    }};
    protected Runnable _acquireFlowControlRunnable = new Runnable (){
	    public void run (){
		acquireNow ();
	    }};
    protected Runnable _releaseFullBufferFlowControlRunnable = new Runnable (){
	    public void run (){
		releaseNow (_acquireBufferMaxSize);
	    }};

    public FlowController (AsyncChannel channel, int lowWM, int highWM, PlatformExecutor exec){
	_channel = channel;
	_flowControlLow = lowWM;
	_flowControlHigh = highWM;
	_flowControlExec = exec;
	_acquireBufferMaxSize = (_flowControlLow*8) / 10; // 80% of lowWM
	if (_acquireBufferMaxSize == 0) _acquireBufferMaxSize = 1;
    }
    // called in exec - assume socket is not closed
    protected int _acquireBuffer = 0;
    protected int _acquireBufferMaxSize = 1;
    public int acquireNowWithBuffer (){ // only works if all subsequent schedules are done in the same Q --> releases are successive
	if (_flowControl >= _flowControlLow){
	    acquireNow ();
	    if (_acquireBuffer == 0) return 1;
	    try{return 1 + _acquireBuffer;}finally{_acquireBuffer = 0;}
	}
	_flowControl++;
	_acquireBuffer++;
	if (_acquireBuffer == _acquireBufferMaxSize){
	    try {return _acquireBuffer;}finally{_acquireBuffer = 0;}
	}
	return 0;
    }
    // called in exec - assume socket is not closed
    public boolean acquireNow (){
	boolean crossHigh = (++_flowControl == _flowControlHigh);
	if (crossHigh && !_readingDisabled){
	    _channel.disableReading ();
	    _readingDisabled = true;
	    return true;
	}
	return false;
    }
    // called in exec - assume socket is not closed
    public boolean acquireNow (int acquire){
	boolean crossHigh = _flowControl < _flowControlHigh; // == wasBelow
	_flowControl += acquire;
	crossHigh = (crossHigh && _flowControl >= _flowControlHigh); // wasBelow && isAbove
	if (crossHigh && !_readingDisabled){
	    _channel.disableReading ();
	    _readingDisabled = true;
	    return true;
	}
	return false;
    }
    public boolean releaseNow (){
	if (--_flowControl == _flowControlLow && _readingDisabled){
	    _channel.enableReading ();
	    _readingDisabled = false;
	    return true;
	}
	return false;
    }
    public boolean releaseNow (int release){
	boolean crossLow = _flowControl > _flowControlLow; // == wasAbove
	_flowControl -= release;
	crossLow = (crossLow && _flowControl <= _flowControlLow); // wasBelow && isAbove
	if (crossLow && _readingDisabled){
	    _channel.enableReading ();
	    _readingDisabled = false;
	    return true;
	}
	return false;
    }
    // called from anywhere
    public void acquire (){
	_flowControlExec.execute (_acquireFlowControlRunnable, ExecutorPolicy.INLINE);
    }
    // called from anywhere
    public void release (){
	_flowControlExec.execute (_releaseFlowControlRunnable, ExecutorPolicy.INLINE);
    }
    // called from anywhere
    public void release (final int release){
	if (release == 0) return;
	if (release == 1){
	    release ();
	    return;
	}
	if (release == _acquireBufferMaxSize){
	    _flowControlExec.execute (_releaseFullBufferFlowControlRunnable, ExecutorPolicy.INLINE);
	    return;
	}
	_flowControlExec.execute (new Runnable (){public void run (){releaseNow (release);}}, ExecutorPolicy.INLINE);
    }
    // called from anywhere
    public void acquireAndRelease (final int acquire){
	if (acquire == 0){
	    release ();
	    return;
	}
	if (acquire == 1) return;
	Runnable r = new Runnable (){
		public void run (){
		    acquireNow (acquire - 1);
		}};
	_flowControlExec.execute (r, ExecutorPolicy.INLINE);
    }
}
