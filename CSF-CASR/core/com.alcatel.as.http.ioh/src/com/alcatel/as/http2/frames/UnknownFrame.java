package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class UnknownFrame extends Frame {

    protected UnknownFrame (int type){
	super (type);
    }

    public boolean isControlFrame (){ return true;}
    
    @Override
    public void received (Stream stream){}
    
}
