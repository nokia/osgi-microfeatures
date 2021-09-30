/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.utils;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


public class NMSLicenseValidator extends BaseLicenseValidator {

	private static final String ADDITIONAL_INFO_ATTR_NAME = "additionalInfo";
	private static final String ALGORITHM_ATTR = "Algorithm";
	private static final String NMS_ADDITIONAL_INFO_REGEX = "originOMC=[0-9]+, ?pool=[A-Z][0-9]+\\.XML";
	private static final String NONE_ALGORITHM = "http://none";
	private static final String SHA1_ALGORITHM = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
	private static final String SIGNATURE_METHOD_TAG = "ds:SignatureMethod";
	private static final String SIGNED_INFO_TAG = "ds:SignedInfo";
	private static final String SUPPLEMENTARY_INFO_TAG = "supplementaryInfo";

	@Override
	public boolean validate(final InputStream inputStream) throws LicenseException {

		final Document doc = getDOMDocument(inputStream);
		System.out.println(" ############################# START VALIDATING !!!!!!!!!!!!!!");
		System.out.println("############################# IS NMS License= "+isNMSLicence(doc));
		if (isNMSLicence(doc)) {
			try {
				boolean res = this.validateSHA1DigestOnly(doc);
				System.out.println("############################# NMS signature valid = "+res);
				return res;
			} catch (final XMLSignatureException ex) {
				throw new LicenseException("NMS digest validation failed", ex);
			}
		} else {
			boolean res = this.validateSignature(doc, false);
			System.out.println("############################# IS signature valid = "+res);
			return res;
		}
	}

	private boolean isNMSLicence(final Document doc) throws LicenseException {
		final NodeList nl = doc.getElementsByTagName(SUPPLEMENTARY_INFO_TAG);
		if (nl.getLength() > 0) {
			final NamedNodeMap nnmap = nl.item(0).getAttributes();
			if (nnmap != null) {
				final Node attr = nnmap.getNamedItem(ADDITIONAL_INFO_ATTR_NAME);
				if (attr != null) {
					final String val = attr.getTextContent();
					if (val.matches(NMS_ADDITIONAL_INFO_REGEX)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Document getDOMDocument(final InputStream inputStream) throws LicenseException {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		dbf.setNamespaceAware(true);

		Document doc;

		try {
			doc = dbf.newDocumentBuilder().parse(inputStream);
		} catch (final SAXException e) {
			throw new LicenseException("");
		} catch (final IOException e) {
			throw new LicenseException("");
		} catch (final ParserConfigurationException e) {
			throw new LicenseException("");
		}
		return doc;
	}

	private boolean validateSHA1DigestOnly(final Document doc) throws XMLSignatureException, LicenseException {

		// Find signature node in document
		final NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

		if (nl.getLength() == 0) {
			throw new XMLSignatureException("Cannot find Signature element");
		}

		final Node signatureNode = nl.item(0);

		setDigestAlgorithm(signatureNode);

		final XMLSignature signature = getXMLSignature(signatureNode);

		// Create a DOMValidateContext and specify a KeySelector
		// and document context.
		final DOMValidateContext xvc = new DOMValidateContext(new X509KeySelector(), signatureNode);
		xvc.setIdAttributeNS((Element) doc.getElementsByTagName("LicenceData").item(0), null, "Id");

		for (final Object reference : signature.getSignedInfo().getReferences()) {
			if (!((Reference) reference).validate(xvc)) {
				LicenseLogger.getInstance().error(getClass().getName(), "validateSHA1DigestOnly",
						"Licence file verifying failed. Digest value error.");
				return false;
			}
		}

		return true;
	}

	// Create signature factory to unmarshal a signature node
	private XMLSignature getXMLSignature(final Node signatureNode) throws LicenseException, XMLSignatureException {
		try {
			final XMLSignatureFactory fac = getSignatureFactory();
			final XMLStructure xmlStructure = new DOMStructure(signatureNode);
			final XMLSignature signature = fac.unmarshalXMLSignature(xmlStructure);
			return signature;
		} catch (final MarshalException e) {
			throw new XMLSignatureException();
		}

	}

	// Correct signature structure
	// For backward compatibility of OSS5.x NMSes (i.e. reference validation,
	// where digest value is checked below, is not working without this conversion.)
	private void setDigestAlgorithm(final Node signatureNode) {
		NodeList nl;

		nl = signatureNode.getChildNodes();

		for (int i = 0; i < nl.getLength(); i++) {
			final Node node = nl.item(i);

			if ((node.getNodeType() == Node.ELEMENT_NODE) && (node.getNodeName() == SIGNED_INFO_TAG)) {
				final NodeList children = node.getChildNodes();

				for (int j = 0; j < children.getLength(); j++) {
					final Node child = children.item(j);

					if ((child.getNodeType() == Node.ELEMENT_NODE) && (child.getNodeName() == SIGNATURE_METHOD_TAG)) {
						final NamedNodeMap attributes = child.getAttributes();
						final Node algorithm = attributes.getNamedItem(ALGORITHM_ATTR);

						if ((algorithm != null) && algorithm.getTextContent().contentEquals(NONE_ALGORITHM)) {
							algorithm.setTextContent(SHA1_ALGORITHM);
						}
					}
				}
			}
		}
	}
}
