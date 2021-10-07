package com.nokia.licensing.storages.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.utils.Constants;


public class LicenseCancelDataStorageFileImpl implements LicenseCancelDataStorage {
	CLJLPreferences cljlPreferences;
	Preferences prefSystemRoot;

	public LicenseCancelDataStorageFileImpl() throws LicenseException {
		this.cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
		this.prefSystemRoot = this.cljlPreferences.getPreferencesSystemRoot();
	}

	private List<String> getCancelInfoFileNames(final String status) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoFileNames",
				"Index file search for the filenames with status 'Cancelled'");

		final String xpath = "//*[status='" + status + "']";
		final List<String> cancelInfoFilenames = getFileNames(xpath);

		return cancelInfoFilenames;
	}

	private List<String> getCancelInfoFileNamesFrmSerialNoAndStatus(final String serialNbr, final String status) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoFileNamesFrmSerialNoAndStatus",
				"Index file search for filenames for given serial number and status  'Cancelled'");

		// find out the proper xpath from Ajay
		final String xpath = "//*[status='" + status + "' and serialNumber='" + serialNbr + "']";
		final List<String> fileNames = getFileNames(xpath);

		return fileNames;
	}

	@Override
	public List<LicenseCancelInfo> getAllCancelInfos(final boolean checkDataIntegrity) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos",
				"Getting all the Cancel Info Files from the CancelInfo directory");

		List<LicenseCancelInfo> cancelInfoList;
		List<String> cancelInfoFileNameList;
		final String cancLicFolderLoc = this.prefSystemRoot.node("directory").get(Constants.CANCEL_LICENSES, null);
		final String status = "Cancelled";

		cancelInfoList = new ArrayList<LicenseCancelInfo>();

		Iterator<String> iterator = null;
		FileInputStream cancelLicFileInStream = null;
		ObjectInputStream canceLicObjInStream = null;
		LicenseCancelInfo cancelInfoObj = null;

		try {
			cancelInfoFileNameList = getCancelInfoFileNames(status);
			iterator = cancelInfoFileNameList.iterator();

			while (iterator.hasNext()) {
				final String cancelInfofileName = iterator.next();

				cancelLicFileInStream = new FileInputStream(cancLicFolderLoc + cancelInfofileName);
				canceLicObjInStream = new ObjectInputStream(cancelLicFileInStream);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos",
						"Reading all the Cancel Info Files from the CancelInfo directory");
				cancelInfoObj = (LicenseCancelInfo) canceLicObjInStream.readObject();
				cancelInfoList.add(cancelInfoObj);
				cancelLicFileInStream.close();
				canceLicObjInStream.close();
			}
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
					"An I/O exception has been thrown" + ioExpObj.getMessage());
		} catch (final ClassNotFoundException cnfExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
					"Class not found exception has been thrown" + cnfExpObj.getMessage());
		}

		return cancelInfoList;
	}

	@Override
	public List<LicenseCancelInfo> getCancelInfoBySerialNumber(final String serialNumber,
			final boolean checkDataIntegrity) throws LicenseException {
		List<LicenseCancelInfo> cancelInfoList;
		List<String> cancelInfoFileNameList;
		final String status = "Cancelled";
		final String cancLicFolderLoc = this.prefSystemRoot.node("directory").get(Constants.CANCEL_LICENSES, null);

		cancelInfoList = new ArrayList<LicenseCancelInfo>();

		Iterator<String> iterator = null;
		FileInputStream cancelLicFileInStream = null;
		ObjectInputStream canceLicObjInStream = null;
		LicenseCancelInfo cancelInfoObj = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber",
				"Getting the Cancel Info Files from the CancelInfo directory By Serial Number");

		try {
			cancelInfoFileNameList = getCancelInfoFileNamesFrmSerialNoAndStatus(serialNumber, status);
			iterator = cancelInfoFileNameList.iterator();

			while (iterator.hasNext()) {
				final String cancelInfofileName = iterator.next();

				cancelLicFileInStream = new FileInputStream(cancLicFolderLoc + cancelInfofileName);
				canceLicObjInStream = new ObjectInputStream(cancelLicFileInStream);
				cancelInfoObj = (LicenseCancelInfo) canceLicObjInStream.readObject();
				cancelInfoList.add(cancelInfoObj);
				cancelLicFileInStream.close();
				canceLicObjInStream.close();
			}
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"An I/O exception has been thrown" + ioExpObj.getMessage());
		} catch (final ClassNotFoundException cnfExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"Class not found exception has been thrown" + cnfExpObj.getMessage());
		}

		return cancelInfoList;
	}

	@Override
	public List<LicenseCancelInfo> getCanceledLicense(final Date startTime, final Date endTime)
			throws LicenseException {
		return null;
	}

	@Override
	public List<StoredLicense> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
		return null;
	}

	@Override
	public void insertCancelInformation(final LicenseCancelInfo cancelInfo) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
				"Moving the Active license and inserting cancel info object to cancelled directory");

		final String indexFilePath = this.prefSystemRoot.node("directory").get(Constants.INDEX_FILE_PATH, null);
		final String cancLicFolderLoc = this.prefSystemRoot.node("directory").get(Constants.CANCEL_LICENSES, null);

		// String successLicenseFolderLocation= prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES, null);
		final IndexFileElement originalObj = new IndexFileElement(Integer.toString(cancelInfo.getId()),
				cancelInfo.getSerialNbr(), cancelInfo.getLicenseFileName(), "active",
				Long.toString(cancelInfo.getFeaturecode()));

		originalObj.setStatus("Cancelled");

		final IndexFileElement changedObj = originalObj;

		new IndexFileHandler(indexFilePath).changeElementInFile(originalObj, changedObj);

		String cancelInfoFilename;

		try {
			cancelInfoFilename = cancelInfo.getLicenseFileName();

			final File cancelLicenseFolder = new File(cancLicFolderLoc);

			if (!cancelLicenseFolder.exists()) {
				cancelLicenseFolder.mkdir();
			}

			/*
			 * // File (or directory) to be moved File file = new File(successLicenseFolderLocation+cancel_Filename); //
			 * Destination directory File dir = new File(cancLicFolderLoc);
			 *
			 * // Move file to new directory boolean success = file.renameTo(new File(dir, file.getName())); if
			 * (!success) { LicenseLogger.getInstance().info(this.getClass().getName(), "insertCancelInformation",
			 * "Active License File was not sucessfully moved to the Cancelled directory" ); }
			 */
			FileOutputStream fileOutputStream;

			fileOutputStream = new FileOutputStream(cancLicFolderLoc + cancelInfoFilename);

			final ObjectOutputStream objOutputStream = new ObjectOutputStream(fileOutputStream);

			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
					"Writing Cancel Info Object into the Cancelled directory");
			objOutputStream.writeObject(cancelInfo);
			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
					"Cancel info object is stored in the Cancelled directory");
			objOutputStream.close();
			fileOutputStream.close();
		} catch (final FileNotFoundException fileNFEExp) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"File not found" + fileNFEExp.getMessage());
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"An I/O exception has been thrown" + ioExpObj.getMessage());
		}
	}

	/*
	 * private List<String> getLicenseFileNameFrmSerialNo(String serialNbr) {
	 * LicenseLogger.getInstance().info(this.getClass().getName(), "getLicenseFileNameFrmSerialNo",
	 * "Index file search for filenames for given serial number" ); String xpath="//*[serialNumber='"+serialNbr+"']";
	 * List<String> fileNames=getFileNames(xpath); return fileNames; }
	 */
	private List<String> getFileNames(final String xpath) {
		List<String> filenames = null;
		String fileName;

		try {
			LicenseLogger.getInstance().finest(this.getClass().getName(), "getFileNames",
					"Parsing the License Index File with XPATH expression " + xpath);
			filenames = new ArrayList<String>();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final String indexFileLocation = this.prefSystemRoot.node("directory").get(Constants.INDEX_FILE_PATH, null);
			final File indexFile = new File(indexFileLocation);

			if (indexFile.exists()) {
				final Document dom = db.parse(indexFile);
				final XPath theXPath = XPathFactory.newInstance().newXPath();
				final NodeList nodelist = (NodeList) theXPath.evaluate(xpath, dom, XPathConstants.NODESET);

				for (int nodeListIterator = 0; nodeListIterator < nodelist.getLength(); nodeListIterator++) {

					// Get element
					final Element elem = (Element) nodelist.item(nodeListIterator);

					fileName = elem.getElementsByTagName("fileName").item(0).getFirstChild().getNodeValue();
					LicenseLogger.getInstance().finest(this.getClass().getName(), "getFileNames", fileName + " is selected.");
					filenames.add(fileName);
				}
			}
		} catch (final ParserConfigurationException parserConExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"Failed to configure the parser" + parserConExpObj.getMessage());
		} catch (final SAXException saxExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"Failed to parse the file" + saxExpObj.getMessage());
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"An I/O exception has been thrown" + ioExpObj.getMessage());
		} catch (final XPathExpressionException transExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"Transformer exception has been thrown" + transExpObj.getMessage());
		}

		return filenames;
	}
}
