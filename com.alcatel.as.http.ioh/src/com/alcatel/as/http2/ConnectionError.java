// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;

import com.alcatel.as.http2.frames.*;

public class ConnectionError extends Http2Error {

    public ConnectionError (Code code){
	super (code);
    }
    public ConnectionError (Code code, String message){
	super (code, message);
    }
    public ConnectionError (Code code, Frame frame){
	super (code, frame);
    }
    public ConnectionError (Code code, String message, Frame frame){
	super (code, message, frame);
    }
    
    public String toString (){
	return new StringBuilder ()
	    .append ("ConnectionError[")
	    .append ("Code=").append (_code)
	    .append (",Msg=").append (getMessage ())
	    .append (",Frame=").append (_frame)
	    .append (']')
	    .toString ();
    }
    
}
