package com.alcatel.as.http.parser;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class CommonLogFormat {

    public static final String PROP_FORMAT_DATE = "http.access.format.date";
    public static final String PROP_LOGGER = "http.access.logger";
    public static final String PROP_LOGGER_LEVEL = "http.access.logger.level";

    protected static Logger DEF_LOGGER = Logger.getLogger ("http.access");

    protected static java.text.SimpleDateFormat DEF_DATE_FORMAT = new java.text.SimpleDateFormat ("[dd/MMM/YYYY:kk:mm:ss Z]");

    protected Logger _logger;
    protected Level _level;
    protected java.text.SimpleDateFormat _dateFormat = DEF_DATE_FORMAT;

    public CommonLogFormat (Logger logger, Level level){
	setLogger (logger);
	setLevel (level);
	_dateFormat = DEF_DATE_FORMAT;
    }
    public CommonLogFormat (Logger logger){
	this (logger, Level.INFO);
    }
    public CommonLogFormat (){
	this (DEF_LOGGER);
    }
    public Logger getLogger (){ return _logger;}
    public Level getLevel (){ return _level;}
    
    public CommonLogFormat setDateFormat (String txt){
	_dateFormat = new java.text.SimpleDateFormat (txt);
	return this;
    }
    public CommonLogFormat setLogger (Logger logger){
	_logger = logger;
	return this;
    }
    public CommonLogFormat setLevel (Level level){
	_level = level;
	return this;
    }
    public CommonLogFormat configure (java.util.Map props){
	String s = (String) props.get (PROP_FORMAT_DATE);
	if (s != null) setDateFormat (s);
	s = (String) props.get (PROP_LOGGER);
	if (s != null) setLogger (Logger.getLogger (s));
	s = (String) props.get (PROP_LOGGER_LEVEL);
	if (s != null){
	    Level level = Level.toLevel (s);
	    if (level != null) setLevel (level);
	}
	return this;
    }

    public boolean isEnabled (){ return _logger.isEnabledFor (_level);}

    public String format (AccessLog log){
	StringBuilder sb = new StringBuilder ();
	sb.append (toString (log._remoteIP))
	    .append (" - ")
	    .append (toString (log._user))
	    .append (' ')
	    .append (toDateString (log._timestamp))
	    .append (" \"")
	    .append (toString (log._method))
	    .append (' ')
	    .append (toString (log._url))
	    .append (' ')
	    .append (version (log._version))
	    .append ("\" ")
	    .append (toString (log._status))
	    .append (' ')
	    .append (toString (log._respSize))
	    ;
	return sb.toString ();
    }

    public boolean log (AccessLog log){
	if (_logger.isEnabledFor (_level)){
	    _logger.log (_level, format (log));
	    return true;
	}
	return false;
    }

    protected static String toString (String s){
	return s != null ? s : "-";
    }
    protected static String toString (int i){
	return i == -1 ? "-" : String.valueOf (i);
    }
    protected static String version (int i){
	switch (i){
	case 0 : return "HTTP/1.0";
	case 1 : return "HTTP/1.1";
	case 2 : return "HTTP/2.0";
	}
	return " - ";
    }
    protected String toDateString (java.util.Date date){
	return date == null ? "-" : _dateFormat.format (date);
    }
}
