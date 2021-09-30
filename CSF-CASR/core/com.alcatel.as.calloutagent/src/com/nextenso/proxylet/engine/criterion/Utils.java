package com.nextenso.proxylet.engine.criterion;

public class Utils {
  
  public static final String AND = "&";
  public static final String OR = "|";
  public static final String NOT = "!";
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  
  public static final String CLIENT_IP = "clientip";
  public static final String CLIENT_ID = "clientid";
  
  public static final String HOST = "host";
  public static final String HOST_IP = "hostip";
  public static final String PORT = "port";
  public static final String PATH = "path";
  
  public static final String HEADER = "header";
  
  public static final String MESSAGE_ATTR = "message-attr";
  public static final String SESSION_ATTR = "session-attr";
  
  public static final String DATE = "date";
  public static final String DAY = "day";
  public static final String MONTH = "month";
  public static final String TIME = "time";
  public static final String FROM = "from";
  public static final String UNTIL = "until";
  
  public static final String[] httpStackUnsupported = { CLIENT_ID, MESSAGE_ATTR, SESSION_ATTR, DATE, DAY,
      MONTH, TIME, FROM, UNTIL, HOST, HOST_IP };
  
  public static String getHttpStackCriterion(String c) {
    // we check unsupported criteria
    iter: for (int i = 0; i < httpStackUnsupported.length; i++) {
      if (c.indexOf(httpStackUnsupported[i]) != -1) {
        c = TrueCriterion.getInstance().toString();
        break iter;
      }
    }
    
    // we switch to Perl regexp
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < c.length(); i++) {
      char ch = c.charAt(i);
      switch (ch) {
      case '*':
        buff.append(".*");
        break;
      case '.':
      case '\u0088': //circumflex
      case '$':
      case '+':
      case '?':
      case '\\': //backslash
        buff.append("\\");
      default:
        buff.append(ch);
      }
    }
    
    return buff.toString();
  }
  
  /**********************************************************************
   * SMS related code
   *********************************************************************/
  
  public static final String ALPHABET = "alphabet";
  public static final String CLASS = "class";
  public static final String TYPE = "type";
  public static final String SIDE = "side";
  public static final String DEST_ADDRESS = "destAddress";
}
