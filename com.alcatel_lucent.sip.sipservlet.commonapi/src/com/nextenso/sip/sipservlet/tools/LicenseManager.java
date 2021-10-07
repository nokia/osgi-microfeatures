package com.nextenso.sip.sipservlet.tools;
/**SecurityManager dedicated to the License control. */
public interface LicenseManager {
    static public org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LicenseManager.class);
	/**
	 *check the object is granted to be loaded
	 *
	 * @param  o  Description of the Parameter
	 */
	void checkLoad(LicenseableObject o);


	/**
	 *check the object is granted to be performed
	 *
	 * @param  o  Description of the Parameter
	 */
	void checkPerform(LicenseableObject o);


	void release(LicenseableObject o);
}

