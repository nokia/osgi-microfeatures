// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class PingFrame extends Frame {

    public static final int TYPE = 0x06;

    protected PingFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return ack () ? "PingAckFrame" : "PingFrame";}
    public boolean isControlFrame (){ return true;}
    public boolean ack (){ return (_flags & 0x01) == 0x01;}

    @Override
    public Frame parse () throws ConnectionError, StreamError {
	if (_payload.remaining () != 8)
	    throw new ConnectionError (Http2Error.Code.FRAME_SIZE_ERROR,
				       "Invalid Ping Frame data length : "+_payload.remaining ());
	return check ();
    }

    public PingFrame makeAck (){
	PingFrame pong = new PingFrame ();
	pong.set (0x01, 0x00);
	pong.payload (_payload, _isCopy);
	return pong;
    }

    public PingFrame set (){
	set (0, 0);
	payload (ByteBuffer.wrap (new byte[8]), true);
	return this;
    }
    
}
