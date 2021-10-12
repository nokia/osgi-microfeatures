// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.impl.h2;

import java.util.*;
import java.io.*;

public class ConfigHelper {

    public static List<String> getLines (String data, String prefix){
	if (data == null) data = "#";
	prefix = prefix+" ";
	BufferedReader reader = new BufferedReader(new StringReader(data));
	ArrayList<String> ret = new ArrayList<> ();
	try {
	    String line;
	    while ((line = reader.readLine()) != null) {
		line = line.trim();
		if (line.startsWith("#") || line.length() == 0)
		    continue;
		if (line.startsWith (prefix)) ret.add (line);
	    }
	    reader.close ();
	} catch (java.io.IOException ioe) {
	    // no io possible
	}
	return ret;
    }

    public static final String getParam(String line, boolean required, String ... pnames) {
	for (String pname : pnames) {
	    String p = " " + pname + " ";
	    int index = line.indexOf(p);
	    if (index == -1)
		continue;
	    index += p.length();
	    if (index == line.length()){
		// this should be a flag !
		if (required) throw new IllegalArgumentException ("Invalid line : "+line);
		return null;
	    }
	    int end = line.indexOf(' ', index);
	    if (end == -1)
		end = line.indexOf('\t', index);
	    if (end == -1)
		end = line.length();
	    return line.substring(index, end);
	}
	if (required) throw new IllegalArgumentException ("Invalid line : "+line);
	return null;
    }
    public static final String getParam(String line, String def, String ... pnames) {
	String ret = getParam (line, false, pnames);
	return ret != null ? ret : def;
    }
    public static final boolean getFlag(String line, boolean def, String ... fnames) {
	for (String fname : fnames) {
	    String f = " " + fname;
	    int index = line.indexOf(f);
	    if (index == -1)
		continue;
	    int end = index + f.length ();
	    if (line.length () == end) return true;
	    if (line.charAt (end) == ' ' || line.charAt (end) == '\t') return true;
	}
	return def;
    }

    public static final List<String> getParams(String line, boolean required, String ... pnames) {
	List<String> ret = getParams (line, new ArrayList<String> (), pnames);
	if (required && ret.size () == 0) throw new IllegalArgumentException ("Invalid line : "+line);
	return ret;
    }
    public static final List<String> getParams(String line, List<String> dest, String ... pnames) {
	for (String pname : pnames){
	    String p = " " + pname + " ";
	    int index = line.indexOf(p);
	    if (index == -1)
		continue;
	    index += p.length();
	    if (index == line.length())
		continue; // this should be a flag !
	    int end = line.indexOf(' ', index);
	    if (end == -1)
		end = line.indexOf('\t', index);
	    if (end == -1)
		end = line.length();
	    dest.add (line.substring(index, end));
	    if (end < line.length ())
		getParams (line.substring (end), dest, pname);
	}
	return dest;
    }

    public static final int getIntParam (String line, int def, String... pnames){
	String s = getParam (line, null, pnames);
	if (s == null) return def;
	if (s.startsWith ("0x")) return Integer.parseInt (s.substring (2), 16);
	return Integer.parseInt (s);
    }
    public static final int getIntParam (String line, String... pnames){ // means required
	String s = getParam (line, true, pnames);
	if (s.startsWith ("0x")) return Integer.parseInt (s.substring (2), 16);
	return Integer.parseInt (s);
    }
}
