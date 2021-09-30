package com.alcatel.as.ioh.socks;

import java.util.function.*;
import java.net.*;
import java.nio.*;
import alcatel.tess.hometop.gateways.reactor.*;

public class SocksParser {

    private TcpClientContext _ctx;
    private boolean _allow4, _allow5;
    private boolean _v4 = true;
    private byte[] _ip;
    private int _port;
    private int _count;
    private StringBuilder _userId = new StringBuilder ();
    
    public SocksParser (TcpClientContext ctx, boolean v4, boolean v5){
	_ctx  = ctx;
	_allow4 = v4;
	_allow5 = v5;
    }

    public InetSocketAddress messageReceived (ByteBuffer data){
	return _v4 ? parseV4 (data) : parseV5 (data);
    }

    public void destConnected (TcpChannel client){
	if (_v4){
	    ByteBuffer bb = ByteBuffer.wrap (new byte[]{(byte)0, (byte)0x5A,
							(byte)0, (byte)0,
							(byte)0, (byte)0, (byte)0, (byte)0});
	    client.send (bb, false);
	} else {
	    ByteBuffer bb = ByteBuffer.wrap (new byte[]{(byte)0x5,
							(byte)0,
							(byte)0,
							(byte)1, (byte)0, (byte)0, (byte)0, (byte)0,
							(byte)0, (byte)0
							
		});
	    client.send (bb, false);
	}
    }

    public void destFailed (TcpChannel client){
	if (_v4){
	    ByteBuffer bb = ByteBuffer.wrap (new byte[]{(byte)0, (byte)0x5B,
							(byte)0, (byte)0,
							(byte)0, (byte)0, (byte)0, (byte)0});
	    client.send (bb, false);
	} else {
	    ByteBuffer bb = ByteBuffer.wrap (new byte[]{(byte)0x5,
							(byte)1,
							(byte)0,
							(byte)1, (byte)0, (byte)0, (byte)0, (byte)0,
							(byte)0, (byte)0
							
		});
	    client.send (bb, false);
	}
    }

    public InetSocketAddress parseV4 (ByteBuffer buffer){	    
	int remaining = buffer.remaining ();
	if (remaining == 0) return null;
	switch (_count){
	case 0:
	    byte version = buffer.get ();
	    if (version == (byte)4 && _allow4){
		_v4 = true;
		if (_ctx.logger ().isDebugEnabled ())
		    _ctx.logger ().debug (_ctx+" : socks version : 4");
	    } else if (version == (byte)5 && _allow5){
		_v4 = false;
		if (_ctx.logger ().isDebugEnabled ())
		    _ctx.logger ().debug (_ctx+" : socks version : 5");
		return parseV5 (buffer);
	    } else {
		throw new RuntimeException ("Invalid SOCKS version : "+version);
	    }
	    _count++;
	    if (--remaining == 0) return null;
	case 1:
	    byte cmd = buffer.get ();
	    if (cmd != (byte)1) throw new RuntimeException ("Invalid command : "+cmd);
	    _count++;
	    if (--remaining == 0) return null;
	case 2:
	    _port = buffer.get () & 0xFF;
	    _count++;
	    if (--remaining == 0) return null;
	case 3:
	    _port = _port << 8;
	    _port |= buffer.get () & 0xFF;
	    _count++;
	    if (--remaining == 0) return null;
	case 4:
	    _ip = new byte[4];
	    _ip[0] = buffer.get ();
	    _count++;
	    if (--remaining == 0) return null;
	case 5:
	    _ip[1] = buffer.get ();
	    _count++;
	    if (--remaining == 0) return null;
	case 6:
	    _ip[2] = buffer.get ();
	    _count++;
	    if (--remaining == 0) return null;
	case 7:
	    _ip[3] = buffer.get ();
	    _count++;
	    if (--remaining == 0) return null;
	default:
	forloop: for (int i=0; i<remaining; i++){
		if (_count++ == (SocksProxy.PROP_V4_USER_ID_MAX+9)) throw new RuntimeException ("User Id too long");
		byte b = buffer.get ();
		if (b == (byte)0){
		    if (!_ctx.checkUserId (_userId.toString ()))
			throw new RuntimeException ("Invalid userId : "+_userId.toString ());
		    try {
			return new InetSocketAddress (InetAddress.getByAddress (_ip), _port);
		    }catch(Exception e){
			throw new RuntimeException ("Invalid address", e);
		    }
		}
		_userId.append ((char)(b & 0xFF));
	    }
	}
	return null;
    }

    /******************** V5 ****************/

    public InetSocketAddress parseV5 (ByteBuffer buffer){
	return _state.parse (buffer, buffer.remaining ());
    }

    public static interface STATE {
	public InetSocketAddress parse (ByteBuffer data, int remaining);
    }
    public STATE STATE_CLIENT_GREETING = new STATE (){
	    private int _nauth = -1;
	    private boolean _noauth;
	    public InetSocketAddress parse (ByteBuffer data, int remaining){
		if (remaining == 0) return null;
		if (_nauth == -1){
		    _nauth = data.get () & 0xFF;
		    if (_nauth <= 0 || _nauth >= 10) throw new RuntimeException ("Invalid nauth : "+_nauth);
		    if (_ctx.logger ().isDebugEnabled ())
			_ctx.logger ().debug (_ctx+" : nauth : "+_nauth);
		    if (--remaining == 0) return null;
		}
		int min = Integer.min (remaining, _nauth);
		for (int i=0; i<min; i++){
		    if (data.get () == (byte)0) _noauth = true;
		    _nauth--;
		}
		if (_nauth == 0){
		    if (_ctx.logger ().isDebugEnabled ())
			_ctx.logger ().debug (_ctx+" : noauth : "+_noauth);
		    if (_noauth){ // we only support no auth
			ByteBuffer bb = ByteBuffer.wrap (new byte[]{(byte)5, (byte)0});
			_ctx._clientChannel.send (bb, false);
			_state = STATE_CLIENT_CONN_REQUEST;
			return _state.parse (data, data.remaining ());
		    } else {
			ByteBuffer bb = ByteBuffer.wrap (new byte[]{(byte)5, (byte)0xFF});
			_ctx._clientChannel.send (bb, false);
			_ctx._clientChannel.close ();
		    }
		}
		return null;
	    }
	};
    public STATE STATE_CLIENT_CONN_REQUEST = new STATE (){
	    private int _type = -1;
	    private int _index = 0;
	    private int _iplen, _ippos;
	    public InetSocketAddress parse (ByteBuffer buffer, int remaining){
		if (remaining == 0) return null;
		switch (_count){
		case 0:
		    byte version = buffer.get ();
		    if (version != (byte)5){
			throw new RuntimeException ("Invalid SOCKS version : "+version);
		    }
		    _count++;
		    if (--remaining == 0) return null;
		case 1:
		    byte cmd = buffer.get ();
		    if (cmd != (byte)1) throw new RuntimeException ("Invalid command : "+cmd);
		    _count++;
		    if (--remaining == 0) return null;
		case 2:
		    if (buffer.get () != (byte)0) throw new RuntimeException ("Invalid RSV");
		    _count++;
		    if (--remaining == 0) return null;
		    break;
		case 10:
		    _port = buffer.get () & 0xFF;
		    _count++;
		    if (--remaining == 0) return null;
		case 11:
		    _port = _port << 8;
		    _port |= buffer.get () & 0xFF;
		    try {
			return new InetSocketAddress (InetAddress.getByAddress (_ip), _port);
		    }catch(Exception e){
			throw new RuntimeException ("Invalid address", e);
		    }
		}
		if (_type == -1){
		    _type = buffer.get () & 0xFF;
		    if (_type != 1 && _type != 4) throw new RuntimeException ("Invalid address type : "+_type);
		    if (_type == 1) _iplen = 4;
		    else if (_type == 4) _iplen = 16;
		    _ip = new byte[_iplen];
		    if (--remaining == 0) return null;
		}
		if (_iplen > 0){
		    int min = Math.min (remaining, _iplen);
		    for (int i=0; i<min; i++){
			_iplen--;
			_ip[_ippos++] = buffer.get ();
		    }
		    if (_iplen > 0) return null;
		}
		_count = 10;
		return this.parse (buffer, buffer.remaining ());
	    }
	};
    
    private STATE _state = STATE_CLIENT_GREETING;
}
