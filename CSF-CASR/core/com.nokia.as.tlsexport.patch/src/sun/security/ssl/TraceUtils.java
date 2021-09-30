package sun.security.ssl;

import javax.net.ssl.SSLSession;
import java.io.IOException;

public class TraceUtils {

    static String stackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.fillInStackTrace();
        e.printStackTrace(pw);
        String sStackTrace = sw.toString();
        pw.close();
        try {
            sw.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return sStackTrace;
    }

    static void here(String description) {
        Exception e = new Exception(description);
        e.fillInStackTrace();
        System.err.println(stackTrace(e));
    }

    static String session(SSLSessionImpl s) {
        if (s == null) {
            return "session@null";
        }
        return "sessionImpl@"+s.toString();
    }
}
