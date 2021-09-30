package com.nokia.as.osgi.launcher.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringUtils {

    /**
     * Trims a string up to len
     * Example: trim("1234567890", 4) = "1234..."
     * @param str = the string to trim
     * @param len = max characters kept
     * @return the trimmed string
     */
    public static String trim(String str, int len) {
    	Objects.requireNonNull(str);
    	
        if(str.length() <= len) return str;
        return str.substring(0, len) + "...";
    }

    /**
     * Converts seconds into hours and minutes
     * Example: secondsToMinutes(100) -> "01:40"
     * @param seconds = the seconds to convert
     * @return a string expressing the minutes
     */
    public static String secondsToMinutes(int seconds) {
        String res = "";
        res += (seconds / 3600) > 0 ? String.format("%02d", seconds / 3660) + ":"
        							: "";
        res += String.format("%02d", seconds / 60);
        res += ":";
        res += String.format("%02d", seconds % 60);

        return res;
    }
    
    /**
     * Joins a list with a separator string
     * Example: joinList({1, 2, 3), ":") -> "1:2:3"
     * @param list = the list containing the elements to join
     * @param separator = the string separating the elements
     * @return a string joining all the elements of the list
     */
    public static String joinList(List<?> list, String separator) {
    	Objects.requireNonNull(list);
    	Objects.requireNonNull(separator);
    	
    	return list.stream()
    				.map(Object::toString)
    				.collect(Collectors.joining(separator));
    }
	
}
