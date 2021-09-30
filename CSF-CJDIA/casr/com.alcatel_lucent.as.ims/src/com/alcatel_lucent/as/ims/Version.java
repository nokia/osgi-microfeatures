package com.alcatel_lucent.as.ims;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The 3GPP TS document version.
 * 
 * A Version contains two parts, a major part and a minor part. It is
 * represented by "&lt;major&gt;.&lt;minor&gt;".
 */
public class Version
		implements Comparable<Version>, Externalizable {

	/**
	 * The UnsupportedVersionException exception.
	 * 
	 * Used when the version is not supported by the client..
	 */
	public static class UnsupportedVersionException
			extends Exception {

		/**
		 * <code>serialVersionUID</code>
		 */
		private static final long serialVersionUID = 1L;

	}

	private int _major = 1;
	private int _minor = 0;

	public Version() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param major The major part of the version.
	 * @param minor The minor part of the version.
	 */
	public Version(int major, int minor) {
		_major = major;
		_minor = minor;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Version version) {
		if (version == null) {
			return 1;
		}
		if (getMajor() > version.getMajor()) {
			return 1;
		}

		if (getMajor() < version.getMajor()) {
			return -1;
		}

		return getMinor() - version.getMinor();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Version) {
			Version v = (Version) obj;
			if (v.getMajor() == getMajor() && v.getMinor() == getMinor()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getMajor() * 1000 + getMinor();
	}

	/**
	 * Gets the major part of the version.
	 * 
	 * @return The major part of the version.
	 */
	public int getMajor() {
		return _major;
	}

	/**
	 * Gets the minor part of the version.
	 * 
	 * @return The minor part of the version.
	 */
	public int getMinor() {
		return _minor;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[Version " + getMajor() + "." + getMinor() + "]";
	}

	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		out.writeInt(getMajor());
		out.writeInt(getMinor());
	}

	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		_major = in.readInt();
		_minor = in.readInt();
	}

}
