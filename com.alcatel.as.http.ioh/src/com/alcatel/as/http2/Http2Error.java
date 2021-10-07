package com.alcatel.as.http2;

import com.alcatel.as.http2.frames.*;

public class Http2Error extends RuntimeException {

    public static final int CODE_NO_ERROR = 0x0;
    public static final int CODE_PROTOCOL_ERROR = 0x1;
    public static final int CODE_INTERNAL_ERROR = 0x2;
    public static final int CODE_FLOW_CONTROL_ERROR = 0x3;
    public static final int CODE_SETTINGS_TIMEOUT = 0x4;
    public static final int CODE_STREAM_CLOSED = 0x5;
    public static final int CODE_FRAME_SIZE_ERROR = 0x6;
    public static final int CODE_REFUSED_STREAM = 0x7;
    public static final int CODE_CANCEL = 0x8;
    public static final int CODE_COMPRESSION_ERROR = 0x9;
    public static final int CODE_CONNECT_ERROR = 0xa;
    public static final int CODE_ENHANCE_YOUR_CALM = 0xb;
    public static final int CODE_INADEQUATE_SECURITY = 0xc;
    public static final int CODE_HTTP_1_1_REQUIRED = 0xd;
    
    public enum Code {
	NO_ERROR (CODE_NO_ERROR),
	PROTOCOL_ERROR (CODE_PROTOCOL_ERROR),
	INTERNAL_ERROR (CODE_INTERNAL_ERROR),
	FLOW_CONTROL_ERROR (CODE_FLOW_CONTROL_ERROR),
	SETTINGS_TIMEOUT (CODE_SETTINGS_TIMEOUT),
	STREAM_CLOSED (CODE_STREAM_CLOSED),
	FRAME_SIZE_ERROR (CODE_FRAME_SIZE_ERROR),
	REFUSED_STREAM (CODE_REFUSED_STREAM),
	CANCEL (CODE_CANCEL),
	COMPRESSION_ERROR (CODE_COMPRESSION_ERROR),
	CONNECT_ERROR (CODE_CONNECT_ERROR),
	ENHANCE_YOUR_CALM (CODE_ENHANCE_YOUR_CALM),
	INADEQUATE_SECURITY (CODE_INADEQUATE_SECURITY),
	HTTP_1_1_REQUIRED (CODE_HTTP_1_1_REQUIRED);

	private long _code;
	private Code (int code){
	    _code = code & 0x00_00_00_00_FF_FF_FF_FFL;
	}
	public long value (){ return _code;}
    }

    protected Code _code;
    protected String _message;
    protected Frame _frame;

    public Http2Error (Code code){
	this (code, null, null);
    }
    public Http2Error (Code code, String message){
	this (code, message, null);
    }
    public Http2Error (Code code, Frame frame){
	this (code, null, frame);
    }
    public Http2Error (Code code, String message, Frame frame){
	super (message);
	_code = code;
	_frame = frame;
    }
    public Code code (){ return _code;}
    public Frame frame (){ return _frame;}
    
    public static Code code (int code){
	switch (code){
	case CODE_NO_ERROR: return Code.NO_ERROR;
	case CODE_PROTOCOL_ERROR: return Code.PROTOCOL_ERROR;
	case CODE_INTERNAL_ERROR: return Code.INTERNAL_ERROR;
	case CODE_FLOW_CONTROL_ERROR: return Code.FLOW_CONTROL_ERROR;
	case CODE_SETTINGS_TIMEOUT: return Code.SETTINGS_TIMEOUT;
	case CODE_STREAM_CLOSED: return Code.STREAM_CLOSED;
	case CODE_FRAME_SIZE_ERROR: return Code.FRAME_SIZE_ERROR;
	case CODE_REFUSED_STREAM: return Code.REFUSED_STREAM;
	case CODE_CANCEL: return Code.CANCEL;
	case CODE_COMPRESSION_ERROR: return Code.COMPRESSION_ERROR;
	case CODE_CONNECT_ERROR: return Code.CONNECT_ERROR;
	case CODE_ENHANCE_YOUR_CALM: return Code.ENHANCE_YOUR_CALM;
	case CODE_INADEQUATE_SECURITY: return Code.INADEQUATE_SECURITY;
	case CODE_HTTP_1_1_REQUIRED: return Code.HTTP_1_1_REQUIRED;
	}
	return Code.NO_ERROR;
    }
}
