/**
 * 
 */
package javax.servlet.sip;

import javax.servlet.ServletException;

public class TooManyHopsException extends ServletException {

    private static final long serialVersionUID = 1L;

    public TooManyHopsException() {
    }

    public TooManyHopsException(String message) {
        super(message);
    } 
}
