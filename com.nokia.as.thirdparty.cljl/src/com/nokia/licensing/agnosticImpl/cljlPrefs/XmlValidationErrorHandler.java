package com.nokia.licensing.agnosticImpl.cljlPrefs;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Error handling for validation issues.
 */
public class XmlValidationErrorHandler implements ErrorHandler {
    /**
     * Set the file name
     *
     * @param fileName
     *            file
     */
    public void setFileName(final String fileName) {
    }

    /**
     * Receive notification of a recoverable error.
     *
     * @param e
     *            exception
     * @throws SAXException
     *             s
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        throw e;
    }

    /**
     * Receive notification of a non-recoverable error.
     *
     * @param e
     *            exception
     * @throws SAXException
     *             s
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
    }

    /**
     * Receive notification of a warning.
     *
     * @param e
     *            exception
     * @throws SAXException
     *             s
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) throws SAXException {
    }
}
