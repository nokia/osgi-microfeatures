// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.test.player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class TcpPlayer extends TestPlayer {

    protected static Map<String, Socket> _sockets = new HashMap<String, Socket> ();
    protected static Map<String, ServerSocket> _servers = new HashMap<String, ServerSocket> ();
    protected static Map<String, List<String>> _acceptSockets = new HashMap<String, List<String>> ();
    
    public TcpPlayer (){
    }

    public void close () throws Exception {
	super.close ();
	for (Socket socket : _sockets.values ())
	    socket.close ();
	for (ServerSocket socket : _servers.values ())
	    socket.close ();
	Thread.sleep (200); // let the sockets close
    }

    public Boolean open (String value) throws Exception {
	String[] toks = split (value);
	try{
	    Socket socket = new Socket (toks[1], asInt (toks[2]));
	    socket.setTcpNoDelay (true);
	    _sockets.put (toks[0], socket);
	    return true;
	} catch (Exception e){
	    return false;
	}
    }

    public Boolean alias (String value) throws Exception {
	String[] toks = split (value);
	_sockets.put (toks[1], _sockets.get (toks[0]));
	return true;
    }
    public Boolean rename (String value) throws Exception {
	String[] toks = split (value);
	_sockets.put (toks[1], _sockets.remove (toks[0]));
	return true;
    }

    public Boolean listen (String value) throws Exception {
	String[] props = split (value);
	final String name = props[0];
	final ServerSocket ss = new ServerSocket (asInt (props[1]));
	_servers.put (props[0], ss);
	Runnable r = new Runnable (){
		public void run (){
		    try{
			while (true){
			    final Socket socket = ss.accept ();
			    List<String> list = _acceptSockets.get (name);
			    String sname = list.remove(0);
			    _sockets.put (sname, socket);
			    if (sname.startsWith ("X")){
				Runnable r = new Runnable (){
					public void run (){
					    try{
						while (socket.getInputStream ().read () != -1){}
					    }catch(Exception e){
					    }
					}
				    };
				new Thread (r).start ();
			    }
			}
		    }catch(Exception e){
			if (ss.isClosed ()) return; // close() was called
			e.printStackTrace ();
		    }
		}
	    };
	new Thread (r).start ();
	return true;
    }

    public Boolean accept (String value) throws Exception {
	String[] props = split (value);
	List<String> list = _acceptSockets.get (props[0]);
	if (list == null){
	    list = new ArrayList<String> ();
	    _acceptSockets.put (props[0], list);
	}
	list.add (props[1]);
	return true;
    }
    
    public Boolean close (String value) throws Exception {
	Socket socket = _sockets.remove (get (value));
	socket.close ();
	return true;
    }

    public Boolean closed (String value) throws Exception {
	Socket socket = _sockets.remove (get (value));
	try {
	    return socket.getInputStream ().read () == -1; 
	} catch (IOException e) {
	    return e.getMessage().toLowerCase().indexOf("reset") > -1;
	}
    }

    public Boolean settimeout (String value) throws Exception {
	String[] props = split (value);
	Socket clientSocket = _sockets.get (props[0]);
	clientSocket.setSoTimeout (asInt (props[1]));
	return true;
    }

    public Boolean sendbin (String value) throws Exception {
	String[] props = split (value);
	Socket clientSocket = _sockets.get (props[0]);
	for (int i=1; i<props.length; i++){
	    String prop = props[i];
	    int p = prop.indexOf ('x');
	    int radix = 10;
	    if (p > -1){
		radix = Integer.parseInt (prop.substring (0, p));
		prop = prop.substring (p+1);
	    }
	    int v = Integer.parseInt (prop, radix);
	    clientSocket.getOutputStream ().write (v);
	}
	clientSocket.getOutputStream ().flush ();
	return true;
    }

    public Boolean write (String value) throws Exception {
	return write (split (value, 2, false), false, false);
    }
    public Boolean writeline (String value) throws Exception {
	String[] toks = split (value);
	if (toks.length == 1) return write (toks, false, true);
	else return write (split (value, 2, false), false, true);
    }
    public Boolean slowwrite (String value) throws Exception {
	return write (split (value, 2, false), true, false);
    }
    private Boolean write (String[] props, boolean slow, boolean addline) throws Exception {
	Socket clientSocket = _sockets.get (get (props[0]));
	String val = props.length > 1 ? props[1] : "";
	byte[] bytes;
	if (val.startsWith ("@")){
	    bytes = getBytes (get (val));
	} else {
	    bytes = getBytes (unquote (get (val)));
	}
	if (addline){
	    byte[] tmp = new byte[bytes.length+2];
	    System.arraycopy (bytes, 0, tmp, 0, bytes.length);
	    tmp[bytes.length] = (byte)'\r';
	    tmp[bytes.length+1] = (byte)'\n';
	    bytes = tmp;
	}
	if (!slow){
	    clientSocket.getOutputStream ().write (bytes);
	    clientSocket.getOutputStream ().flush ();
	}else{
	    for (int i=0; i<bytes.length; i++){
		clientSocket.getOutputStream ().write (bytes[i]);
		clientSocket.getOutputStream ().flush ();
		Thread.sleep (10);
	    }
	}
	return true;
    }
    
    public Boolean read (String value) throws Exception {
	String[] props = split (value);
	Socket clientSocket = _sockets.get (props[0]);
	InputStream fis = new ByteArrayInputStream (getBytes (props[1]));
	int i1 = 0;
	while (i1 != -1 && (i1=fis.read()) != -1){ // the first case is used when i1 is read in the body below
	    boolean var = false;
	    String name = null;
	    String val = null;
	    if (i1 == '{'){
		var = true;
		name = "";
		while ((i1 = fis.read ()) != '}')
		    name = name+(char)i1;
		val = "";
		i1 = fis.read ();
	    }
	    if (i1 == '$'){
		name = "";
		i1 = fis.read ();
		if ((char)i1 != '(') name = name+(char)i1;
		while (true){
		    i1 = fis.read ();
		    if (i1 == -1) break;
		    char c = (char) i1;
		    if (c == ')' || c <= ' ') break;
		    name = name+c;
		}
		val = get (name);
		byte[] valb = val.getBytes ();
		for (int k=0; k<valb.length; k++){
		    int i = clientSocket.getInputStream ().read ();
		    if (i != valb[k])
			return false;
		}
		continue;
	    }
	    do{
		int i2 = clientSocket.getInputStream ().read ();
		if (i2 == '\r'){ // skip CR
		    i2 = clientSocket.getInputStream ().read ();
		}
		if (var){
		    if (i1 == i2){
			set (name, val);
			var = false;
		    } else {
			val = val + (char)i2;
		    }
		} else {
		    if (i1 != i2)
			return false;
		}
	    }while(var);
	}
	fis.close ();
	return true;
    }

    public Boolean exhaust (String value) throws Exception {
	String[] props = split (value);
	Socket clientSocket = _sockets.get (props[0]);
	if (props.length == 2){
	    int size = Integer.parseInt (props[1]);
	    byte[] dest = new byte[size];
	    int read = 0;
	    while (read < size){
		int i = clientSocket.getInputStream ().read (dest, read, dest.length - read);
		if (i == -1) return false;
		read += i;
		if (read >= size) return true;
	    }
	} else {
	    byte[] dest = new byte[100];
	    int i = clientSocket.getInputStream ().read ();
	    if (i == -1) return false;
	    int total = 1;
	    while (clientSocket.getInputStream ().available () > 0){
		total += clientSocket.getInputStream ().read (dest);
	    }
	    set ("tcp.read", total);
	}
	return true;
    }

}
