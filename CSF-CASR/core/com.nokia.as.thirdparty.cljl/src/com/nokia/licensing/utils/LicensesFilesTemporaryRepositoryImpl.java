package com.nokia.licensing.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


public class LicensesFilesTemporaryRepositoryImpl extends LicensesFilesRepositoryImpl {

    private static LicensesFilesTemporaryRepositoryImpl instance;

    private final static String tempRepositoryDir = "in_progress/";

    protected LicensesFilesTemporaryRepositoryImpl() {
        super();
        final String tempRepositoryPath = this.repositoryDir + tempRepositoryDir;
        this.repositoryDir = this.pref.node("directory").get(Constants.TEMP_REPOSITORY_PATH, tempRepositoryPath);
        createRepositoryDirectory();
    }

    public static LicensesFilesTemporaryRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new LicensesFilesTemporaryRepositoryImpl();
        }
        return instance;
    }

    public void moveLicenseToRepository(final LicensesFilesRepository repository, final String licenseFileName)
            throws LicenseException {
        try {
            final FileInputStream stream = new FileInputStream(this.repositoryDir + licenseFileName);
            repository.copyLicenseIntoFileRepo(stream, licenseFileName);
            deleteLicenseFromFileRepo(licenseFileName);
        } catch (final FileNotFoundException e) {
            final LicenseException licenseException = new LicenseException(
                    e.getMessage() + "The license file was not created properly.");
            licenseException.setErrorCode("CLJL221");
            LicenseLogger.getInstance().error(LicensesFilesRepositoryImpl.class.getName(), "moveLicenseToRepository",
                    "error code set to: " + licenseException.getErrorCode(), e);
        }
    }

    protected void createRepositoryDirectory() {
        final File dir = new File(this.repositoryDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

}
