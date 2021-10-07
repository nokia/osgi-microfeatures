package com.alcatel.as.http2;


public interface ConnectionListener {

    public default void opened (Connection connection){}

    public default void failed (Connection connection){}

    public default void updated (Connection connection){}

    public default void closed (Connection connection){}
    
}
