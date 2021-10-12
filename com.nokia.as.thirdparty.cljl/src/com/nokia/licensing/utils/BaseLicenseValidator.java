// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.TargetPlatformCapabilities;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;


/**
 *
 * @author twozniak
 */
public abstract class BaseLicenseValidator {

	private static final String CHECK_CERTIFICATE_CHAIN = "checkCertificateChain";

	private static final String XML_DIG_SIG_CLASS = "org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI";

	private static final List<SignatureFactoryConfig> SIGN_FACTORY_CONFIG_LIST = new LinkedList<SignatureFactoryConfig>();

	private static final String SIGNATURE_ERROR = "CLJL101";

	static {
		SIGN_FACTORY_CONFIG_LIST.add(new SignatureFactoryConfig("DOM", XML_DIG_SIG_CLASS));
	}

	private static String NOKIA_ROOT_CERTIFICATE = "MIIDNjCCAh6gAwIBAgIEOlWp2TANBgkqhkiG9w0BAQUFADApMQ4wDAYDVQQKEwVO"
			+ "b2tpYTEXMBUGA1UEAxMOTm9raWEgIFJvb3QgQ0EwHhcNMDEwMTA1MTEwNTA1WhcN"
			+ "MTYwMTAyMTEwNTA1WjApMQ4wDAYDVQQKEwVOb2tpYTEXMBUGA1UEAxMOTm9raWEg"
			+ "IFJvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCytPLyAuav"
			+ "gti+nSSFVRUfDtqxx2X4KOv7IjkyVnon9TpCy+SKq2GCqrys9wtXQ/X3ZN8GvbtW"
			+ "AoR4Jpl7SJxLWMLaykz/BG/ePZbF2QrFv2QMm24I0lpEGq+Lm8ZMcHXmeYSYq4vl"
			+ "/eKsDF4eo+OBLj3qhZy53MVPV7tetz+BYYVh3vb7/KQgVeNY4xm/pCnEPzHWEs/K"
			+ "HZhVdGT1Ts3um+ZFA1fb2DhIIUljYEEhrkk+XAgXw1z0h49oL8T/Yzi0Yy2I45mW"
			+ "b30Sgvs3SElm0VK9TB0IG53++IfgOCwoaM2Q0qLqkAdz0hpcHq0jKRyhin4v6h7/"
			+ "f5y4QloKPUQrAgMBAAGjZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQQwHQYDVR0OBBYE"
			+ "FI2N95+hDQAhMu+p7SKu4Dv00H2wMB8GA1UdIwQYMBaAFI2N95+hDQAhMu+p7SKu"
			+ "4Dv00H2wMA4GA1UdDwEB/wQEAwIBhjANBgkqhkiG9w0BAQUFAAOCAQEAgYQ+qXZM"
			+ "AvIqL+HUn4yHleiUC1imlxI4sy0GSgDtmi8YEM2+XRVtmzwACiMmqRShYEH4voMr"
			+ "KX+MqE18py6tZEZHKPumdvGsouwjeCZY1aTKjZvrTAnB71+o8PllDLegSIyK32XQ"
			+ "FrqKX+TYapiilQI0xruZ95sTYt4HLkKFgyIKL3tRBIm8B3MRjKVMnDwDBzoCIzZV"
			+ "7k35fGyYqA1//YD/kgncRln7AVGaQjjTrYCg1TfTauoC3QcfzyPOhQXgB9gdaXoe"
			+ "liEZcVtxfjiscg3I+YUFHEtggUnplZSv+Zekv97quoW01Y6RXpSgk0Ps5VmRsuHs" + "XX6VypdFTxM8Ew==";

	/**
	 * Base64 encoded certificate
	 */
	private static String TEST_NOKIA_ROOT_CERTIFICATE = "MIIDhzCCAm+gAwIBAgIBADANBgkqhkiG9w0BAQQFADA6MQswCQYDVQQGEwJGSTEO"
			+ "MAwGA1UEChMFTm9raWExGzAZBgNVBAMTElRFU1QgTm9raWEgUm9vdCBDQTAeFw0w"
			+ "NDA5MTcxMzE1NTFaFw0xOTA5MTQxMzE1NTFaMDoxCzAJBgNVBAYTAkZJMQ4wDAYD"
			+ "VQQKEwVOb2tpYTEbMBkGA1UEAxMSVEVTVCBOb2tpYSBSb290IENBMIIBIjANBgkq"
			+ "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAunPQTY4fbB/4T5c5v03uNFMRJcdNQp1s"
			+ "yLRWMBUkonwG4byF9S8Ja3aPgvmd9E0LT5wL1XxahpaayzWSN2/TTLNM0ZP3zWSX"
			+ "CYDHiCRuOBa+5VA+6eQ5S/vxkvX4Nb0xHJ0iX4vVvL7MO0v+CACCC20FlUIkdpgv"
			+ "VNyYHu2M5Smud9te1q/TanEjxNANMhLDayQadIgyzq65597m4Pjqq0eSGD/X0szq"
			+ "uIuO2yAv+m9+ZIKPwJgVmg4ZM+nUHUQyya78/wo3FBL2NQkZWpn0RQT33jftzeAa"
			+ "gdrTCPdfAYx/ilmoEN2Ygsu5KFh6cnDy9a0PI8J38F6CnfRdr2XpRwIDAQABo4GX"
			+ "MIGUMB0GA1UdDgQWBBQsoh13yWL10i9GRIvdbGoXYLGc4DBiBgNVHSMEWzBZgBQs"
			+ "oh13yWL10i9GRIvdbGoXYLGc4KE+pDwwOjELMAkGA1UEBhMCRkkxDjAMBgNVBAoT"
			+ "BU5va2lhMRswGQYDVQQDExJURVNUIE5va2lhIFJvb3QgQ0GCAQAwDwYDVR0TAQH/"
			+ "BAUwAwEB/zANBgkqhkiG9w0BAQQFAAOCAQEAYjs3iZMAnrzJTVwJX28JO4F5YkiU"
			+ "I0PsJGruZwqrHZWWciox+hRvso+k494O039pAW8DOmUq37HRrbWBMlxsnAO7wmPx"
			+ "q3dKdsFxh2cqx1xyFiT2nMeQHtWQcgxPr1vjrO1OEhZHGnD/vJgVf76O/z8erHaq"
			+ "7upe2jI/fggpEVnnylT2bf5x3MhRbxkmLuIDVkQd0RYjvfAqFVCZMXqZnr8rajwt"
			+ "XeXnHSWzs7zXeapJ+TlieujTArN5jBf3F2zNini+6LaXQStzFJDjWbs1Qxlhbg6T"
			+ "XeBm5Nk+KveGPgZ4aSKRf0d6GDo1eB4qgyuHwyc0/bZkhvoDcp2Lk0Hbmw==";

	private static String NSN_ROOT_CERTIFICATE = "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";


	private static String ASLM_ROOT_CERTIFICATE = "MIID7DCCAtSgAwIBAgIJANo00vkvmlKfMA0GCSqGSIb3DQEBCwUAMIGCMQswCQYD" +
			"VQQGEwJGUjEOMAwGA1UECAwFTm96YXkxDjAMBgNVBAcMBU5vemF5MQ4wDAYDVQQK" +
			"DAVOb2tpYTEOMAwGA1UECwwFTm9raWExDTALBgNVBAMMBFJPT1QxJDAiBgkqhkiG" +
			"9w0BCQEWFWFsZXhpcy5qdWFuQG5va2lhLmxvbDAeFw0xOTA1MTAxNTIzNTRaFw0z" +
			"OTA1MDUxNTIzNTRaMIGCMQswCQYDVQQGEwJGUjEOMAwGA1UECAwFTm96YXkxDjAM" +
			"BgNVBAcMBU5vemF5MQ4wDAYDVQQKDAVOb2tpYTEOMAwGA1UECwwFTm9raWExDTAL" +
			"BgNVBAMMBFJPT1QxJDAiBgkqhkiG9w0BCQEWFWFsZXhpcy5qdWFuQG5va2lhLmxv" +
			"bDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMF40AH+jdoFWDnNm6jq" +
			"A3HGZ33iCV421qp2C7GLdrNrcQubinKTa8axGAZc29ddrOJQ38HH1LTRA3FsRO0q" +
			"D80ouWgl/pf2kCbJiPDR12n9QL2hVwLV4RSdXKhL+qQVHYPKEIxh/dJA5zofOSQq" +
			"U5Fzdo2masnV7bd+R1cNvCPt5AL5kHYHjpo+jkW9vb7RpeCj/HNUCYoBRDUyZd5X" +
			"LuPk6MXPA7zIlEctxCOgVxDctxU6qP6pFa9TRGskYYp9rdF0K7zDAKq2aHjlEv50" +
			"HKIsOiWGWcqixLX5Q7XOzjALXIxg+zlwZK0yrx+ZLTdoEiKUjVqKzBMwK5IFu3gQ" +
			"uzcCAwEAAaNjMGEwHQYDVR0OBBYEFLvimXt6/jsfqLXl0NiP0BFSbo+pMB8GA1Ud" +
			"IwQYMBaAFLvimXt6/jsfqLXl0NiP0BFSbo+pMA8GA1UdEwEB/wQFMAMBAf8wDgYD" +
			"VR0PAQH/BAQDAgGGMA0GCSqGSIb3DQEBCwUAA4IBAQB6Wuqn1b3bOpFwicbbr5fT" +
			"IAT4pK4Vk5gZyLQOeAFPY0UNeO007g9MDi/AOfhpJ0/CIVxuY/9h/f/essNp8LbA" +
			"YoxtQYEgo+ek6MiZJRRRAbPDAYfmz9pSL5bYjl+7ml6eXqEPa650gcQ5HICTuO9o" +
			"PLmbcI8SipS6Gf80axRKkr1uqtCMKSg3Wj9Z4EhXkWI2p2UpaKEnixzp89s+cpX/" +
			"ErC+FwkXHhC30U+tnjVeKw4Znb+Y43zakKMsEQFO4/jru7loJ0XKomozDRMbhklW" +
			"5G3J4WOF4W5h3cs9I1OevX71VVCIw/iFVCpnoj8AcKo4NwRy5+OrBei0ybssjyXV";

/*
"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4QV87PrAY+yfFV/5bqJF" +
			"auf42j2t4f7RZ8Vbv43k9GCqXtb9Ckft2iCHfhoowNpFcF6MDmX7USoVt1Ln9tgc" +
			"9vB3sUG+q3aV1FGAcK8ILy0arG/xGOMFQ9nja+1DVKXNZcaa6nLG0b7y64U4xXxO" +
			"EbGvRXBl573GHQLMpI6iccqmzXZQBJwYmH1YhXV7DUrpX3M2JKmsrFpsOtSULc1g" +
			"pxLFVuZKzMJV0CY4Z6R/TPsz9iSWmWgBRYl09JwSchbTyp6r+juVjBpWsHEfXUH4" +
			"16jhN/tyRGjVGeluxPNwMVOGe+EyB9jYCkRyrOPImN1ePzwAK8Iw+fU5WWXZoTht" +
			"rQIDAQAB";
 */
	private final SignatureFactory signatureFactory;

	public BaseLicenseValidator() {
		this.signatureFactory = new SignatureFactoryImpl();
	}

	public abstract boolean validate(InputStream inputStream) throws LicenseException;

	public boolean validateSignature(final Document xmlDoc, final boolean checkCertificateExpiration)
			throws LicenseException {
		final String sourceMethod = "validateSignature";

		final NodeList signatureNodeList = xmlDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

		if (signatureNodeList.getLength() != 1) {
			throw new LicenseException(SIGNATURE_ERROR, "Signature node not found.");
		}
		final Node signatureNode = signatureNodeList.item(0);
		final XMLStructure xmlStructure = new DOMStructure(signatureNode);
		try {
			final XMLSignatureFactory xmlSignFac = getSignatureFactory();
			xmlSignFac.getProvider().getName();
			final XMLSignature signature = xmlSignFac.unmarshalXMLSignature(xmlStructure);

			// Signature contains certificates (from NOLS)
			if (signature.getKeyInfo() != null) {
				final X509Data data = (X509Data) signature.getKeyInfo().getContent().get(0);
				final X509IssuerSerial issuerSerial = (X509IssuerSerial) data.getContent().get(0);

				final X509Certificate certGen = (X509Certificate) data.getContent().get(1);

				final X509Certificate certNet = (X509Certificate) data.getContent().get(2);

				final X509Certificate trustedX509 = getTrustedCertificate(issuerSerial);

				final List<X509Certificate> certs = new ArrayList<X509Certificate>();
				certs.add(trustedX509);
				certs.add(certNet);
				certs.add(certGen);

				checkCertificateChain(checkCertificateExpiration, trustedX509, certs);

				if (validateXml(xmlDoc, signatureNode, xmlSignFac, certGen)) {
					return true;
				} else {
					return false;
				}
			}

			return false;

		} catch (final XMLSignatureException e) {
			LicenseLogger.getInstance().error(BaseLicenseValidator.class.getName(), sourceMethod,
					"Verifying XML file error.", e);
			throw new LicenseException(SIGNATURE_ERROR, "Verifying XML file error. " + e.getMessage());
		} catch (final MarshalException e) {
			LicenseLogger.getInstance().error(BaseLicenseValidator.class.getName(), sourceMethod,
					"Signature unmarshal error. ", e);
			throw new LicenseException(SIGNATURE_ERROR, "Signature unmarshal error. " + e.getMessage());
		} catch (final IllegalArgumentException e) {
			LicenseLogger.getInstance().error(BaseLicenseValidator.class.getName(), sourceMethod, "Signature error. ",
					e);
			throw new LicenseException(SIGNATURE_ERROR, "Signature error. " + e.getMessage());
		}
	}

	private boolean validateXml(final Document xmlDoc, final Node signatureNode, final XMLSignatureFactory xmlSignFac,
			final X509Certificate certGen) throws MarshalException, XMLSignatureException {

		final DOMValidateContext validateContext = new DOMValidateContext(certGen.getPublicKey(), signatureNode);
		validateContext.setIdAttributeNS((Element) xmlDoc.getElementsByTagName("LicenceData").item(0), null, "Id");
		final XMLSignature signatureWithValidateContext = xmlSignFac.unmarshalXMLSignature(validateContext);

		if (signatureWithValidateContext.validate(validateContext)) {
			LicenseLogger.getInstance().finest(getClass().getName(), "validateXml",
					"Licence file signature verifying passed.");
			return true;
		} else {
			logReasoneFailedValidation(validateContext, signatureWithValidateContext);
			return false;
		}
	}

	private void logReasoneFailedValidation(final DOMValidateContext validateContext,
			final XMLSignature signatureWithValidateContext) throws XMLSignatureException {
		if (!signatureWithValidateContext.getSignatureValue().validate(validateContext)) {
			LicenseLogger.getInstance().error(getClass().getName(), "logReasoneFailedValidation",
					"Licence file verify failed. Signature value error.");
		}
		for (final Object reference : signatureWithValidateContext.getSignedInfo().getReferences()) {
			if (!((Reference) reference).validate(validateContext)) {
				LicenseLogger.getInstance().error(getClass().getName(), "logReasoneFailedValidation",
						"Licence file verifying failed. Reference value error.");
			}
		}
	}

	private X509Certificate getTrustedCertificate(final X509IssuerSerial issuerSerial) throws LicenseException {
		X509Certificate trustedX509;
		if (isTestCertifiate(issuerSerial)) {
			trustedX509 = loadX509Certificate(TEST_NOKIA_ROOT_CERTIFICATE);
		} else if(issuerSerial.getIssuerName().contains("LICENSESIGNER")){
			trustedX509 = loadX509Certificate(ASLM_ROOT_CERTIFICATE);
		}
		else {
			trustedX509 = issuerSerial.getIssuerName().contains("Nokia Siemens Networks")
					? loadX509Certificate(NSN_ROOT_CERTIFICATE) : loadX509Certificate(NOKIA_ROOT_CERTIFICATE);
		}

		return trustedX509;
	}

	private boolean isTestCertifiate(final X509IssuerSerial issuerSerial) throws LicenseException {
		boolean isTestCert = false;
		if (issuerSerial.getIssuerName().indexOf("TEST") > 0) {
			isTestCert = true;
		}

		if (isTestCert) {
			final TargetPlatformCapabilities tpc = PluginRegistry.getRegistry()
					.getPlugin(TargetPlatformCapabilities.class);
			if ((tpc == null) || tpc.isProductionPlatform()) {
				throw new LicenseException("CLJL128", "Testing licences are not supported.");
			}
		}
		return isTestCert;
	}

	private void checkCertificateChain(final boolean checkCertificateExpiration, final X509Certificate trustedX509,
			final List<X509Certificate> certs) throws LicenseException {
		String errorMessage = null;
		X509Certificate parentCertificate = trustedX509;
		for (final X509Certificate cert : certs) {
			final String certName = cert.getSubjectDN().getName();
			final String signatureError = SIGNATURE_ERROR;
			try {
				cert.verify(parentCertificate.getPublicKey());
				cert.checkValidity();
			} catch (final CertificateExpiredException e) {
				if (checkCertificateExpiration) {
					errorMessage = "Certificate with subject " + certName + " has expired.";
				} else {
					LicenseLogger.getInstance().fine(getClass().getName(), "validate", "Certificate with subject "
							+ certName + " has expired. " + "LKF still valid in light PKI verification level");
				}
			} catch (final CertificateNotYetValidException e) {
				errorMessage = "Certificate with subject " + certName + " is not yet valid.";
				logErrorCheckCertificateChain(errorMessage, e);
			} catch (final InvalidKeyException e) {
				errorMessage = "Certificate with subject " + certName + " has incorrect key.";
				logErrorCheckCertificateChain(errorMessage, e);
			} catch (final CertificateException e) {
				errorMessage = "Certificate with subject " + certName + " has encoding errors.";
				logErrorCheckCertificateChain(errorMessage, e);
			} catch (final NoSuchAlgorithmException e) {
				errorMessage = "Certificate with subject " + certName + " has unsupported signature algorithms.";
				logErrorCheckCertificateChain(errorMessage, e);
			} catch (final NoSuchProviderException e) {
				errorMessage = "Certificate with subject " + certName + " has no default provider.";
				logErrorCheckCertificateChain(errorMessage, e);
			} catch (final SignatureException e) {
				errorMessage = "Certificate with subject " + certName + " has signature errors.";
				logErrorCheckCertificateChain(errorMessage, e);
			}
			if (errorMessage != null) {
				LicenseLogger.getInstance().error(getClass().getName(), CHECK_CERTIFICATE_CHAIN, errorMessage);
				throw new LicenseException(signatureError, errorMessage);
			}
			parentCertificate = cert;
		}
	}

	private void logErrorCheckCertificateChain(final String errorMessage, final Exception e) {
		LicenseLogger.getInstance().error(this.getClass().getName(), CHECK_CERTIFICATE_CHAIN, errorMessage, e);
	}

	/**
	 *
	 * @return
	 * @throws LicenseException
	 */
	protected XMLSignatureFactory getSignatureFactory() throws LicenseException {
		final String sourceMethod = "getSignatureFactory";
		XMLSignatureFactory xmlSignFac = null;
		for (final SignatureFactoryConfig config : SIGN_FACTORY_CONFIG_LIST) {
			try {
				xmlSignFac = this.signatureFactory.getSignatureFactory(config.getType(), config.getProvider());
				break;
			} catch (final NoSuchProviderException e) {
				LicenseLogger.getInstance().error(getClass().getName(), sourceMethod,
						"Could not initialize SignatureFactory:" + e.getMessage());
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(getClass().getName(), sourceMethod,
						"Could not initialize SignatureFactory:" + e.getMessage());
			}
		}
		if (xmlSignFac == null) {
			throw new LicenseException("Could not initialize signature factory");
		}
		return xmlSignFac;
	}

	private X509Certificate loadX509Certificate(final String certificate) throws LicenseException {
		CertificateFactory cf = null;
		X509Certificate cert = null;

		try {
			// the lines containing --- are stripped off
			final byte[] certificateBytes = Base64.decodeBase64(certificate.getBytes());

			final InputStream is = new ByteArrayInputStream(certificateBytes);

			cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(is);

		} catch (final CertificateException e) {
			// errorMessage = "Error loading certifacate data: " +e.getMessage();
			throw new LicenseException(SIGNATURE_ERROR, "err.sign.validation.certificate.load");
		}
		return cert;
	}

	public static class X509KeySelector extends KeySelector {

		@Override
		public KeySelectorResult select(final KeyInfo keyInfo, final KeySelector.Purpose purpose,
				final AlgorithmMethod method, final XMLCryptoContext context) throws KeySelectorException {

			final Iterator ki = keyInfo.getContent().iterator();

			while (ki.hasNext()) {
				final XMLStructure xmlStructure = (XMLStructure) ki.next();

				if (!(xmlStructure instanceof X509Data)) {
					continue;
				}

				final X509Data x509Data = (X509Data) xmlStructure;
				final List content = x509Data.getContent();

				for (final Object o : content) {
					if (!(o instanceof X509Certificate)) {
						continue;
					}

					final PublicKey key = ((X509Certificate) o).getPublicKey();
					final boolean algEquals = algEquals(method.getAlgorithm(), key.getAlgorithm());

					// Make sure the algorithm is compatible
					// with the method.
					if (algEquals) {
						return new KeySelectorResult() {

							@Override
							public Key getKey() {
								return key;
							}
						};
					}
				}
			}

			throw new KeySelectorException("No key found!");
		}

		boolean algEquals(final String algURI, final String algName) {
			if ((algName.equalsIgnoreCase("RSA") && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1))) {
				return true;
			} else {
				return false;
			}
		}
	}

	private static class SignatureFactoryConfig {

		private final String type;
		private final String provider;

		public SignatureFactoryConfig(final String type, final String provider) {
			this.type = type;
			this.provider = provider;
		}

		public String getType() {
			return this.type;
		}

		public String getProvider() {
			return this.provider;
		}
	}
}
