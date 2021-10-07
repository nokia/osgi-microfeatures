/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.nokia.licensing.interfaces.LicenseException;


public class LicenseValidator extends BaseLicenseValidator {
    boolean checkCertificateExpiration = false;

    public LicenseValidator() {
    }

    public LicenseValidator(final boolean checkCertificateExpiration) {
        this.checkCertificateExpiration = checkCertificateExpiration;
    }

    @Override
    public boolean validate(final InputStream inputStream) throws LicenseException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        try {
            final Document doc = dbf.newDocumentBuilder().parse(inputStream);
            return validateSignature(doc, this.checkCertificateExpiration);

        } catch (final FileNotFoundException e) {
            throw new LicenseException("");
        } catch (final SAXException e) {
            throw new LicenseException("");
        } catch (final IOException e) {
            throw new LicenseException("");
        } catch (final ParserConfigurationException e) {
            throw new LicenseException("");
        }
    }
}
