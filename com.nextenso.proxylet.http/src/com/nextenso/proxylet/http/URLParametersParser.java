package com.nextenso.proxylet.http;

import java.util.Enumeration;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utility class that parses url query Strings or "application/x-www-form-urlencoded" data to retrieve the parameters.
 */

public class URLParametersParser {
        
    /**
     * The MIME type "application/x-www-form-urlencoded"
     */
    public static final String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
        
    /**
     * The table containing all the query parameters (filled when the query is parsed)
     */
    private Hashtable parameters = new Hashtable ();
    /**
     * The unparsable part of the query (if any)
     */
    private String queryMisc;
    /**
     * A boolean indicating if the query was parsed
     */
    private boolean queryParsed = false;
    /**
     * The query (cached as a String)
     */
    private String cachedQuery;
    /**
     * A boolean indicating if the cached query is up-to-date
     */
    private boolean cachedQueryValid = false;
        
    /**
     * Specified if the body of the message contains application/x-www-form-urlencoded parameters
     * by checking the content-type.
     * @param headers the headers to check
     * @return true or false
     */
    public static boolean hasPostParameters (HttpHeaders headers){
	String ctype = headers.getContentType ();
	if (ctype == null)
	    return false;
	return (ctype.equalsIgnoreCase (X_WWW_FORM_URLENCODED));
    }
        
    /**
     * Initializes a new parameters parser with the specified query String.
     * <br/>The query can optionnally start with '?' (but it should be avoided).
     * <br/>The query can be null (same as "").
     * @param query the query.
     */
    public URLParametersParser (String query){
	init (query);
    }
        
    /**
     * Re-initializes the query with a new value.
     * <br/>The query can optionnally start with '?' (but it should be avoided).
     * <br/>The query can be null (same as "").
     * @param newValue the new query value
     */
    public void reset (String newValue){
	parameters.clear ();
	init (newValue);
    }
        
    private void init (String newValue){
	if (newValue == null || "?".equals (newValue))
	    newValue = "";
	queryMisc = "";
	// we cache
	cachedQueryValid = true;
	if (newValue.length () > 0){
	    cachedQuery = (newValue.charAt (0) == '?')? newValue.substring (1) : newValue;
	    queryParsed = false;
	}
	else{
	    cachedQuery = "";
	    queryParsed = true;
	}
    }
        
    private void invalidateCache (){
	cachedQueryValid = false;
    }
        
    private void parseQuery (){
	if (queryParsed)
	    return;
	// we parse the query
	int start = 0, stop = 0;
	while (stop < cachedQuery.length ()) {
	    stop = cachedQuery.indexOf ('&', stop);
	    if (stop == -1)
		stop = cachedQuery.length ();
	    // we make sure the token is not empty
	    if (start == stop) {
		start = ++stop;
		continue;
	    }
	    String token = cachedQuery.substring (start, stop);
	    int index = token.indexOf ('=');
	    // no '='
	    if (index == -1) {
		queryMisc = (queryMisc.length () == 0)? token : queryMisc+'&'+token;
	    }
	    else if (index == token.length () - 1) {
		// the value is empty
		addParameter (token.substring (0, index), "");
	    } else {
		addParameter (token.substring (0, index), token.substring (index + 1));
	    }
	    start = ++stop;
	}
	queryParsed = true;
    }
        
    /**
     * Private method to add a parameter to the table
     */
    private void addParameter (String name, String value){
	// name and value are never null
	name = HttpUtils.decodeURL (name);
	value = HttpUtils.decodeURL (value);
	Object o = parameters.get (name);
	if (o == null){
	    parameters.put (name, value);
	    return;
	}
	ArrayList list;
	if (o instanceof String){
	    list = new ArrayList (2);
	    list.add (o);
	    list.add (value);
	    parameters.put (name, list);
	}
	else{
	    list = (ArrayList)o;
	    list.add (value);
	}
    }
        
    /**
     * Returns the query - does not prefix with '?'
     * @return the query (as URL-encoded)
     */
    public String getQuery () {
	if (cachedQueryValid)
	    return cachedQuery;
	Enumeration enumeration = getParameterNames ();
	StringBuffer buffer = new StringBuffer ();
	boolean addAnd = false;
	while (enumeration.hasMoreElements ()) {
	    if (addAnd) buffer.append ('&');
	    addAnd = true;
	    String name = (String) enumeration.nextElement ();
	    String name_encoded = URLEncoder.encode (name);
	    Object value = parameters.get (name);
	    if (value instanceof String) {
		buffer.append (name_encoded);
		buffer.append ('=');
		buffer.append (URLEncoder.encode ((String)value));
	    }
	    else{
		ArrayList list = (ArrayList) value;
		for (int i=0; i<list.size (); i++){
		    if (i > 0) buffer.append ('&');
		    buffer.append (name_encoded);
		    Object o = list.get (i);
		    buffer.append ('=');
		    buffer.append (URLEncoder.encode ((String)o));
		}
	    }
	}
	if (queryMisc.length () != 0){
	    if (addAnd) buffer.append ('&');
	    buffer.append (queryMisc);
	}
	cachedQueryValid = true;
	return (cachedQuery = buffer.toString ());
    }
        
    /**
     * Returns the number of parameters
     * @return the number of parameters
     */
    public int getParametersSize () {
	parseQuery ();
	return parameters.size ();
    }

    /**
     * Returns the number of values for a given parameter
     * @param name The parameter name (given as URL-decoded)
     * @return the number of values
     */
    public int getParameterSize (String name) {
	if (name == null)
	    return 0;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return 0;
	if (o instanceof String)
	    return 1;
	return ((ArrayList)o).size ();
    }
        
    /**
     * Returns the list of parameter names (URL-decoded)
     * @return the list of parameter names (URL-decoded)
     */
    public Enumeration getParameterNames () {
	parseQuery ();
	return parameters.keys ();
    }
        
    /**
     * Returns wether or not the specified parameter is single-valued.
     * <br/>NOTE: Returns false if the parameter does not exist.
     * @param name The parameter name (given as URL-decoded)
     * @return true (use getParameterValue(..)) or false (use getParameterValues(..))
     */
    public boolean isSingleValuedParameter (String name) {
	if (name == null)
	    return false;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return false;
	return (o instanceof String);
    }

    /**
     * Returns wether or not the specified parameter is multi-valued.
     * <br/>NOTE: Returns false if the parameter does not exist.
     * @param name The parameter name (given as URL-decoded)
     * @return true (use getParameterValue(..)) or false (use getParameterValues(..))
     */
    public boolean isMultiValuedParameter (String name) {
	if (name == null)
	    return false;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return false;
	return (o instanceof ArrayList);
    }
        
    /**
     * Returns the value of a given parameter.
     * <br/>It can be : a String (single-valued parameter)
     * <br/>It can be : an ArrayList (multi-valued parameter)
     * <br/>It can be : null (unknown parameter)
     * @param name The parameter name (given as URL-decoded)
     * @return the value of the specified parameter
     */
    public Object getParameterValue (String name){
	if (name == null)
	    return null;
	parseQuery ();
	return parameters.get (name);
    }
  
    /**
     * Returns the value of a given single-valued parameter.
     * <br/>Throws an IllegalArgumentException if the parameter is multi-valued.
     * <br/>It can be : a String (ex: the query is : ?name=bill)
     * <br/>It can be : a blank String (ex: the query is : ?name=)
     * <br/>It can be : null (ex: the query is : ?name or the parameter does not exist (see containsParameter(..) to differentiate the 2 cases)
     * @param name The parameter name (given as URL-decoded)
     * @param getEncodedValue If true returns the URL-encoded value - if false returns the URL-decoded value
     * @return the value of the specified parameter
     * @exception IllegalArgumentException if the parameter is multi-valued
     */
    public String getParameterValue (String name, boolean getEncodedValue)
        throws IllegalArgumentException{
	if (name == null)
	    return null;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return null;
	if (o instanceof ArrayList)
	    throw new IllegalArgumentException ("Parameter "+name+" is multi-valued");
	return (getEncodedValue)? URLEncoder.encode ((String)o) : (String)o ;
    }

    /**
     * Returns a value of a given multi-valued parameter.
     * <br/>An Exception can be thrown if the specified index is not valid.
     * <br/>If it is single-valued, the value is returned (no matter the index).
     * <br/>It can be : a String (ex: the query is : ?name=bill)
     * <br/>It can be : a blank String (ex: the query is : ?name=)
     * <br/>It can be : null (ex: the query is : ?name or the parameter does not exist (see containsParameter(..) to differentiate the 2 cases)
     * @param name The parameter name (given as URL-decoded)
     * @param index The index in the list of values.
     * @param getEncodedValue If true returns the URL-encoded value - if false returns the URL-decoded value
     * @return the value of the specified parameter
     */
    public String getParameterValue (String name, int index, boolean getEncodedValue) {
	if (name == null)
	    return null;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return null;
	if (o instanceof String)
	    return (getEncodedValue)? URLEncoder.encode ((String)o) : (String)o ;
                
	String s = (String) ((ArrayList)o).get (index);
	return (getEncodedValue)? URLEncoder.encode (s) : s ;
    }
        
    /**
     * Returns the list of values of a given multi-valued parameter.
     * <br/>Returns a list with 1 element if the parameter is single-valued.
     * <br/>Returns null if the parameter does not exist.
     * <br/>Note: the list can contain blank elements.
     * @param name The parameter name (given as URL-decoded)
     * @return the list of values (as URL-decoded) of the specified parameter
     */
    public ArrayList getParameterValues (String name) {
	if (name == null)
	    return null;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return null;
	if (o instanceof String){
	    // then it is a single-valued parameter
	    ArrayList list = new ArrayList (1);
	    list.add (o);
	    return list;                        
	}
	return (ArrayList)o;
    }
        
    /**
     * Sets the value of a given parameter. This parameter will be single-valued.
     * <br/>The name must not be null but can be blank.
     * <br/>The value must not be null but can be blank.
     * @param name The parameter name
     * @param value The parameter value
     * @param encoded If true takes the name and the value as URL-encoded (method is slower if encoded=true)
     * @exception MalformedURLException when the name or value is invalid (then no action is performed)
     */
    public void setParameter (String name, String value,
			      boolean encoded) throws MalformedURLException {
	if (name == null)
	    throw new NullPointerException ("Null parameter name");
	if (value == null)
	    throw new NullPointerException ("Null parameter value");
	if (encoded){
	    if (!HttpUtils.isValidURLQuery (name))
		throw new MalformedURLException ("Invalid parameter name : "+name);
	    name = HttpUtils.decodeURL (name);
	    if (!HttpUtils.isValidURLQuery (value))
		throw new MalformedURLException ("Invalid parameter value : "+value);        
	    value = HttpUtils.decodeURL (value);
	}
	parseQuery ();
	parameters.put (name, value);
	invalidateCache ();
    }

    /**
     * Adds a value to a given parameter.
     * <br/>It is possible to specify if the new value should be added if already present (and avoid duplicates).
     * <br/>The name must not be null but can be blank.
     * <br/>The value must not be null but can be blank.
     * @param name The parameter name
     * @param value The parameter value
     * @param encoded If true takes the name and the value as URL-encoded (method is slower if encoded=true)
     * @param unique If true the new value is not added if already present, if false it is always added
     * @return the new number of values for the parameter
     * @exception MalformedURLException when the name or value is invalid
     */
    public int addParameter (String name, String value,
			     boolean encoded, boolean unique) throws MalformedURLException {
	if (name == null)
	    throw new NullPointerException ("Null parameter name");
	if (value == null)
	    throw new NullPointerException ("Null parameter value");
	if (encoded){
	    if (!HttpUtils.isValidURLQuery (name))
		throw new MalformedURLException ("Invalid parameter name : "+name);
	    name = HttpUtils.decodeURL (name);
	    if (!HttpUtils.isValidURLQuery (value))
		throw new MalformedURLException ("Invalid parameter value : "+value);        
	    value = HttpUtils.decodeURL (value);
	}
    
	parseQuery ();
    
	Object o = parameters.get (name);
	if (o == null){
	    parameters.put (name, value);
	    invalidateCache ();
	    return 1;
	}
	ArrayList list;
	if (o instanceof String){
	    if (unique){
		if (value.equals (o))
		    return 1;
	    }
	    list = new ArrayList (2);
	    list.add (o);
	    list.add (value);
	    parameters.put (name, list);
	    invalidateCache ();
	    return 2;
	}
	list = (ArrayList)o;
	if (unique){
	    if (!list.contains (value)){
		list.add (value);
		invalidateCache ();
	    }
	}
	else{
	    list.add (value);
	    invalidateCache ();
	}
	return list.size ();
    }
        
    /**
     * Removes a given parameter
     * @param name The parameter name (given as URL-decoded)
     * @return the removed value
     */
    public Object removeParameter (String name) {
	if (name == null)
	    return null;
	parseQuery ();
	Object o = parameters.remove (name);
	if (o != null)
	    invalidateCache ();
	return o;
    }

    /**
     * Clears the query (removes all the parameters) - same as reset(null)
     */
    public void clearQuery (){
	reset ("");
    }
        
    /**
     * Cleans the query by removing useless '&' from the original query String
     */
    public void cleanQuery (){
	parseQuery ();
	invalidateCache ();
    }
        
    /**
     * Removes duplicates values for a given parameter and returns the new number of values.
     * @param name the parameter name
     */
    public int trim (String name){
	if (name == null)
	    return 0;
	parseQuery ();
	Object o = parameters.get (name);
	if (o == null)
	    return 0;
	if (o instanceof String)
	    return 1;
	ArrayList oldV = (ArrayList)o;
	ArrayList newV = new ArrayList (oldV.size ());
	for (int i = 0; i<oldV.size (); i++){
	    Object v = oldV.get (i);
	    if (!newV.contains (v))
		newV.add (v);
	    else
		invalidateCache ();
	}
	parameters.put (name, newV);
	return newV.size ();
    }
        
    /**
     * Removes duplicates values for all the parameters.
     */
    public void trim (){
	Enumeration enumeration = getParameterNames ();
	while (enumeration.hasMoreElements ())
	    trim ((String)enumeration.nextElement ());
    }
        
}

