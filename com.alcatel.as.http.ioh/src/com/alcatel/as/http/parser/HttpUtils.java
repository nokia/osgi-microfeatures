package com.alcatel.as.http.parser;

public class HttpUtils {

    public static byte[] HTTP10_404;
    public static byte[] HTTP10_100;
    public static byte[] HTTP11_404;
    public static byte[] HTTP11_100;
    public static java.nio.charset.Charset UTF_8 = null;

    static {
	try {
	    UTF_8 = java.nio.charset.Charset.forName("utf-8");
	} catch (Exception e) {
	}
	HTTP10_404 = "HTTP/1.0 404 Not Found\r\n\r\n".getBytes(UTF_8);
	HTTP10_100 = "HTTP/1.0 100 Continue\r\n\r\n".getBytes(UTF_8);
	HTTP11_404 = "HTTP/1.1 404 Not Found\r\n\r\n".getBytes(UTF_8);
	HTTP11_100 = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(UTF_8);
    }
    
}
