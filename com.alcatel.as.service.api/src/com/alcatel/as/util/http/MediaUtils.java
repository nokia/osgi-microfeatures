// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * 
 * Some utilities to manipulate Media.
 */
public class MediaUtils {
    
    private static final MediaRange ALL_MEDIA = new MediaRange ("*/*");

    /**
     * Extracts the accepted media from the Accept header value
     * 
     * @return the list of MediaRange ordered in DESCENDING preference : the first
     *         is preferred. Preference is specified in rfc 2616 (HTTP 1.1)
     *         section 14.1
     */
    public static List<MediaRange> parseAcceptHeader(String accept) {
	if (accept == null){
	    List<MediaRange> ret = new ArrayList<MediaRange> ();
	    ret.add (ALL_MEDIA);
	    return ret;
	}
	accept = accept.trim();
	ArrayList<MediaRange> list = new ArrayList<MediaRange>();
	if (accept.length() == 0)
	    return list;
	String[] tokens = accept.split(",");
	for (int i = 0; i < tokens.length; i++)
	    list.add(new MediaRange(tokens[i]));
	java.util.Collections.sort(list, java.util.Collections.reverseOrder());
	return list;
    }

    /**
     * Generates an Accept header value from the list of media ranges
     * 
     * @return the header value
     */
    public static String makeAcceptHeader(List<MediaRange> ranges) {
	StringBuilder sb = new StringBuilder();
	boolean notFirst = false;
	for (MediaRange range : ranges) {
	    if (notFirst)
		sb.append(", ");
	    else
		notFirst = true;
	    range.toString(sb);
	}
	return sb.toString();
    }

    /**
     * Indicates if a given media type is part of a list of media ranges.
     * 
     * @return null if not included, otherwise the MediaRange best fitting the media type.<br/>
     *         ex: ("text/*, text/xml, * / *).includes("text/plain") returns "text/*"
     */
    public static MediaRange includes(List<MediaRange> ranges, MediaType mediaType) {
	if (ranges == null)
	    return null;
	MediaRange bestMatch = null;
	for (MediaRange mediaRange : ranges) {
	    MediaRange.RANGE match = mediaRange.includes(mediaType);
	    if (match == null)
		continue;
	    if (bestMatch == null) {
		bestMatch = mediaRange;
		continue;
	    }
	    int comp = mediaRange.getRange().compareTo(bestMatch.getRange());
	    if (comp < 0)
		continue;
	    if (comp > 0) {
		bestMatch = mediaRange;
		continue;
	    }
	    // should not happen : 2 identical match in the same list...return the better q...
	    if (mediaRange.q > bestMatch.q)
		bestMatch = mediaRange;
	}
	return bestMatch;
    }

    /**
     * Returns the preferred MediaType given a list of accepted MediaRanges.<br/>
     * It assumes that both lists (ranges and types) are ordered by preference. Note that the accepted ranges can be null (means all types are supported).
     * @return the preferred media type from the list, null if none is accepted
     */
    public static MediaType getPreferredMediaType(List<MediaRange> acceptedRanges, List<MediaType> mediaTypes) {
	if (acceptedRanges == null)
	    return mediaTypes.size () > 0 ? mediaTypes.get (0) : null;
	MediaType preferred = null;
	MediaRange bestRange = null;
	for (MediaType mediaType : mediaTypes) {
	    MediaRange range = includes(acceptedRanges, mediaType);
	    if (range == null)
		continue;
	    if (bestRange != null){
		if (range.compareTo (bestRange) <= 0) continue;
	    }
	    bestRange = range;
	    preferred = mediaType;
	}
	return preferred;
    }

     /**
     * Indicates if a given media type is an extension of another.
     * 
     * @param first the media type subject to extending the second
     * @param second the media type subject to being extended by the first
     * @return true/false.<br/>
     *         ex: isExtension("text/plain;level=1","text/plain") == true
     */
    public static boolean isExtension (MediaType first, MediaType second){
	if (first == null || second == null)
	    throw new NullPointerException ("argument is null");
	if (!first.type.equals (second.type))
	    return false;
	if (!first.subtype.equals (second.subtype))
	    return false;
	if (second.params.size() == 0)
	    return true;
	if (first.params.size () == 0)
	    return false;
	Iterator<String> it = second.params.navigableKeySet().iterator();
	while (it.hasNext()) {
	    String p = it.next();
	    if (!second.params.get (p).equals (first.params.get (p)))
		return false;
	}
	return true;
    }
    
    /**
     * Returns the root media type, meaning stripped of the parameters.<br/>
     * Note that it returns a MediaRange if the input is a MediaRange.
     * @param mt the media type (or media range)
     * @return the root media type, possibly a MediaRange if mt is a MediaRange
     */
    public static MediaType getRootMediaType (MediaType mt){
	String s = getRoot (mt);
	return (mt instanceof MediaRange) ? new MediaRange (s) : new MediaType (s);
    }

    /**
     * Returns the root media type as String, meaning stripped of the parameters.<br/>
     * ex: root of "text/plain;level=1" is "text/plain"
     * @param mt the media type (or media range)
     * @return the root media type
     */
    public static String getRoot (MediaType mt){
	return new StringBuilder ().append (mt.getType ()).append ('/').append (mt.getSubType ()).toString ();
    }

    public static void main (String[] args) throws Exception {
	System.out.println (parseAcceptHeader (args[0]));
	ArrayList<MediaType> l = new ArrayList<MediaType> ();
	l.add (new MediaType ("text/uri"));
	l.add (new MediaType ("application/json"));
	System.out.println (getPreferredMediaType (parseAcceptHeader(args[0]), l));
    }

}
