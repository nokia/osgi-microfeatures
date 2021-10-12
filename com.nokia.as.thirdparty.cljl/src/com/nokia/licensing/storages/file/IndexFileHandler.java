// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.storages.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.logging.LicenseLogger;


/**
 * @author ajnn
 *
 */
public class IndexFileHandler {

	private static final String PRETTY_PRINT = "format-pretty-print";

	Document documentObj;
	Field[] fields;

	// No generics
	List<IndexFileElement> licenseData;
	File licenseFilePointer;

	public IndexFileHandler(final String absoluteFilePath) {

		// create a list to hold the data
		this.licenseData = new ArrayList<IndexFileElement>();
		this.licenseFilePointer = new File(absoluteFilePath);

		// Get a DOM object
		createDocument();
		this.fields = IndexFileElement.class.getDeclaredFields();
	}

	/**
	 * @return the licenseData
	 */
	public List<IndexFileElement> getLicenseData() {
		return this.licenseData;
	}

	/**
	 * Add a list of books to the list In a production system you might populate the list from a DB
	 *
	 * @param licenseFileList
	 */
	public void loadData(final List<StoredLicense> licenseFileList) {

		// StoredLicense licenseFile;
		IndexFileElement indexFileEleObj;
		final Iterator<StoredLicense> listIterator = licenseFileList.iterator();

		while (listIterator.hasNext()) {
			final StoredLicense storedLicObj = listIterator.next();
			final List<FeatureInfo> featureInfoList = storedLicObj.getFeatureInfoList();
			final Iterator<FeatureInfo> featureInfoListIterator = featureInfoList.iterator();

			while (featureInfoListIterator.hasNext()) {
				final String featureCode = Long.toString(featureInfoListIterator.next().getFeatureCode());

				indexFileEleObj = new IndexFileElement(storedLicObj.getCustomerId(), storedLicObj.getSerialNbr(),
						storedLicObj.getLicenseFileName(), "active", featureCode);
				this.licenseData.add(indexFileEleObj);
			}
		}
	}

	/**
	 * void
	 */
	private void getPreviousData() {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document dom = db.parse(this.licenseFilePointer);
			final XPath theXPath = XPathFactory.newInstance().newXPath();
			final NodeList nodelist = (NodeList) theXPath.evaluate("*/*", dom, XPathConstants.NODESET);

			for (int i = 0; i < nodelist.getLength(); i++) {

				// Get element
				final Element elem = (Element) nodelist.item(i);
				final String custID = elem.getElementsByTagName("customerID").item(0).getFirstChild().getNodeValue();
				final String serialNo = elem.getElementsByTagName("serialNumber").item(0).getFirstChild()
						.getNodeValue();
				final String fileName = elem.getElementsByTagName("fileName").item(0).getFirstChild().getNodeValue();
				final String featureCode = elem.getAttribute("FeatureCode");
				final String status = elem.getElementsByTagName("status").item(0).getFirstChild().getNodeValue();

				this.licenseData.add(new IndexFileElement(custID, serialNo, fileName, status, featureCode));
			}
		} catch (final ParserConfigurationException parserConExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getPreviousData",
					"Failed to configure the parser" + parserConExpObj.getMessage());
		} catch (final SAXException saxExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getPreviousData",
					"Failed to parse the file" + saxExpObj.getMessage());
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getPreviousData",
					"An I/O exception has been thrown" + ioExpObj.getMessage());
		} catch (final XPathExpressionException transExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getPreviousData",
					"Transformer exception has been thrown" + transExpObj.getMessage());
		}
	}

	/**
	 * Using JAXP in implementation independent manner create a document object using which we create a xml tree in
	 * memory
	 */
	private void createDocument() {

		// get an instance of factory
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			// get an instance of builder
			final DocumentBuilder db = dbf.newDocumentBuilder();

			// create an instance of DOM
			this.documentObj = db.newDocument();
		} catch (final ParserConfigurationException parserConExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "createIndexFileElement",
					"Failed to configure the parser" + parserConExpObj.getMessage());
		}
	}

	/**
	 * The real workhorse which creates the XML structure
	 */
	private void createDOMTree() {

		// create the root element <Books>
		final Element rootEle = this.documentObj.createElement("LicenseFiles");

		this.documentObj.appendChild(rootEle);

		// No enhanced for
		final Iterator<IndexFileElement> it = this.licenseData.iterator();

		while (it.hasNext()) {
			final IndexFileElement licenseFile = it.next();

			// For each IndexFileElement object create <IndexFileElement> element and attach it to root
			final Element licenseEle = createIndexFileElement(licenseFile);

			rootEle.appendChild(licenseEle);
		}
	}

	/**
	 * Helper method which creates a XML element <IndexFileElement>
	 *
	 * @param b
	 *            The book for which we need to create an xml representation
	 * @return XML element snippet representing a book
	 */
	private Element createIndexFileElement(final IndexFileElement indexFileElement) {
		final Element licenseFileEle = this.documentObj.createElement("LicenseFile");

		licenseFileEle.setAttribute("FeatureCode", indexFileElement.getFeatureCode());

		try {
			for (int fieldsIterator = 0; fieldsIterator < this.fields.length; fieldsIterator++) {
				if (!this.fields[fieldsIterator].equals(indexFileElement.getClass().getDeclaredField("featureCode"))) {
					final Element elementName = this.documentObj.createElement(this.fields[fieldsIterator].getName());

					this.fields[fieldsIterator].setAccessible(true);

					final Text elementValue = this.documentObj
							.createTextNode(this.fields[fieldsIterator].get(indexFileElement).toString());

					this.fields[fieldsIterator].setAccessible(false);
					elementName.appendChild(elementValue);
					licenseFileEle.appendChild(elementName);
				}
			}
		} catch (final SecurityException secExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "createIndexFileElement",
					"SecurityException is thrown" + secExpObj.getMessage());
		} catch (final DOMException domExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "createIndexFileElement",
					"IllegalArgumentException is thrown" + domExpObj.getMessage());
		} catch (final IllegalArgumentException illegalArgExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "createIndexFileElement",
					"IllegalArgumentException is thrown" + illegalArgExpObj.getMessage());
		} catch (final NoSuchFieldException noSuchFieldExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "createIndexFileElement",
					"NoSuchFieldException is thrown" + noSuchFieldExpObj.getMessage());
		} catch (final IllegalAccessException illegalAccExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "createIndexFileElement",
					"IllegalAccessException is thrown" + illegalAccExpObj.getMessage());
		}

		return licenseFileEle;
	}

	/**
	 * This method uses Xerces specific classes prints the XML document to file.
	 */
	private void printToFile() {
		try {
			final DOMImplementationLS implementation = (DOMImplementationLS) this.documentObj.getImplementation();
			final LSSerializer lsSerializer = implementation.createLSSerializer();
			if (lsSerializer.getDomConfig().canSetParameter(PRETTY_PRINT, Boolean.TRUE)) {
				lsSerializer.getDomConfig().setParameter(PRETTY_PRINT, Boolean.TRUE);
			}
			final LSOutput output = implementation.createLSOutput();
			final FileOutputStream fos = new FileOutputStream(this.licenseFilePointer, false);
			output.setByteStream(fos);
			lsSerializer.write(this.documentObj, output);
			fos.flush();
			fos.close();
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "printToFile",
					"IO exeption is thrown" + ioExpObj.getMessage());
		}
	}

	public void createFile() {
		createDOMTree();
		printToFile();
	}

	public void addElementsInFile(final List<StoredLicense> elements) {
		getPreviousData();
		loadData(elements);
		createFile();
	}

	public void changeElementInFile(IndexFileElement originalObj, final IndexFileElement changedObj) {
		this.getPreviousData();

		final int index = this.licenseData.indexOf(originalObj);

		originalObj = this.licenseData.get(index);
		changedObj.setCustomerID(originalObj.getCustomerID());
		this.licenseData.remove(originalObj);
		this.licenseData.add(index, changedObj);
		this.createFile();
	}
}
