package com.nokia.as.util.test.player;

import java.io.*;
import java.net.*;
import java.util.*;

import com.nokia.as.util.test.player.diameter.*;
import com.nokia.as.util.test.player.diameter.DiameterUtils.Avp;

public class DiameterPlayer extends TcpPlayer {
    
    private Map<String, Avp> _avps = new HashMap<String, Avp> ();
    private DiameterMessage _req, _resp;
    
    public DiameterPlayer () throws Exception {
	set ("diameter.command.cer", "257");
	set ("diameter.command.dwr", "280");
	set ("diameter.command.dpr", "282");
	set ("diameter.command.app", "1");
	avp ("diameter.avp.OriginHost 264 0 test.alu.com");
	avp ("diameter.avp.OriginRealm 296 0 alu.com");
	avp ("diameter.avp.VendorId 266 0 /0");
	avp ("diameter.avp.ProductName 269 0 tester");
	avp ("diameter.avp.ResultCode 268 0 /2001");

	avp ("diameter.avp.TestSleep 3 123 /1000");
	avp ("diameter.avp.TestIgnore 1 123 /1");
	avp ("diameter.avp.TestFillback2000 4 123 /2000");
	avp ("diameter.avp.SessionPriority0 650 10415 /0");
	avp ("diameter.avp.SessionPriority1 650 10415 /1");
	avp ("diameter.avp.SessionPriority2 650 10415 /2");
	avp ("diameter.avp.SessionPriority3 650 10415 /3");
	avp ("diameter.avp.SessionPriority4 650 10415 /4");
    }

    public Boolean open (String value) throws Exception {
	return super.open ("diameter.connection "+value);
    }
    public Boolean close (String value) throws Exception {
	return super.close ("diameter.connection");
    }
    public Boolean closed (String value) throws Exception {
	return super.closed ("diameter.connection");
    }
    
    // diameter.avp : resp-ok 10 0 /2001
    public Boolean avp (String value) throws Exception {
	String[] props = split (value, false);
	_avps.put (get (props[0]), parseAvp (concatenate (props, 1)));
	return true;
    }
    private Avp parseAvp (String value) throws Exception {
	String[] props = split (value);
	byte[] avpvalue = parseAvpValue (props[2]);
	return new Avp (asInt (props[0]),
			asInt (props[1]),
			false,
			avpvalue
			);
    }
    public byte[] parseAvpValue (String s) throws Exception {
	ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	String tmp;
	if ((tmp=appendAvpValue (s, baos)) == null) return getBytes (unquote (s));
	if (tmp.length () > 0){
	    s = tmp;
	    while ((s = appendAvpValue (s, baos)).length () > 0);
	}
	return baos.toByteArray ();
    }
    public String appendAvpValue (String s, OutputStream os) throws Exception{
	boolean isInt = s.startsWith ("/");
	boolean isByte = s.startsWith ("!");
	if (!isInt && !isByte) return null;
	int index = s.indexOf ('/', 1);
	if (index == -1) index = s.indexOf ('!', 1);
	int i = asInt (index == -1 ? s.substring(1) : s.substring (1, index));
	if (isInt) DiameterUtils.writeIntValue (i, os);
	else os.write ((byte)i);
	return index == -1 ? "" : s.substring (index);
    }
    // diameter.send-req: 10 101 avp1 avp2
    public Boolean sendreq (String value) throws Exception {
	String[] props = split (value);
	Avp[] avps = new Avp[props.length - 2];
	for (int i=2; i<props.length; i++){
	    avps[i-2] = _avps.get (props[i]);
	}
	DiameterMessage msg = DiameterUtils.makeRequest (asInt(props[0]),
							 asInt(props[1]),
							 avps
							 );
	DiameterUtils.updateHopIdentifier (msg, (int)System.currentTimeMillis () & 0xFFFF);
	return send (msg);
    }
    
    public Boolean sendresp (String value) throws Exception {
	String[] props = split (value);
	Avp[] avps = new Avp[props.length];
	for (int i=0; i<props.length; i++){
	    avps[i] = _avps.get (props[i]);
	}
	DiameterMessage msg = DiameterUtils.makeResponse (_req,
							  avps
							  );
	return send (msg);	
    }
    
    private Boolean send (DiameterMessage msg) throws Exception {
	setMessage (msg);
	return write ("diameter.connection "+getBinaryString (msg.getBytes ()));
    }

    public Boolean readreq (String value) throws Exception {
	_req = readmsg (value);
	return _req.isRequest ();
    }

    public Boolean readresp (String value) throws Exception {
	_resp = readmsg (value);
	return _resp.isRequest () == false;
    }
    
    private DiameterMessage readmsg (String value) throws Exception {
	if (value == null) value = "diameter.connection";
	String[] props = split (value);
	Socket socket = _sockets.get (props[0]);
	ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	InputStream in = socket.getInputStream ();
	int i = in.read (); baos.write (i); if (i != 1) throw new RuntimeException ("Invalid diameter version");
	int len = 0;
	i = in.read (); len = i; baos.write (i);
	i = in.read (); len <<=8; len |= i; baos.write (i);
	i = in.read (); len <<=8; len |= i; baos.write (i);
	for (i=4; i<len; i++){
	    baos.write (in.read ());
	}
	return setMessage (new DiameterMessage (baos.toByteArray ()));
    }

    private DiameterMessage setMessage (DiameterMessage msg) throws Exception {
	String type = msg.isRequest () ? ".req." : ".resp.";
	set ("diameter"+type+"hopid", msg.getHopIdentifier ());
	set ("diameter"+type+"endid", msg.getEndIdentifier ());
	set ("diameter"+type+"command", msg.getCommandCode ());
	set ("diameter"+type+"app", msg.getApplicationID ());
	byte[] result = msg.getAvp (268, 0);
	if (result == null) set ("diameter"+type+"result", -1);
	else set ("diameter"+type+"result", msg.getIntValue (result, 0, 4));
	return msg;
    }

    public Boolean checkresp (String value) throws Exception {
	for (String p : System.getProperties ().stringPropertyNames ())
	    if (p.startsWith ("diameter.resp.")){
		if (p.endsWith ("result")) continue;
		if (!get ("$"+p).equals (get ("$diameter.req."+p.substring ("diameter.resp.".length ()))))
		    return false;
	    }
	return true;
    }

    // diameter.check-resp-avp: diameter.avp.ResultCode
    // diameter.check-resp-avp: 268 0 /2001
    public Boolean checkrespavp (String value) throws Exception {
	String[] props = split (value, 3, false);
	Avp avp = null;
	if (props.length == 1){
	    avp = _avps.get (get (props[0]));
	} else if (props.length == 2){
	    avp = parseAvp (value+" -");
	} else {
	    avp = parseAvp (value);
	}
	byte[] b1 = _resp.getAvp  (avp.getCode (), avp.getVendorId ());
	if (props.length == 2) return avp != null;
	if (avp == null) return false;
	byte[] b2 = avp.getValue ();
	if (b1.length != b2.length) return false;
	for (int i= 0; i<b1.length; i++) if (b1[i] != b2[i]) return false;
	return true;
    }

    public Boolean checkrespavplen (String value) throws Exception {
	String[] props = split (value, 3);
	byte[] avp = _resp.getAvp  (asInt(props[0]), asInt(props[1]));
	return avp != null && avp.length == asInt(props[2]);
    }

    public Boolean cer (String value) throws Exception {
	if (value == null) value = "";
	return play ("diameter.send-req",
		     "0 $diameter.command.cer diameter.avp.OriginHost diameter.avp.OriginRealm diameter.avp.VendorId diameter.avp.ProductName "+value);
    }

    public Boolean dwr (String value) throws Exception {
	if (value == null) value = "";
	return play ("diameter.send-req",
		     "0 $diameter.command.dwr diameter.avp.OriginHost diameter.avp.OriginRealm "+value);
    }
    
    public Boolean dpr (String value) throws Exception {
	if (value == null) value = "";
	return play ("diameter.send-req",
		     "0 $diameter.command.dpr diameter.avp.OriginHost diameter.avp.OriginRealm "+value);
    }
    
}