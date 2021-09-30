/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.nokia.licensing.logging.LicenseLogger;


/**
 * this class moves file from one folder to another folder
 *
 * @author hajeri
 *
 */
public class MoveFile {
	public void movefile(final String src, final String dest) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "movefile", "moving file");

		final File inputFile = new File(src);
		final File destFile = new File(dest);
		final File gg = new File(destFile, inputFile.getName());

		try {
			final boolean success = inputFile.renameTo(gg);

			LicenseLogger.getInstance().finest(this.getClass().getName(), "movefile", "status" + success);
		} catch (final SecurityException ex) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "movefile", "Error during moving file " + ex.getMessage());
		}
	}

	public void copyFile(final File sourceFile, final File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}

			if (destination != null) {
				destination.close();
			}
		}
	}
}
