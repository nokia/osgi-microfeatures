package com.alcatel.as.ioh.impl.conf;

import java.util.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.io.xml.DomDriver;

@XStreamAlias("property")
public class Property {

    public String name;
    public String value;
    public String agent;

    public String getName (){
	return name;
    }

    public String getValue (){
	return value;
    }

    public static Map<String, Object> fillProperties (List<Property> list, Map<String, Object> map){
	if (map == null) map = new HashMap<String, Object> ();
	if (list == null) return map;
	return fillProperties (list, map, true);
    }
    
    private static Map<String, Object> fillProperties (List<Property> list, Map<String, Object> map, boolean add){
	for (Property p : list){
	    boolean already = map.containsKey (p.getName ());
	    if (!already){
		map.put (p.getName (), p.getValue ());
		continue;
	    }
	    if (add == false) continue;
	    Object o = map.get (p.getName ());
	    if (o == null){
		map.put (p.getName (), p.getValue ());
		continue;
	    }
	    List<String> ls = null;
	    if (o instanceof String){
		ls = new ArrayList<String> ();
		ls.add ((String)o);
		map.put (p.getName (), ls);
	    } else 
		ls = (List<String>) o;
	    ls.add (p.getValue ());
	}
	return map;
    }

    public static void fillDefaultProperties (Map<String, Object> props, List<Property> def){
	if (def == null) return;
	fillProperties (def, props, false);
    }

    public static void fillDefaultProperties (Map<String, Object> props, Map<String, Object> def){
	if (def == null) return;
	for (String key: def.keySet ()){
	    if (props.containsKey (key) == false)
		props.put (key, def.get (key));
	}
    }

    public static void fillDefaultProperties (Map<String, Object> props, Map<String, Object> def, String... propNames){
	if (def == null) return;
	for (String key: propNames){
	    if (props.containsKey (key) == false && def.containsKey (key))
		props.put (key, def.get (key));
	}
    }

    public static void fillDefaultStringProperties (Map<String, Object> props, Map<String, String> def){
	if (def == null) return;
	for (String key: def.keySet ()){
	    if (props.containsKey (key) == false)
		props.put (key, def.get (key));
	}
    }

    // May return null if explicitely set to null in props
    public static Object getProperty (String name, Map<String, Object> props, Object def, boolean setDef){
	if (props.containsKey (name))
	    return props.get (name);
	if (setDef) props.put (name, def);
	return def;
    }

    public static int getIntProperty (String name, Map<String, Object> props, int def, boolean setDef){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String){
		Integer i = Integer.parseInt (((String)o).trim ());
		props.put (name, i);
		return i.intValue ();
	    }
	    return ((Number) o).intValue ();
	}
	if (setDef) props.put (name, Integer.valueOf (def));
	return def;
    }

    public static long getLongProperty (String name, Map<String, Object> props, long def, boolean setDef){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String){
		Long l = Long.parseLong (((String)o).trim ());
		props.put (name, l);
		return l.longValue ();
	    }
	    return ((Number) o).longValue ();
	}
	if (setDef) props.put (name, Long.valueOf (def));
	return def;
    }

    public static boolean getBooleanProperty (String name, Map<String, Object> props, boolean def, boolean setDef){
	Object o = props.get (name);
	if (o != null){
	    if (o instanceof String) return Boolean.parseBoolean (((String)o).trim ());
	    return ((Boolean) o).booleanValue ();
	}
	if (setDef) props.put (name, Boolean.valueOf (def));
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
    public static List<String> getStringListProperty (String name, Map<String, String> props, String separator){
	String s = props.get (name);
	if (s == null) return null;
	s = s.trim ();
	List<String> list = new ArrayList<String> ();
	if (separator == null){
	    if (s.length () > 0) list.add (s);
	} else {
	    String[] ss = s.split (separator);
	    for (String tmp : ss){
		tmp = tmp.trim ();
		if (tmp.length () > 0) list.add (tmp);
	    }
	}
	return list.size () > 0 ? list : null;
    }

    public static List<String> getStringListProperty (String name, List<Property> props){
	List<String> list = new ArrayList<String> ();
	for (Property prop: props){
	    String pname = prop.getName ();
	    if (pname.equals (name))
		list.add (prop.getValue ());
	}
	Collections.sort (list);
	return list;
    }

    public static List<Property> clean (List<Property> props, String thisAgent){
	if (thisAgent == null) return props;
	for (int i=0; i<props.size ();){
	    String agent = props.get (i).agent;
	    if (agent == null || thisAgent.equals (agent)){
		i++;
		continue;
	    }
	    props.remove (i);
	}
	return props;
    }
}
