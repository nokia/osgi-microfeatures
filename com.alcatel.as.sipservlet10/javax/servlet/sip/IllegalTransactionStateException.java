/**
 * 
 */
package javax.servlet.sip;

/**
 * @author christophe
 *
 */
public class IllegalTransactionStateException extends IllegalSessionStateException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param s
     */
    public IllegalTransactionStateException(SipSession s) {
        super(s);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param m
     * @param s
     */
    public IllegalTransactionStateException(String m, SipSession s) {
        super(m, s);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     * @param s
     */
    public IllegalTransactionStateException(String message, Throwable cause, SipSession s) {
        super(message, cause, s);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     * @param s
     */
    public IllegalTransactionStateException(Throwable cause, SipSession s) {
        super(cause, s);
        // TODO Auto-generated constructor stub
    }

}
