package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class SettingsFrame extends Frame {

    public static final int TYPE = 0x04;
    
    private Settings _settings = new Settings ();
    
    public boolean ack (){ return (_flags & 0x01) == 0x01;}

    public SettingsFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return ack () ? "SettingsAckFrame" : "SettingsFrame["+_settings+"]";}
    public boolean isControlFrame (){ return true;}
    public Settings settings (){ return _settings;}
    
    @Override
    public Frame parse () throws ConnectionError, StreamError {
	int remaining = _payload.remaining ();
	if (remaining == 0) return this;
	if (ack ())
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
				       "Invalid SettingsFrame : ACK with non-empty payload");
	if (remaining % 6 != 0)
	    throw new ConnectionError (Http2Error.Code.FRAME_SIZE_ERROR,
				       "Invalid Settings Frame : payload not multiple of 6");
	int nb = remaining / 6;
	for (int i=0; i<nb; i++){
	    int id = _payload.get () & 0xFF;
	    id <<= 8;
	    id |= _payload.get () & 0xFF;
	    long value = _payload.getInt () & 0xFFFFFFFFL;
	    switch (id){
	    case 0x01 : _settings.HEADER_TABLE_SIZE = value; break;
	    case 0x02 : _settings.ENABLE_PUSH = value; break;
	    case 0x03 : _settings.MAX_CONCURRENT_STREAMS = value; break;
	    case 0x04 : _settings.INITIAL_WINDOW_SIZE = value; break;
	    case 0x05 : _settings.MAX_FRAME_SIZE = value; break;
	    case 0x06 : _settings.MAX_HEADER_LIST_SIZE = value; break;
	    default : break; //ignore per rfc
	    }
	}
	return check ();
    }

    @Override
    protected Frame check () throws ConnectionError, StreamError {
	super.check ();
	_settings.check ();
	return this;
    }

    public SettingsFrame set (Settings settings){
	_settings = settings;
	_payload = ByteBuffer.allocate (36);
	writeSetting (0x01, settings.HEADER_TABLE_SIZE);
	writeSetting (0x02, settings.ENABLE_PUSH);
	writeSetting (0x03, settings.MAX_CONCURRENT_STREAMS);
	writeSetting (0x04, settings.INITIAL_WINDOW_SIZE);
	writeSetting (0x05, settings.MAX_FRAME_SIZE);
	writeSetting (0x06, settings.MAX_HEADER_LIST_SIZE);
	_payload.flip ();
	_isCopy = true;
	return this;
    }
    private void writeSetting (int code, long value){
	_payload.put ((byte) (code >> 8));
	_payload.put ((byte) code);
	_payload.putInt ((int) value);
    }

    public static SettingsFrame makeAck (){
	SettingsFrame sf = new SettingsFrame ();
	sf.set (0x01, 0x00);
	sf.payload (ByteBuffer.allocate (0), true); // check if it can be optimized
	return sf;
    }

}
