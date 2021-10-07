package com.nextenso.sip.sipservlet.tools;
/**
 *Capacity for an object to be licensed. A LicenseableObject can be checked by the LicenseManager.
 */
public interface LicenseableObject {
	final static Integer UNLICENSED = new Integer(Integer.MAX_VALUE);


	/**
	 * @return    an optional counter for license manager to control
	 */
	long getCounter();


	/**
	 * @return    the licenseid of the object
	 */
	Integer getID();


	String getDomain();


	void setID(Integer id);

}

