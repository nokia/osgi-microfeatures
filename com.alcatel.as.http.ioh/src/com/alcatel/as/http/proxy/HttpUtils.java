// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.proxy;

import java.nio.ByteBuffer;

import com.alcatel.as.http.parser.*;

public class HttpUtils {

    
    private static final byte[] CONNECT_ESTABLISHED_V0 = HttpParser.getUTF8 ("HTTP/1.0 200 Connection established\r\n\r\n");
    private static final byte[] CONNECT_ESTABLISHED_V1 = HttpParser.getUTF8 ("HTTP/1.1 200 Connection established\r\n\r\n");
    private static final byte[] CONNECT_FAILED_V0 = HttpParser.getUTF8 ("HTTP/1.0 502 Tunnel Connection Failed\r\nConnection: close\r\n\r\n");
    private static final byte[] CONNECT_FAILED_V1 = HttpParser.getUTF8 ("HTTP/1.1 502 Tunnel Connection Failed\r\nConnection: close\r\n\r\n");
    private static final byte[] UNKNOWN_HOST_V0 = HttpParser.getUTF8 ("HTTP/1.0 504 Unknown Host\r\nConnection: close\r\n\r\n");
    private static final byte[] UNKNOWN_HOST_V1 = HttpParser.getUTF8 ("HTTP/1.1 504 Unknown Host\r\nConnection: close\r\n\r\n");
    private static final byte[] HOST_UNREACHABLE_V0 = HttpParser.getUTF8 ("HTTP/1.0 504 Host Unreachable\r\nConnection: close\r\n\r\n");
    private static final byte[] HOST_UNREACHABLE_V1 = HttpParser.getUTF8 ("HTTP/1.1 504 Host Unreachable\r\nConnection: close\r\n\r\n");

    public static ByteBuffer getConnectionEstablishedResponse (HttpMessage req, boolean ok){
	if (req.getVersion () == 0)
	    return ok ? ByteBuffer.wrap (CONNECT_ESTABLISHED_V0) : ByteBuffer.wrap (CONNECT_FAILED_V0);
	else
	    return ok ? ByteBuffer.wrap (CONNECT_ESTABLISHED_V1) : ByteBuffer.wrap (CONNECT_FAILED_V1);
    }
    public static ByteBuffer getUnknownHostResponse (HttpMessage req){
	return req.getVersion () == 0 ?
	    ByteBuffer.wrap (UNKNOWN_HOST_V0) : ByteBuffer.wrap (UNKNOWN_HOST_V1);
    }
    public static ByteBuffer getHostUnreachableResponse (HttpMessage req){
	return req.getVersion () == 0 ?
	    ByteBuffer.wrap (HOST_UNREACHABLE_V0) : ByteBuffer.wrap (HOST_UNREACHABLE_V1);
    }

}
