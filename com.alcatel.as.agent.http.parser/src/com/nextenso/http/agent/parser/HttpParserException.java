// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.parser;

import alcatel.tess.hometop.gateways.utils.NestedException;

@SuppressWarnings("serial")
public class HttpParserException extends NestedException {
    
    public HttpParserException (String msg){
	super (msg);
    }

    public HttpParserException (Throwable t){
	super (t);
    }

    public HttpParserException (String msg, Throwable t){
	super (msg, t);
    }
    
}
