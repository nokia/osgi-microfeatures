package com.alcatel.as.http.parser;


public interface HttpMessageFilter {

    public default void init (HttpMessage msg){}

    public default void method (HttpMessage msg, String method){}

    public default void url (HttpMessage msg, String url){}

    public default void status (HttpMessage msg, int status){}

    public default boolean header (HttpMessage msg, String name, java.util.function.Supplier<String> value){
	return true;
    }
}
