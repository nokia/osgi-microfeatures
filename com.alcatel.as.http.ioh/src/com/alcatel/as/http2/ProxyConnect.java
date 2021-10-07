package com.alcatel.as.http2;

import com.alcatel.as.http.parser.*;
import java.nio.ByteBuffer;
import java.net.*;
import alcatel.tess.hometop.gateways.reactor.*;

public class ProxyConnect {

    private static final byte[] CONNECT_B = HttpParser.getUTF8 ("CONNECT ");
    private static final byte[] HTTP11_B = HttpParser.getUTF8 (" HTTP/1.1\r\n\r\n");
    
    private HttpParser _parser = new HttpParser ();

    public ProxyConnect (){}

    public ProxyConnect connect (TcpChannel cnx, InetSocketAddress dest){
	StringBuilder sb = new StringBuilder ();
	sb.append (dest.getAddress ().getHostAddress ())
	    .append (':')
	    .append (String.valueOf (dest.getPort ()));
	cnx.send (new ByteBuffer[]{
		ByteBuffer.wrap (CONNECT_B),
		ByteBuffer.wrap (HttpParser.getUTF8 (sb.toString ())),
		ByteBuffer.wrap (HTTP11_B)}, false);
	return this;
    }

    public void received (ByteBuffer data, Runnable onOK, Runnable onKO){
	HttpMessage msg = _parser.parseMessage (data);
	if (msg == null ||
	    !msg.isLast ()) return; // not full received yet
	if (msg.getStatus () == 200)
	    onOK.run ();
	else
	    onKO.run ();
    }
    
}
