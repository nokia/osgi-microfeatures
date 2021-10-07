package com.nokia.licensing.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.prefs.Preferences;

import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;


public class LicensesFilesRepositoryImpl implements LicensesFilesRepository {

    Preferences pref;
    String repositoryDir;
    private static LicensesFilesRepositoryImpl instance;

    protected LicensesFilesRepositoryImpl() {
        this.pref = getPrefSystemRoot();
        this.repositoryDir = this.pref.node("directory").get(Constants.REPOSITORY_PATH,
                "/var/opt/oss/global/license/LIC-File_Repo/");
    }

    public static LicensesFilesRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new LicensesFilesRepositoryImpl();
        }
        return instance;
    }

    private void checkRepoDir() throws LicenseException {
        if (!new File(this.repositoryDir).exists()) {
            final LicenseException licenseException = new LicenseException(
                    "The license cannot be installed. Directory \""
                            + this.repositoryDir + "\" does not exists. Please create neccessary directory"
                            + " and provide proper directory permission.");

            licenseException.setErrorCode("CLJL107");
            LicenseLogger.getInstance().error(LicensesFilesRepositoryImpl.class.getName(), "createFile",
                    "error code set to: " + licenseException.getErrorCode());

            throw licenseException;
        }
    }

    /**
     * Get the preference system root.
     * 
     * @return preferences
     */
    protected Preferences getPrefSystemRoot() {
        try {
            final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
            this.pref = cljlPreferences.getPreferencesSystemRoot();
        } catch (final LicenseException e) {
        }
        return this.pref;
    }

    @Override
    public String getRepositoryDir() {
        return this.repositoryDir;
    }

    /**
     * Delete license file from license-repository.
     * 
     * @param licenseFilePath
     */
    @Override
    public void deleteLicenseFromFileRepo(final String licenseFileName) throws LicenseException {
        checkRepoDir();
        final File licenseFileToBeDeleted = new File(this.repositoryDir + licenseFileName);
        if (licenseFileToBeDeleted.exists()) {
            licenseFileToBeDeleted.delete();
        }
    }

    /**
     * Creating a file from InputStream
     * 
     * @param licenseFileStream
     * @param destPath
     * @param licenseFilePath
     * @return
     * @throws LicenseException
     */
    @Override
    public File copyLicenseIntoFileRepo(final InputStream licenseFileStream, final String licenseFileName)
            throws LicenseException {
        File destntnFile = null;
        final String sourceMethod = "copyLicenseIntoFileRepo";
        try {
            checkRepoDir();
            destntnFile = new File(this.repositoryDir + licenseFileName);
            final OutputStream out = new FileOutputStream(destntnFile);
            final byte buf[] = new byte[4096];
            int len;
            while ((len = licenseFileStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            licenseFileStream.close();
        } catch (final IOException e) {
            final LicenseException licenseException = new LicenseException(
                    e.getMessage() + "The license file was not created properly.");
            licenseException.setErrorCode("CLJL106");
            LicenseLogger.getInstance().error(LicensesFilesRepositoryImpl.class.getName(), sourceMethod,
                    "error code set to: " + licenseException.getErrorCode(), e);
            throw licenseException;
        }
        LicenseLogger.getInstance().fine(LicensesFilesRepositoryImpl.class.getName(), sourceMethod,
                "License file is created under LIC-Repo.");
        return destntnFile;
    }

    @Override
    public StoredLicense readStoredLicense(final String licenseFileName) throws LicenseException {
        final LicenseXMLParser parser = new LicenseXMLParser();
        StoredLicense storedLicense = null;
        InputStream parsingInputStream = null;
        final String sourceMethod = "readStoredLicense";
        try {
            parsingInputStream = new FileInputStream(this.repositoryDir + licenseFileName);
            storedLicense = parser.parse(parsingInputStream);
            storedLicense.setLicenseFileImportUser(System.getProperty("user.name"));
            storedLicense.setLicenseFilePath(this.repositoryDir + licenseFileName);
            storedLicense.setLicenseFileName(licenseFileName);
        } catch (final IOException ioe) {
            final LicenseException ex = new LicenseException(" IO Exception.", ioe);
            ex.setErrorCode("CLJL116");
            LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod, "error code set to: " + ex.getErrorCode(),
                    ioe);
            throw ex;
        } catch (final SAXException saxex) {
            final LicenseException ex = new LicenseException(" SAX Exception.", saxex);
            ex.setErrorCode("CLJL118");
            LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod, "error code set to: " + ex.getErrorCode(),
                    saxex);
            throw ex;
        } catch (final DOMException domexe) {
            final LicenseException ex = new LicenseException(" DOM Exception.", domexe);
            ex.setErrorCode("CLJL117");
            LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod, "error code set to: " + ex.getErrorCode(),
                    domexe);
            throw ex;
        } finally {
            if (parsingInputStream != null) {
                closeInputStream(parsingInputStream);
            }
        }
        return storedLicense;
    }

    /**
     * This method validates the Digital Signature of the stored license.
     * 
     * @param storedLicense
     *            to validate
     * @param checkCertificateExpiration
     *            if true verify certificate expiration date (PKI C+/C++)
     */
    @Override
    public boolean isValid(final StoredLicense storedLicense, final boolean deleteInvalid,
            final boolean checkCertificateExpiration)
                    throws LicenseException {
        // Digital Signature Validation
        boolean status = false;
        InputStream validatingInputStream = null;
        final String sourceMethod = "isValid";
        try {
            validatingInputStream = new FileInputStream(this.repositoryDir + storedLicense.getLicenseFileName());
            if (storedLicense.isNmsGenerated()) {
                LicenseLogger.getInstance().fine(this.getClass().getName(), sourceMethod, "NMS generated License");
                final NMSLicenseValidator nmsLicenseValidator = new NMSLicenseValidator();
                try {
                    status = nmsLicenseValidator.validate(validatingInputStream);
                } catch (final LicenseException e) {
                    LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod, "NMS validation error", e);
                }
            } else {
                LicenseLogger.getInstance().fine(this.getClass().getName(), sourceMethod, "Application License");
                final LicenseValidator licenseValidator = new LicenseValidator(checkCertificateExpiration);
                try {
                    status = licenseValidator.validate(validatingInputStream);
                } catch (final LicenseException e) {
                    throw e;
                }
            }
        } catch (final IOException ioe) {
            // Log the information into log files
            final LicenseException ex = new LicenseException(" IO Exception.");
            ex.setErrorCode("CLJL116");
            LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod, "error code set to: " + ex.getErrorCode(), ex);
            throw ex;
        } catch (final DOMException domexe) {
            // Log the information into log files
            final LicenseException ex = new LicenseException(" DOM Exception.");
            ex.setErrorCode("CLJL117");
            LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod, "error code set to: " + ex.getErrorCode(),
                    domexe);
            throw ex;
        } finally {
            if (!status && deleteInvalid) {
                deleteLicenseFromFileRepo(storedLicense.getLicenseFileName());
            }
            closeInputStream(validatingInputStream);
        }

        return status;
    }

    private void closeInputStream(final InputStream inputStream) {
        final String sourceMethod = "closeInputStream";
        try {
            inputStream.close();
        } catch (final IOException ioe) {
            LicenseLogger.getInstance().error(getClass().getName(), sourceMethod, "", ioe);
        }
        ;
    }
}
