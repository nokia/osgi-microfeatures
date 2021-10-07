/**
 * 
 */
package javax.servlet.sip;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author christophe
 * 
 */
public interface Parameterable extends Cloneable {
    Object clone();

    boolean equals(Object o);

    String getParameter(String key) throws NullPointerException;

    Iterator<String> getParameterNames();

    Set<Map.Entry<String, String>> getParameters();

    String getValue();

    void removeParameter(String name) throws IllegalStateException,NullPointerException;

    void setParameter(String name, String value) throws IllegalStateException,NullPointerException;

    void setValue(String value) throws IllegalStateException,NullPointerException;
}
