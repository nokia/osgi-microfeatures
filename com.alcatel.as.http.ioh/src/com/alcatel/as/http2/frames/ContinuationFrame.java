// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class ContinuationFrame extends Frame {

    public static final int TYPE = 0x09;

    private boolean _endStream = false;
    
    protected ContinuationFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return "ContinuationFrame["+_streamId+"/endHeaders="+endHeadersFlag ()+"]";}
    public boolean isControlFrame (){ return false;}
    public boolean endHeadersFlag (){ return (_flags & 0x04) == 0x04;}

    // the following is used when sending headers to piggyback the _endStream flag
    public ContinuationFrame endStream (boolean endStream) { _endStream = endStream; return this;}
    public boolean endStream (){ return _endStream;}
    @Override
    public boolean endsStream (){ return endHeadersFlag () && _endStream;}
    
    @Override
    public void received (Stream stream) {
	stream.event (StreamStateMachine.Event.RecvC, this);
    }
    @Override
    public void sent (Stream stream) {
	stream.event (StreamStateMachine.Event.SendC, this);
    }

    public static int flags (boolean endHeaders){
	return endHeaders ? 0x04 : 0x00;
    }
}
