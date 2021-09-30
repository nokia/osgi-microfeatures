package sun.security.ssl;

public class RecordStatic implements Record {
	/**
     * Return a description for the given content type.
     */
    static String contentName(byte contentType) {
        switch (contentType) {
        case ct_change_cipher_spec:
            return "Change Cipher Spec";
        case ct_alert:
            return "Alert";
        case ct_handshake:
            return "Handshake";
        case ct_application_data:
            return "Application Data";
        default:
            return "contentType = " + contentType;
        }
    }

    static boolean isValidContentType(byte contentType) {
        return (contentType == 20) || (contentType == 21) ||
               (contentType == 22) || (contentType == 23);
    }
}
