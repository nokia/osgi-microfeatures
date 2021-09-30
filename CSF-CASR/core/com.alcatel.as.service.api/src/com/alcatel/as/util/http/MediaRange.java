package com.alcatel.as.util.http;

/**
 * A MediaRange is a MediaType which may include wildcards and which has a "q"
 * (quality) factor.
 * <p/>
 * MediaRanges can be compared for preference following rfc 2616.<br/>
 * Example : text/*;q=0.5<br/>
 * "text" is the type<br/>
 * "*" is the subtype<br/>
 * q=0.5 is the quality
 */
public class MediaRange
    extends MediaType
    implements Comparable<MediaRange> {

    public static enum RANGE {
	/**
	 * Corresponds to * / *
	 */
	TYPE_WIDE,
	    /**
	     * Corresponds to for ex: text/*
	     */
	    SUBTYPE_WIDE,
	    /**
	     * Corresponds to for ex: text/plain
	     */
	    EXACT_MATCH
	    }

    protected float q = 1;

    public MediaRange(String value) {
	super(value);
	String qs = params.remove("q");
	if (qs != null) {
	    try{
		q = Float.parseFloat(qs);
	    }catch(Exception e){
		throw new IllegalArgumentException("Invalid MediaRange : " + value);
	    }
	}
	if (q<0 || q>1) throw new IllegalArgumentException("Invalid MediaRange : " + value);
    }

    protected boolean isValid() {
	if (type.equals("*") && !subtype.equals("*"))
	    return false;
	// TODO : allow parameters if */* ???
	return true;
    }

    public float q() {
	return q;
    }

    /**
     * Indicates if the specified MediaType is included.<br/>
     * 
     * @return null if not included, or the range type if it is included (the
     *         range type describes the reason of inclusion)
     */
    public RANGE includes(MediaType other) {
	switch (getRange()) {
	case TYPE_WIDE:
	    return RANGE.TYPE_WIDE;
	case SUBTYPE_WIDE:
	    if (type.equals(other.type))
		return RANGE.SUBTYPE_WIDE;
	    return null;
	case EXACT_MATCH:
	    if (equals (other))
		return RANGE.EXACT_MATCH;
	}
	return null;
    }
    
    public RANGE getRange() {
	if (type.equals("*"))
	    return RANGE.TYPE_WIDE;
	if (subtype.equals("*"))
	    return RANGE.SUBTYPE_WIDE;
	return RANGE.EXACT_MATCH;
    }

    public int compareTo(MediaRange other) {
	if (other == null)
	    throw new NullPointerException();
	if (q < other.q)
	    return -1;
	if (q > other.q)
	    return 1;
	int i = getRange ().compareTo (other.getRange ());
	if (i != 0) return i;
	return toString().compareTo(other.toString());
    }

    protected StringBuilder toString(StringBuilder sb) {
	super.toString(sb);
	if (q != 1)
	    sb.append(';').append("q=").append(String.valueOf(q));
	return sb;
    }
}



