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

public class SessionEnabPlayer extends TestPlayer {
    
    public SessionEnabPlayer (){
    }

    // enab.create : type id [expire]
    // enab.create : @json
    public Boolean create (String value) throws Exception {
	if (value.startsWith ("@")){
	    if (play ("http.post",
		      "$enab.server application/json "+value
		      ) &&
		play ("equal",
		      "http.resp.code 201")
		){
		set ("enab.session.url", get ("$http.resp.header.location"));
		return true;
	    } else
		return false;
	} else {
	    String[] props = split (value);
	    String expire = props.length > 2 ? "&expire="+props[2] : "";
	    // we escape the id in particular which can include a $
	    if (play ("http.post",
		      "$enab.server application/x-www-form-urlencoded type="+escape (props[0])+"&id="+escape (props[1])+expire
		      ) &&
		play ("equal",
		      "http.resp.code 201")
		){
		set ("enab.session.url", get ("$http.resp.header.location"));
		return true;
	    } else
		return false;
	}
    }
    
    // enab.locate: type id
    public Boolean locate (String value) throws Exception {
	String[] props = split (value);
	String tmp = get ("$http.req.header.accept");
	if (tmp.length () == 0) tmp = null;
	set ("http.req.header.accept", "text/uri-list");
	try{
	    if (play ("http.get",
		      "$(enab.server)?type="+java.net.URLEncoder.encode (escape (props[0]))+"&id="+java.net.URLEncoder.encode (escape (props[1]))
		      ) &&
		play ("equal",
		      "http.resp.code 200")
		){
		set ("enab.session.url", get ("$http.resp.body").trim ());
		return true;
	    } else
		return false;
	}finally{set ("http.req.header.accept", tmp);}
    }

    // enab.get-attr: attr
    public Boolean getattr (String value) throws Exception {
	if (play ("http.get",
		  "$(enab.session.url)/"+java.net.URLEncoder.encode (get (value))
		  ) &&
	    play ("equal",
		  "http.resp.code 200")
	    ){
	    set ("enab.session.attr."+get (value), get ("$http.resp.body"));
	    return true;
	} else
	    return false;
    }
    
    public Boolean equalattr (String value) throws Exception {
	String[] props = split (value, 2, false);
	String attr = props[0];
	String val = props[1];
	return
	    play ("http.get",
		  "$(enab.session.url)/"+java.net.URLEncoder.encode (get (attr))
		  ) &&
	    play ("equal",
		  "http.resp.code 200"
		  ) &&
	    play ("equal",
		  "http.resp.body "+get (val));
    }

    public Boolean compareattr (String value) throws Exception {
	String[] props = split (value, 2, false);
	String attr = props[0];
	String val = props[1];
	return
	    play ("http.get",
		  "$(enab.session.url)/"+java.net.URLEncoder.encode (get (attr))
		  ) &&
	    play ("equal",
		  "http.resp.code 200"
		  ) &&
	    play ("equal",
		  "compare.resp.body "+get (val));
    }

    public Boolean deleteattr (String value) throws Exception {
	return
	    play ("http.delete",
		  "$(enab.session.url)/"+java.net.URLEncoder.encode (get (value))
		  ) &&
	    play ("equal",
		  "http.resp.code 204"
		  );
    }

    public Boolean destroyattr (String value) throws Exception {
	return
	    play ("http.delete",
		  "$(enab.session.url)/"+java.net.URLEncoder.encode (get (value))
		  );
    }

    public Boolean update (String value) throws Exception {
	return
	    play ("http.post",
		  "$enab.session.url application/json "+value
		  ) &&
	    play ("equal",
		  "http.resp.code 200"
		  );
    }

    // enab.update-attr: attr content-type @value
    // enab.update-attr: attr value
    public Boolean updateattr (String value) throws Exception {
	String[] props = split (value, false);
	if (props.length > 2){
	    if (play ("http.post",
		      "$(enab.session.url)/"+java.net.URLEncoder.encode (get (props[0]))+" "+concatenate (props, 1)
		      ) &&
		play ("equal",
		      "http.resp.code 204")
		){
		return true;
	    } else
		return false;	    
	} else {
	    if (play ("http.post",
		      "$(enab.session.url)/"+java.net.URLEncoder.encode (get (props[0]))+" text/plain "+props[1]
		      ) &&
		play ("equal",
		      "http.resp.code 204")
		){
		return true;
	    } else
		return false;
	}
    }

    public Boolean clean (String value) throws Exception {
	return
	    play ("http.delete",
		  "$(enab.server)?type="+value
		  ) &&
	    play ("equal",
		  "http.resp.code 204"
		  );
    }

    public Boolean destroy (String value) throws Exception {
	return
	    play ("http.delete",
		  "$enab.session.url"
		  ) &&
	    play ("equal",
		  "http.resp.code 204"
		  );
    }

}