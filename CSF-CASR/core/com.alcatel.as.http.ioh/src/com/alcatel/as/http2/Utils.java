package com.alcatel.as.http2;

import java.util.*;
import java.net.*;
import org.apache.log4j.Logger;

public class Utils {


    public static int getIntProperty (String name, Map<String, Object> props, int def){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String)
		return Integer.parseInt (((String)o).trim ());
	    return ((Number) o).intValue ();
	}
	return def;
    }

    public static long getLongProperty (String name, Map<String, Object> props, long def){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String)
		return Long.parseLong (((String)o).trim ());
	    return ((Number) o).longValue ();
	}
	return def;
    }

    public static boolean getBooleanProperty (String name, Map<String, Object> props, boolean def){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String) return Boolean.parseBoolean (((String)o).trim ());
	    return ((Boolean) o).booleanValue ();
	}
	return def;
    }

    public static InetAddress getInetAddressProperty (String name, Map<String, Object> props, InetAddress def){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String){
		try{
		    return InetAddress.getByName ((String) o);
		}catch(Exception e){
		    return def;
		}
	    }
	    return (InetAddress) o;
	}
	return def;
    }

    public static List<String> getStringListProperty (String name, Map<String, Object> props){
	Object o = props.get (name);
	if (o == null) return null;
	if (o instanceof List) return (List<String>) o;
	List<String> list = new ArrayList<String> ();
	list.add ((String)o);
	return list;
    }

    public static Logger getLoggerProperty (String name, Map<String, Object> props, Logger def){
	Object o = props.get (name);
	if (o == null) return def;
	if (o instanceof Logger) return (Logger) o;
	return Logger.getLogger (o.toString ());
    }

    private static final int STATE_4_6 = 46;
    private static final int STATE_4   = 4;
    private static final int STATE_6   = 6;
    private static final int ERROR     = 0;

    public static boolean isHostName (String name){

        int state = STATE_4_6;
        int dot   = 0;
        int colon = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            switch (state) {
                case STATE_4_6:
                    switch (c) {
                        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                            break;
                        case 'a': case 'A': case 'b': case 'B': case 'c': case 'C': case 'd': case 'D': case 'e': case 'E': case 'f': case 'F':
                            state = STATE_6;
                            break;
                        case '.':
                            if (i == 0)
                                state = ERROR;
                            else
                                state = STATE_4;
                            dot++;
                            break;
                        case ':':
                            state = STATE_6;
                            colon++;
                            break;
                        default:
                            state = ERROR;
                            break;
                    }
                    break;
                case STATE_4:
                    switch (c) {
                        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                            break;
                        case '.':
                            dot++;
                            break;
                        default:
                            state = ERROR;
                            break;
                    }
                    break;
                case STATE_6:
                    switch (c) {
                        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                        case 'a': case 'A': case 'b': case 'B': case 'c': case 'C': case 'd': case 'D': case 'e': case 'E': case 'f': case 'F':
                            break;
                        case ':':
                            colon++;
                            break;
                        case '.':
                            dot++;
                            if (colon > 0)
                                state = STATE_4;
                            else
                                state = ERROR;
                            break;
                        default:
                            state = ERROR;
                            break;
                    }
                    break;
                case ERROR:
                    return true;
                default:
                    state = ERROR;
                    break;
            }
        }
        if ((state == STATE_4 && dot == 3) || (state == STATE_6 && colon > 0))
            return false;
        return true;
    }
    
    /*
     * for documentation and testing :
    static public void hostname_test(String[] argv) {
        test("127.0.0.1", false);
        test(".127.0.0.1", true);
        test(".0.0.1", true);
        test("127.0.0.1.9", true);
        test("127.0.0.1.", true);
        test("127.0.0", true);
        test("toto.com", true);
        test("::1", false);
        test("127.0.0.toto", true);
        test("1234:5400:0:3::1347:82d2", false);
        test("1:2:3:4:5:6:77.77.88.88", false);
        test("ABCDEF.1.2.3", true);
        test("BAD", true);

        // corner cases not handled
        test("0...", false);
        test(":...", false);
        test("127.0.0.", false);
        test("127.0.0.", false);
        test("999.257.0.1", false);
        test("1:2:3:4:5:6:ABCDEF.77.88.88", false);
        test("AAAAAAAAAAAAAA:", false);
    }

    static void test(String host, boolean expected) {

        System.out.println("    ------------------ ");
        System.out.println(host + " : " + expected);
        assert (isHostName(host) == expected);
    }
    */

}
