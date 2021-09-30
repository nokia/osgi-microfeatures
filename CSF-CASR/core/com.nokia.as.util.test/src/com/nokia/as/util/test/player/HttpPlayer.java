package com.nokia.as.util.test.player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpPlayer extends TestPlayer implements HttpHandler {

    public HttpPlayer () throws Exception {
	set ("http.server.get.code", "404");
    }

    public String getMethod (String cmd){
	if (cmd.equals ("get")) return "_get";
	return super.getMethod (cmd);
    }

    public Boolean post (String value) throws Exception {
	String[] props = split (value);
	URL url = new URL (props[0]);
	HttpURLConnection conn = (HttpURLConnection) url.openConnection ();
	conn.setInstanceFollowRedirects (false);
	conn.setDoInput (true);
	conn.setDoOutput (true);
	conn.setRequestMethod ("POST");
	conn.setRequestProperty ("Content-Type", props[1]);
	setHttpReq (conn);
	conn.connect ();
	conn.getOutputStream ().write (props[2].getBytes ("iso-8859-1"));
	conn.getOutputStream ().flush ();
	return parseHttpResp (conn);
    }

    public Boolean put (String value) throws Exception {
	String[] props = split (value);
	URL url = new URL (props[0]);
	HttpURLConnection conn = (HttpURLConnection) url.openConnection ();
	conn.setInstanceFollowRedirects (false);
	conn.setDoInput (true);
	conn.setDoOutput (true);
	conn.setRequestMethod ("PUT");
	conn.setRequestProperty ("Content-Type", props[1]);
	setHttpReq (conn);
	conn.connect ();
	conn.getOutputStream ().write (props[2].getBytes ("iso-8859-1"));
	conn.getOutputStream ().flush ();
	return parseHttpResp (conn);
    }

    public Boolean _get (String value) throws Exception {
	String[] props = split (value);
	URL url = new URL (props[0]);
	HttpURLConnection conn = (HttpURLConnection) url.openConnection ();
	conn.setInstanceFollowRedirects (false);
	conn.setDoInput (true);
	conn.setDoOutput (false);
	conn.setRequestMethod ("GET");
	setHttpReq (conn);
	conn.connect ();
	return parseHttpResp (conn);
    }

    public Boolean delete (String value) throws Exception {
	String[] props = split (value);
	URL url = new URL (props[0]);
	HttpURLConnection conn = (HttpURLConnection) url.openConnection ();
	conn.setInstanceFollowRedirects (false);
	conn.setRequestMethod ("DELETE");
	setHttpReq (conn);
	conn.connect ();
	return parseHttpResp (conn);
    }

    public Boolean reset (String value) throws Exception {
	String s = value != null ? getPrefix ()+"http."+value+"." : getPrefix ()+"http.";
	List<String> list = new ArrayList<String> ();
	for (String header : System.getProperties ().stringPropertyNames ())
	    if (header.startsWith (s))
		list.add (header);
	for (String h : list) set (h, null);
	return true;
    }

    private void setHttpReq (HttpURLConnection conn) throws Exception {
	for (String header : System.getProperties ().stringPropertyNames ())
	    if (header.startsWith (getPrefix ()+"http.req.header."))
		conn.setRequestProperty (header.substring ((getPrefix () + "http.req.header.").length ()), System.getProperty (header));
    }
    
    private Boolean parseHttpResp (HttpURLConnection conn) throws Exception {
	reset ("resp");
	Integer code = conn.getResponseCode ();
	set (getPrefix ()+"http.resp.code", code.toString ());
	for (String header : conn.getHeaderFields ().keySet ()){
	    if (header == null) continue; //happens with DELETE !!!
	    set (getPrefix ()+"http.resp.header."+header.toLowerCase (), conn.getHeaderField (header));
	}
	StringBuilder sb = new StringBuilder ();
	if (code >= 200 && code < 300){
	    InputStream in = conn.getInputStream ();
	    int i;
	    while ((i=in.read()) != -1){
		sb.append ((char)i);
	    }
	    set (getPrefix ()+"http.resp.body", sb.toString ());
	} else
	    set (getPrefix ()+"http.resp.body", null);
	return true;
    }

    public boolean server (String value) throws Exception {
	String[] toks = split (value);
	InetSocketAddress addr = new InetSocketAddress(asInt (toks[0]));
	HttpServer server = HttpServer.create(addr, 0);

	server.createContext("/", this);
	server.start();
	return true;
    }

    public void handle(HttpExchange exchange) throws IOException {
	try{
	    String requestMethod = exchange.getRequestMethod();
	    if (requestMethod.equalsIgnoreCase("GET")) {
		String tmp = get ("$http.server.get");
		if (tmp.length () == 0) tmp = "0";
		set ("http.server.get", String.valueOf (asInt (tmp)+1));
		Headers responseHeaders = exchange.getResponseHeaders();
		if (get ("$http.server.get.content-type").length () > 0){
		    responseHeaders.set("Content-Type", get ("$http.server.get.content-type"));
		    exchange.sendResponseHeaders(asInt (get ("$http.server.get.code")), 0);
		    OutputStream responseBody = exchange.getResponseBody();
		    responseBody.write (getBytes (unquote (get ("$http.server.get.content"))));
		    responseBody.close();
		} else {
		    exchange.sendResponseHeaders(asInt (get ("$http.server.get.code")), 0);
		    exchange.getResponseBody().close ();
		}	    
	    }
	}catch(Exception e){
	    e.printStackTrace ();
	}
    }
}