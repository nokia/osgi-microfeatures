// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.osgi.launcher.impl;

import java.net.URL;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OtherUtils {

    /**
     * Chains comparators by thenComparing
     * Example chainComparators(a, b, c) => ((s1, s2) -> 0).thenComparing(a).thenComparing(b).thenComparing(c)
     * @param criteria = the comparators to chain
     * @return the chained comparator
     */
    @SafeVarargs
    public static <T> Comparator<T> chainComparators(Comparator<T>... criteria) {
    	Stream.of(criteria)
    		  .forEach(Objects::requireNonNull);
    	
    	Comparator<T> initial = (s1, s2) -> 0; //identity comparator
        return Stream.of(criteria)
        			 .reduce(initial, 
        					 (c1, c2) -> c1.thenComparing(c2));
    }
    
    /**
     * Calls a method from an object
     * Example callMethod(object, "myMethod", new Class<?>[]{String.class}, new Object[]{"parameter"}, Throwable::printStackTrace)
     * @param object the object
     * @param method the method name
     * @param types the types of the arguments
     * @param arguments the arguments of the method
     * @param exception what to do if exception
     * @return the return value of the method, empty if error or null
     */
    public static Optional<Object> callMethod(Object object, String method, Class<?>[] types, 
    										  Object[] arguments, Consumer<Throwable> exception) {
    	Objects.requireNonNull(object);
    	Objects.requireNonNull(exception);
    	
    	Stream.of(types)
    		  .forEach(Objects::requireNonNull);
    	Stream.of(arguments)
		  	  .forEach(Objects::requireNonNull);
    	
    	try {
    		Object returned = object.getClass()
    								.getMethod(method, types)
    								.invoke(object, arguments);
    		return Optional.ofNullable(returned);
    	} catch(Exception e) {
    		exception.accept(e);
    		return Optional.empty();
    	}
    }
    
    /**
     * Trims an url
     * @param url the url
     * @param toRemove the part to remove
     * @param exception what to do if exception
     * @return
     */
    public static URL trimURL(URL url, String toRemove, Consumer<Throwable> exception) {
    	Objects.requireNonNull(url);
    	Objects.requireNonNull(toRemove);
    	Objects.requireNonNull(exception);
    	
    	if(!(url.toString().endsWith(toRemove)))
    			throw new IllegalArgumentException(url + "does not finish with " + toRemove);
    	
    	String externalForm = url.toExternalForm();
    	try {
    		return new URL(externalForm.substring(0, externalForm.length() - toRemove.length()));
    	} catch(Exception e) {
    		exception.accept(e);
    		return url;
    	}
    }

}
