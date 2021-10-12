// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine.tools;

import java.io.IOException;

public class StreamTokenizerUtils {
    
    private StreamTokenizerUtils (){
    }
    
    public static int getWordAsInt (MyStreamTokenizer st, String exceptionInfo) throws IOException {
	int n = 0;
	char c;
	char[] word = st.getWord ();
	int len = st.getWordLength ();
	for (int i = 0; i<len; i++){
	    c = word[i];
	    n *= 10;
	    switch (c){
	    case '1': n += 1; break;
	    case '2': n += 2; break;
	    case '3': n += 3; break;
	    case '4': n += 4; break;
	    case '5': n += 5; break;
	    case '6': n += 6; break;
	    case '7': n += 7; break;
	    case '8': n += 8; break;
	    case '9': n += 9; break;
	    case '0': break;
	    default:
		throw new IOException (exceptionInfo);
	    }
	}
	st.resetWord ();
	return n;
    }

    public static String getWordTrimmed (MyStreamTokenizer st){
	int wordLen = st.getWordLength ();
	if (wordLen == 0) return "";
	int first = 0;
	for (int i=0; i<wordLen; i++){
	    char c = st.getWord ()[i];
	    if (c == ' ' || c == '\t') {first++;}
	    else break;
	}
	if (first == wordLen) return "";
	int last = wordLen-1;
	for (int i=last; true; i--){
	    char c = st.getWord ()[i];
	    if (c == ' ' || c == '\t') {last--;}
	    else break;
	}
	st.resetWord ();
	return new String (st.getWord (), first, last+1-first);
    }

    public static boolean compareWord (MyStreamTokenizer st, String comparee){
	char[] word = st.getWord ();
	int len = st.getWordLength ();
	if (comparee.length () != len)
	    return false;
	for (int i = 0; i<len; i++){
	    if (word[i] != comparee.charAt (i))
		return false;
	}
	return true;
    }

    public static boolean wordStartsWith (MyStreamTokenizer st, String prefix){
	char[] word = st.getWord ();
	int len = st.getWordLength ();
	int nb = prefix.length ();
	if (len < nb) return false;
	for (int i=0; i<nb; i++) if (prefix.charAt (i) != word[i]) return false;
	return true;
    }

}
