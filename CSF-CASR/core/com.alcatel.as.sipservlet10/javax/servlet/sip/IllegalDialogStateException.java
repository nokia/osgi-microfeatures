/**
 * 
 */
package javax.servlet.sip;


public class IllegalDialogStateException extends IllegalSessionStateException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param s
     */
    public IllegalDialogStateException(SipSession s) {
        super(s);
    }

    /**
     * @param m
     * @param s
     */
    public IllegalDialogStateException(String m, SipSession s) {
        super(m, s);
    }

    /**
     * @param message
     * @param cause
     * @param s
     */
    public IllegalDialogStateException(String message, Throwable cause, SipSession s) {
        super(message, cause, s);
    }

    /**
     * @param cause
     * @param s
     */
    public IllegalDialogStateException(Throwable cause, SipSession s) {
        super(cause, s);
    }

}
