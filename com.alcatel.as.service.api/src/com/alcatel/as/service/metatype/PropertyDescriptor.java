package com.alcatel.as.service.metatype;

import java.util.Map;

/**
 * Accessor interface for the descriptor of a single configuration property
 */
public interface PropertyDescriptor {

	/** Attribute key for property meta data */
	public static final String NAME = "name";
	/** Attribute key for property meta data */
	public static final String VALUE = "value";
	/** Attribute key for property meta data */
	public static final String TYPE = "type";
	/** Attribute key for property meta data */
	public static final String SECTION = "section";
	/** Attribute key for property meta data */
	public static final String TITLE = "title";
	/** Attribute key for property meta data */
	public static final String LEVEL = "level";
	/** Attribute key for property meta data */
	public static final String DYNAMIC = "dynamic";
	/** Attribute key for property meta data */
	public static final String HELP = "help";
	/** Attribute key for property meta data */
	public static final String MIN = "min";
	/** Attribute key for property meta data */
	public static final String MAX = "max";
	/** Attribute key for property meta data */
	public static final String VALIDATION = "validation";
	/** Attribute key for property meta data */
	public static final String SCOPE = "scope";
	/** Possible attribute value for key TYPE */
	public static final String FILEDATA = "filedata";
	/** Possible attribute value for key TYPE */
	public static final String MSELECT = "mselect";
	/**
	 * Attribute key for property meta data. Only available if TYPE is FILEDATA.
	 */
	public static final String FILENAME = "filename";
	/** Attribute key for property meta data (initial default value) */
	public static final String DEFAULT_VALUE = "defaultValue";
	/**
	 * Attribute key for property meta data. The hash is computed based on
	 * attributes of the property. It may be used to compare 2 versions of a
	 * PropertyDescriptor during an update.
	 */
	public static final String HASH = "hash";

	/** generic accessor for meta information on this property descriptor */
	String getAttribute(String key);

	/** generic accessor for meta information on this property descriptor */
	Map<String, Object> getAttributes();

	/** returns the PID this PropertyDescriptor belongs to 
	String getPid();*/


	/** returns the name of this property */
	String getName();

	/** returns the value of this property */
	String getValue();

	/** sets the value of this property */

	void setValue(String s);

	/** returns true if this property can be dynamically updated */
	boolean isDynamic();
	
	/** returns true if this the initial default value been modified by admin */
	boolean isModified();


	/**
	 * returns a copy of this property for use at runtime by a component instance.
	 * 
	 * @param p the platform name
	 * @param g the group name
	 * @param c the component name
	 * @param i the instance name
	 */
	InstanceProperty instantiate(String p, String g, String c, String i);
}
