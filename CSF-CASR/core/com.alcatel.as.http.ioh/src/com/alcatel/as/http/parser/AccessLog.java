package com.alcatel.as.http.parser;


public class AccessLog {

    protected String _remoteIP;
    protected String _user;
    protected java.util.Date _timestamp;
    protected String _method;
    protected String _url;
    protected int _version;
    protected int _status = -1;
    protected int _respSize = -1;

    protected AccessLog _next; // used in cases of pipelining : may be useful to chain accesslogs pending responses

    public AccessLog (){
    }
    public boolean isEmpty (){ return _timestamp == null; }
    public AccessLog touch (){ _timestamp = new java.util.Date (); return this;}
    
    public AccessLog next (AccessLog next){
	AccessLog tmp = this;
	while (tmp._next != null) tmp = tmp._next;
	tmp._next = next;
	return this;
    }
    public AccessLog next (){ return _next; }
    
    public AccessLog request (HttpMessage req){
	// remoteIP not here
	return touch ()
	    .username (req.getHeaderValue ("authorization"))
	    .method (req.getMethod ())
	    .url (req.getURL ())
	    .version (req.getVersion ());
    }
    public AccessLog remoteIP (String ip){
	_remoteIP = ip;
	return this;
    }
    public AccessLog remoteIP (java.net.InetAddress ip){
	_remoteIP = ip.getHostAddress ();
	return this;
    }
    public AccessLog responseStatus (int status){
	_status = status;
	return this;
    }
    public AccessLog responseSize (int size){
	_respSize = size;
	return this;
    }
    public AccessLog incResponseSize (int inc){
	if (_respSize == -1) _respSize = inc;
	else _respSize += inc;
	return this;
    }

    // all the following are set in request (HttpMessage req)
    public AccessLog username (String authorization){
	_user = parseUsername (authorization);
	return this;
    }
    public AccessLog method (String method){
	_method = method;
	return this;
    }
    public AccessLog url (String url){
	_url = url;
	return this;
    }
    public AccessLog version (int v){
	_version = v;
	return this;
    }


    public static String parseUsername (String auth) {
	if (auth == null) return "-";
	int start = auth.indexOf("username");
	if (start == -1) {
	    start = auth.toLowerCase(java.util.Locale.getDefault()).indexOf("username");
	    if (start == -1) {
		return "-";
	    }
	}

	start = trim(auth, start + 8);
	if (start == -1) {
	    return "-";
	}
	char c = auth.charAt(start);
	if (c != '=') {
	    return "-";
	}
	start = trim(auth, start + 1);
	if (start == -1) {
	    return "-";
	}

	c = auth.charAt(start);
	boolean hasSep = c == '"' || c == '\'';
	char sep = hasSep ? c : ',';

	if (hasSep) {
	    start++;
	    if (start == auth.length()) {
		return "-";
	    }
	}

	int end = auth.indexOf(sep, start);
	if (end == -1) {
	    end = auth.length();
	}

	if (hasSep) {
	    return auth.substring(start, end);
	}
	return auth.substring(start, end).trim();
    }
    private static int trim(String s, int f) {
	int from = f;
	while (from < s.length()) {
	    if (s.charAt(from) != ' ') {
		return from;
	    }
	    from++;
	}
	return -1;
    }




    
}
