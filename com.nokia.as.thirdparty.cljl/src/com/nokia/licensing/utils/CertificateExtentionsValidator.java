// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.X509Extension;

import com.nokia.licensing.interfaces.LicenseException;


/**
 * Class was extracted from BaseLicenseValidator - currently functionality of checking certificateExtensions is not used
 *
 */

@Deprecated
public abstract class CertificateExtentionsValidator {

    private static final int INT_0X10 = 0x10;
    private static final int INT_0XFF = 0xFF;
    private static final int INT_1024 = 1024;
    private static final String ANY_POLICY = "2.5.29.32.0";
    private static final String ASK_RESPONDER = "1.3.6.1.4.1.94.1.49.7.7.7";
    private static final String EXPECTED_POLICY = "1.3.6.1.4.1.94.1.49.5.2.1";
    private static final String[] KEY_USAGE_NAME_ARR = {
            "Digital Signature", "Non Repudiation", "Key Encipherment", "Data Encipherment", "Key Agreement",
            "Key Cert Sign", "CRL Sign", "Encipher Only", "Decipher Only", };

    private static final String SIGNATURE_ERROR = "CLJL101";

    private static final Map<ASN1ObjectIdentifier, String> DER_OBJECT_IDS = new HashMap<ASN1ObjectIdentifier, String>();

    static {
        DER_OBJECT_IDS.put(BCStyle.C, "C");
        DER_OBJECT_IDS.put(BCStyle.CN, "CN");
        DER_OBJECT_IDS.put(BCStyle.O, "O");
    }

    protected Boolean checkCertificateExtentions(final X509Certificate cert) throws LicenseException {
        String errorMessage = null;

        try {
            final Set<String> criticalExtensions = new HashSet<String>(cert.getCriticalExtensionOIDs());
            final Set<String> nonCriticalExtensions = new HashSet<String>(cert.getNonCriticalExtensionOIDs());

            final Set<String>[] extensionsArr = new Set[2];
            extensionsArr[0] = criticalExtensions;
            extensionsArr[1] = nonCriticalExtensions;

            boolean isCertificatePolicies = false;
            boolean isAnyPolicy = false;
            boolean isCriticalPolicy = false;

            for (int i = 0; i < extensionsArr.length; i++) {
                for (final String extensionOid : extensionsArr[i]) {
                    final boolean isCritical = i == 0;
                    final DEROctetString extnValue = (DEROctetString) new ASN1InputStream(
                            new ByteArrayInputStream(cert.getExtensionValue(extensionOid))).readObject();
                    final ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(extnValue.getOctets()));
                    final ASN1Primitive extObj = aIn.readObject();
                    final StringBuilder extensionName = new StringBuilder();
                    final StringBuilder extensionValue = new StringBuilder();
                    if (extensionOid.equals(X509Extension.basicConstraints.getId())) {

                        checkX509ExtensionsBasicConstrains(isCritical, extObj, extensionName, extensionValue);

                    } else if (extensionOid.equals(X509Extension.subjectKeyIdentifier.getId())) {

                        checkX509ExtensionsSubjectKeyIdentifier(isCritical, extObj, extensionName, extensionValue);

                    } else if (extensionOid.equals(X509Extension.authorityKeyIdentifier.getId())) {

                        checkX509ExtensionsAuthorityKeyIdentifier(isCritical, extObj, extensionName, extensionValue);

                    } else if (extensionOid.equals(X509Extension.certificatePolicies.getId())) {

                        isCertificatePolicies = true;
                        final DERObjectIdentifier id = checkX509ExtensionsCertificatePolicies(isCritical, extObj,
                                extensionName, extensionValue);
                        if (isCritical) {
                            isCriticalPolicy = id.getId().equals(EXPECTED_POLICY);
                        }
                        if (!isCritical) {
                            isAnyPolicy = id.getId().equals(ANY_POLICY);
                        }
                    } else if (extensionOid.equals(X509Extension.keyUsage.getId())) {
                        checkX509ExtensionsKeyUsage(isCritical, cert, extensionName, extensionValue);
                    } else {
                        if (isCritical) {
                            throw new LicenseException(SIGNATURE_ERROR, "err.sign.validation.certificate.extension");
                        }
                        // Other Extension IOD
                    }
                }
            } // for

            ////////////////////////////////////////////////////////
            // Accept the policy extension if either of below
            // is valid: extension is non-critical and OID
            // is any_policy, extension is critical and
            // OID is EXPECTED_POLICY
            ////////////////////////////////////////////////////////
            if (isCertificatePolicies && !(isAnyPolicy || isCriticalPolicy)) {
                errorMessage = "Certificate chain does not contain the expected policy OID";
                throw new LicenseException(SIGNATURE_ERROR, "err.sign.validation.certificate.policy");
            }
        } catch (final IOException e) {
            errorMessage = "Certificate parsing error. " + e.getMessage();
        } catch (final CertificateParsingException e) {
            errorMessage = "Certificate parsing error. " + e.getMessage();
        }
        if (errorMessage != null) {
            throw new LicenseException(SIGNATURE_ERROR, errorMessage);
        }
        return true;
    }

    private DERObjectIdentifier checkX509ExtensionsCertificatePolicies(final boolean isCritical, final ASN1Primitive extObj,
            final StringBuilder extensionName, final StringBuilder extensionValue) {
        extensionName.append("X509v3 Certificate Policies:").append(isCritical ? " critical" : "");
        final ASN1Sequence policy = (ASN1Sequence) ((ASN1Sequence) extObj).getObjectAt(0);
        final DERObjectIdentifier id = (DERObjectIdentifier) policy.getObjectAt(0);
        extensionValue.append("\tPolicy: ");
        if (id.getId().equals(ANY_POLICY)) {
            // Policy: X509v3 Any Policy
            extensionValue.append("X509v3 Any Policy");
        } else {
            extensionValue.append(id.getId());
        }
        return id;
    }

    private void checkX509ExtensionsKeyUsage(final boolean isCritical, final X509Certificate cert,
            final StringBuilder extensionName,
            final StringBuilder extensionValue) throws CertificateParsingException, LicenseException {
        extensionName.append("X509v3 Key Usage:").append(isCritical ? " critical" : "");
        extensionValue.append("\t");
        for (int j = 0; j < cert.getKeyUsage().length; j++) {
            if (cert.getKeyUsage()[j]) {
                extensionValue.append(KEY_USAGE_NAME_ARR[j]).append(':');
            }
        }
        /////////////////////////////////////////////////////
        // Check for extended key usage critical extension.
        // If present, then OID should be ASK_RESPONDER
        // Otherwise throw unhandled critical extension error.
        /////////////////////////////////////////////////////
        if (cert.getExtendedKeyUsage() != null && isCritical) {
            if (!cert.getSigAlgOID().equals(ASK_RESPONDER)) {
                final String errorMessage = "Extended Key Usage critical extension found without ASK RESPONDER OID";
                throw new LicenseException(SIGNATURE_ERROR, errorMessage);
            }
            ///////////////////////////////////////////////////
            // 3. Trigger the Nokia Responder
            // by sending a Challenge.
            ///////////////////////////////////////////////////
        }
    }

    private void checkX509ExtensionsAuthorityKeyIdentifier(final boolean isCritical, final ASN1Primitive extObj,
            final StringBuilder extensionName, final StringBuilder extensionValue) {
        String extensionHexValue;
        extensionName.append("X509v3 Authority Key Identifier:").append(isCritical ? " critical" : "");
        final ASN1Sequence authority = (ASN1Sequence) extObj;
        final Enumeration<ASN1Primitive> e1 = authority.getObjects();
        while (e1.hasMoreElements()) {
            final DERTaggedObject taggedObject = (DERTaggedObject) e1.nextElement();
            if (taggedObject.getTagNo() == 0) {
                extensionHexValue = getExtensionHexValue(taggedObject.getObject());
                extensionValue.append("\tkeyid:").append(extensionHexValue);

            } else if (taggedObject.getTagNo() == 1) {
                final DERTaggedObject taggedSetObject = (DERTaggedObject) taggedObject.getObject();
                final ASN1Sequence dirName = (ASN1Sequence) taggedSetObject.getObject();
                extensionValue.append("\tDirName:");
                final Enumeration<ASN1Primitive> e2 = dirName.getObjects();
                while (e2.hasMoreElements()) {
                    final DERSet setObject = (DERSet) e2.nextElement();
                    final ASN1Sequence dirSubName = (ASN1Sequence) setObject.getObjectAt(0);
                    final DERObjectIdentifier id = (DERObjectIdentifier) dirSubName.getObjectAt(0);
                    final ASN1String string = (ASN1String) dirSubName.getObjectAt(1);

                    extensionValue.append(DER_OBJECT_IDS.get(id)).append("=");
                    extensionValue.append(string.getString()).append(" ");
                }

            } else if (taggedObject.getTagNo() == 2) {
                extensionHexValue = getExtensionHexValue(taggedObject.getObject());
                extensionValue.append("\tserial:").append(extensionHexValue);
            }
        }
    }

    private void checkX509ExtensionsSubjectKeyIdentifier(final boolean isCritical, final ASN1Primitive extObj,
            final StringBuilder extensionName, final StringBuilder extensionValue) {
        extensionName.append("X509v3 Subject Key Identifier:").append(isCritical ? " critical" : "");
        final String extensionHexValue = getExtensionHexValue(extObj);
        extensionValue.append("\t").append(extensionHexValue);
    }

    private void checkX509ExtensionsBasicConstrains(final boolean isCritical, final ASN1Primitive extObj,
            final StringBuilder extensionName,
            final StringBuilder extensionValue) {
        extensionName.append("X509v3 Basic Constraints:").append(isCritical ? " critical" : "");
        final ASN1Sequence basic = (ASN1Sequence) extObj;
        final ASN1Boolean bool = basic.getObjects().hasMoreElements() ? (ASN1Boolean) basic.getObjectAt(0)
                : ASN1Boolean.getInstance(false);
        extensionValue.append("\tCA:").append((bool.isTrue()) ? "TRUE" : "FALSE");
    }

    private String getExtensionHexValue(final ASN1Primitive extensionObject) {
        final StringBuilder sb = new StringBuilder();
        for (final byte hexByte : ((ASN1OctetString) extensionObject).getOctets()) {
            final int hexInt = hexByte & INT_0XFF;
            if (hexInt < INT_0X10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(hexInt)).append(':');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString().toUpperCase();
    }
}
