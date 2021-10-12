// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;

import com.alcatel.as.http2.*;
import com.alcatel.as.http2.frames.*;

public class StreamError extends Http2Error {
    
    public StreamError (Code code, Frame frame){
	super (code, frame);
    }
    public StreamError (Code code, String message, Frame frame){
	super (code, message, frame);
    }
    
    public String toString (){
	return new StringBuilder ()
	    .append ("StreamError[")
	    .append ("Code=").append (_code)
	    .append (",Msg=").append (getMessage ())
	    .append (",Frame=").append (_frame)
	    .append (']')
	    .toString ();
    }
    
}
