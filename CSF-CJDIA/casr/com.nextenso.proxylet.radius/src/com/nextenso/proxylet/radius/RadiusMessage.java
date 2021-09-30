package com.nextenso.proxylet.radius;

import java.util.Enumeration;
import com.nextenso.proxylet.ProxyletData;

/**
 * This interface encapsulates all the methods common to all radius messages
 * (accounting requests and responses, access requests and responses).
 * <p/>
 * The main features are: client identification, server identification and
 * attributes storing.
 */
public interface RadiusMessage
		extends ProxyletData {

	/**
	 * Gets the message code.<br/>
	 * The message code is the first byte of the radius message.
	 * 
	 * @return The code.
	 */
	public int getCode();

	/**
	 * Gets the message identifier. <br/>
	 * The message identifier is the second byte of the radius message.
	 * 
	 * @return The identifier.
	 */
	public int getIdentifier();

	/**
	 * Gets the server IP Address.
	 * 
	 * @return The server IP Address if known, <code>null</code> otherwise
	 */
	public String getServerAddr();

	/**
	 * Gets the server host name. <br/>
	 * The Agent may return the IP Address if it does not choose to resolve it.
	 * 
	 * @return The server host name or IP Address if known, <code>null</code>
	 *         otherwise.
	 */
	public String getServerHost();

	/**
	 * Gets the server port.
	 * 
	 * @return The server port if known, -1 otherwise.
	 */
	public int getServerPort();

	/**
	 * Gets the secret shared with the server.
	 * 
	 * @return The secret if known, null otherwise.
	 */
	public byte[] getServerSecret();

	/**
	 * Gets the client IP Address.
	 * 
	 * @return The client IP Address.
	 */
	public String getClientAddr();

	/**
	 * Gets the client host name. <br/>
	 * The Agent may return the IP Address if it does not choose to resolve it.
	 * 
	 * @return The client host name or IP Address.
	 */
	public String getClientHost();

	/**
	 * Gets the client port.
	 * 
	 * @return The client port if known, -1 otherwise.
	 */
	public int getClientPort();

	/**
	 * Gets the secret shared with the client.
	 * 
	 * @return The secret.
	 */
	public byte[] getProxySecret();

	/**
	 * Gets the attributes.
	 * 
	 * @return An enumeration of all the attributes stored in the message.
	 */
	public Enumeration getRadiusAttributes();

	/**
	 * Gets the number of attributes.
	 * 
	 * @return The number of attributes.
	 */
	public int getRadiusAttributesSize();

	/**
	 * Gets the attribute of the given type. <br/>
	 * This method does not work for VendorSpecificAttributes (use
	 * <code>getVendorSpecificAttribute(int vendorId)</code> instead).
	 * 
	 * @param type The attribute type.
	 * @return The attribute, or <code>null</code> if no attribute of the given
	 *         type is stored.
	 */
	public RadiusAttribute getRadiusAttribute(int type);

	/**
	 * Removes the attribute of the given type. <br/>
	 * This method does not work for VendorSpecificAttributes (use
	 * <code>removeVendorSpecificAttribute(int vendorId)</code> instead).
	 * 
	 * @param type The attribute type.
	 * @return The removed attribute, or <code>null</code> if no attribute of the
	 *         given type was stored.
	 */
	public RadiusAttribute removeRadiusAttribute(int type);

	/**
	 * Adds an attribute.
	 * The attribute can be of any type (regular, vendor-specific, extended, extended-vendor-specific).
	 * 
	 * @param attribute The attribute to add.
	 */
	public void addRadiusAttribute(RadiusAttribute attribute);

	/**
	 * Gets the VendorSpecificAttribute of the given vendorId.
	 * 
	 * @param vendorId The vendorId.
	 * @return The VendorSpecificAttribute, or <code>null</code> if no
	 *         VendorSpecificAttribute of the given vendorId is stored.
	 */
	public VendorSpecificAttribute getVendorSpecificAttribute(int vendorId);

	/**
	 * Removes the VendorSpecificAttribute of the given vendorId.
	 * 
	 * @param vendorId The vendorId.
	 * @return The removed VendorSpecificAttribute, or <code>null</code> if no
	 *         VendorSpecificAttribute of the given vendorId was stored.
	 */
	public VendorSpecificAttribute removeVendorSpecificAttribute(int vendorId);

	/**
	 * Adds a Vendor-Specific attribute.
	 * addRadiusAttribute may be called instead.
	 * 
	 * @param attribute The Vendor-Specific attribute to add.
	 */
	public void addVendorSpecificAttribute(VendorSpecificAttribute attribute);

	/**
	 * Gets the ExtendedVendorSpecificAttribute of the given type, vendorId and EVS Type.
	 * 
	 * @param type the type (see constants in ExtendedTypeAttribute)
	 * @param vendorId The vendorId.
	 * @param evsType the EVS Type.
	 * @return The ExtendedVendorSpecificAttribute, or <code>null</code> if no
	 *         matching ExtendedVendorSpecificAttribute was found.
	 */
	public ExtendedVendorSpecificAttribute getExtendedVendorSpecificAttribute(int type, int vendorId, int evsType);

	/**
	 * Removes the ExtendedVendorSpecificAttribute of the given type, vendorId and EVS Type.
	 * 
	 * @param type the type (see constants in ExtendedTypeAttribute)
	 * @param vendorId The vendorId.
	 * @param evsType the EVS Type.
	 * @return The removed VendorSpecificAttribute, or <code>null</code> if no
	 *         matching ExtendedVendorSpecificAttribute was found.
	 */
	public ExtendedVendorSpecificAttribute removeExtendedVendorSpecificAttribute(int type, int vendorId, int evsType);

	/**
	 * Gets the ExtendedTypeAttribute of the given type / extended type.
	 * 
	 * @param type the type (see constants in ExtendedTypeAttribute)
	 * @param extendedType the extended type
	 * @return The ExtendedTypeAttribute, or <code>null</code> if no
	 *         matching ExtendedTypeAttribute is stored
	 */
	public ExtendedTypeAttribute getExtendedTypeAttribute(int type, int extendedType);

	/**
	 * Removes the ExtendedTypeAttribute of the given type / extended type.
	 * 
	 * @param type the type (see constants in ExtendedTypeAttribute)
	 * @param extendedType the extended type
	 * @return The removed ExtendedTypeAttribute, or <code>null</code> if no
	 *         matching ExtendedTypeAttribute is stored
	 */
	public ExtendedTypeAttribute removeExtendedTypeAttribute(int type, int extendedType);

	/**
	 * Removes all the attributes.
	 */
	public void removeRadiusAttributes();

	/**
	 * Indicates whether the attribute is valid for this message.
	 * 
	 * @param attribute The atrribute.
	 * @return true if the attribute is valid for this message.
	 */
	public boolean isValidAttribute(RadiusAttribute attribute);

}
