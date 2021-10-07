package com.nextenso.proxylet.diameter;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nextenso.proxylet.diameter.dictionary.AbstractDiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.util.DiameterAVPFormat;

/**
 * This class is used to define a DiameterAVP.
 * <p/>
 * The definition contains:
 * <ul>
 * <li>a name (a String)
 * <li>a code (a long)
 * <li>a vendorId (a long)
 * <li>a 'V' flag policy (required, optional or forbidden).
 * <li>a 'M' flag policy (required, optional or forbidden).
 * <li>a 'P' flag policy (required, optional or forbidden).
 * <li>an encoding policy (required or not)
 * <li>a DiameterAVPFormat (to display values)
 * </ul>
 */
public class DiameterAVPDefinition
		implements Serializable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The value indicating that a flag is required.
	 */
	public static final int REQUIRED_FLAG = 1;
	/**
	 * The value indicating that a flag is optional.
	 */
	public static final int OPTIONAL_FLAG = 2;
	/**
	 * The value indicating that a flag is forbidden.
	 */
	public static final int FORBIDDEN_FLAG = -1;

	private int _vFlag, _mFlag, _pFlag;
	private long _code, _vendorId;
	private boolean _needEnc;
	private String _name;

	private DiameterAVPFormat _format;

	/**
	 * Constructs a new definition. <br/>
	 * The newly created DiameterAVPDefinition will be stored in the Dictionary.
	 * 
	 * @param name The name.
	 * @param code The code.
	 * @param vendorId The vendor identifier.
	 * @param vFlag The V flag policy (REQUIRED_FLAG, OPTIONAL_FLAG or
	 *          FORBIDDEN_FLAG).
	 * @param mFlag The M flag policy (REQUIRED_FLAG, OPTIONAL_FLAG or
	 *          FORBIDDEN_FLAG).
	 * @param pFlag The P flag policy (REQUIRED_FLAG, OPTIONAL_FLAG or
	 *          FORBIDDEN_FLAG).
	 * @param needEncryption The encoding policy (true to encrypt, false
	 *          otherwise).
	 * @param format The AVP format.
	 */
	public DiameterAVPDefinition(String name, long code, long vendorId, int vFlag, int mFlag, int pFlag, boolean needEncryption,
			DiameterAVPFormat format) {
		this(name, code, vendorId, vFlag, mFlag, pFlag, needEncryption, format, true);
	}

	/**
	 * Constructs a new definition.
	 * 
	 * @param name The name.
	 * @param code The code.
	 * @param vendorId The vendor identifier.
	 * @param vFlag The V flag policy (REQUIRED_FLAG, OPTIONAL_FLAG or
	 *          FORBIDDEN_FLAG).
	 * @param mFlag The M flag policy (REQUIRED_FLAG, OPTIONAL_FLAG or
	 *          FORBIDDEN_FLAG).
	 * @param pFlag The P flag policy (REQUIRED_FLAG, OPTIONAL_FLAG or
	 *          FORBIDDEN_FLAG).
	 * @param needEncryption The encoding policy (true to encrypt, false
	 *          otherwise).
	 * @param format The DiameterAVPFormat.
	 * @param registered Specifies if the newly created definition should be
	 *          registered in the Dictionary.
	 */
	public DiameterAVPDefinition(String name, long code, long vendorId, int vFlag, int mFlag, int pFlag, boolean needEncryption,
			DiameterAVPFormat format, boolean registered) {
		_name = name;
		_code = code;
		_vendorId = vendorId;
		_vFlag = vFlag;
		_mFlag = mFlag;
		_pFlag = pFlag;
		_needEnc = needEncryption;
		_format = format;
		if (registered) {
			Dictionary.registerDiameterAVPDefinition(this);
		}
	}

	/**
	 * Gets the display name.
	 * 
	 * @return The name.
	 */
	public String getAVPName() {
		return _name;
	}

	/**
	 * Gets the AVP code.
	 * 
	 * @return The code.
	 */
	public long getAVPCode() {
		return _code;
	}

	/**
	 * Gets the AVP vendor identifier.
	 * 
	 * @return The vendor identifier.
	 */
	public long getVendorId() {
		return _vendorId;
	}

	/**
	 * Gets the required flags.
	 * 
	 * @return The required flags - a combination of DiameterAVP.V_FLAG,
	 *         DiameterAVP.M_FLAG and DiameterAVP.P_FLAG.
	 */
	public int getRequiredFlags() {
		int flags = 0;
		if (vFlagRequired())
			flags |= 0x80;
		if (mFlagRequired())
			flags |= 0x40;
		if (pFlagRequired())
			flags |= 0x20;
		return flags;
	}

	public int getVFlag() {
		return _vFlag;
	}

	public int getMFlag() {
		return _mFlag;
	}

	public int getPFlag() {
		return _pFlag;
	}

	
	/**
	 * Specifies if encryption is needed.
	 * 
	 * @return true if needed, false otherwise.
	 */
	public boolean needsEncryption() {
		return _needEnc;
	}

	/**
	 * Gets the AVP format.
	 * 
	 * @return The format.
	 */
	public DiameterAVPFormat getDiameterAVPFormat() {
		return _format;
	}

	/**
	 * Specifies if the V flag is required.
	 * 
	 * @return true if required, false otherwise.
	 */
	public boolean vFlagRequired() {
		return (_vFlag == REQUIRED_FLAG);
	}

	/**
	 * Specifies if the V flag is optional.
	 * 
	 * @return true if optional, false otherwise.
	 */
	public boolean vFlagOptional() {
		return (_vFlag == OPTIONAL_FLAG);
	}

	/**
	 * Specifies if the V flag is forbidden.
	 * 
	 * @return true if required, false otherwise.
	 */
	public boolean vFlagForbidden() {
		return (_vFlag == FORBIDDEN_FLAG);
	}

	/**
	 * Specifies if the V flag is allowed (required or optional).
	 * 
	 * @return true if allowed, false otherwise.
	 */
	public boolean vFlagAllowed() {
		return (_vFlag > 0);
	}

	/**
	 * Specifies if the M flag is required.
	 * 
	 * @return true if required, false otherwise.
	 */
	public boolean mFlagRequired() {
		return (_mFlag == REQUIRED_FLAG);
	}

	/**
	 * Specifies if the M flag is optional.
	 * 
	 * @return true if optional, false otherwise.
	 */
	public boolean mFlagOptional() {
		return (_mFlag == OPTIONAL_FLAG);
	}

	/**
	 * Specifies if the M flag is forbidden.
	 * 
	 * @return true if required, false otherwise.
	 */
	public boolean mFlagForbidden() {
		return (_mFlag == FORBIDDEN_FLAG);
	}

	/**
	 * Specifies if the M flag is allowed (required or optional).
	 * 
	 * @return true if allowed, false otherwise.
	 */
	public boolean mFlagAllowed() {
		return (_mFlag > 0);
	}

	/**
	 * Specifies if the P flag is required.
	 * 
	 * @return true if required, false otherwise.
	 */
	public boolean pFlagRequired() {
		return (_pFlag == REQUIRED_FLAG);
	}

	/**
	 * Specifies if the P flag is optional.
	 * 
	 * @return true if optional, false otherwise.
	 */
	public boolean pFlagOptional() {
		return (_pFlag == OPTIONAL_FLAG);
	}

	/**
	 * Specifies if the P flag is forbidden.
	 * 
	 * @return true if required, false otherwise.
	 */
	public boolean pFlagForbidden() {
		return (_pFlag == FORBIDDEN_FLAG);
	}

	/**
	 * Specifies if the P flag is allowed (required or optional).
	 * 
	 * @return true if allowed, false otherwise.
	 */
	public boolean pFlagAllowed() {
		return (_pFlag > 0);
	}

	/**
	 * Checks if a set of flags complies to this definition.
	 * 
	 * @param flags The flags to check.
	 * @return true if the flags comply, false otherwise.
	 */
	public boolean checkFlags(int flags) {
		if ((flags & DiameterAVP.V_FLAG) == DiameterAVP.V_FLAG) {
			if (vFlagForbidden())
				return false;
		} else {
			if (vFlagRequired())
				return false;
		}
		if ((flags & DiameterAVP.M_FLAG) == DiameterAVP.M_FLAG) {
			if (mFlagForbidden())
				return false;
		} else {
			if (mFlagRequired())
				return false;
		}
		if ((flags & DiameterAVP.P_FLAG) == DiameterAVP.P_FLAG) {
			if (pFlagForbidden())
				return false;
		} else {
			if (pFlagRequired())
				return false;
		}
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("[DiameterAVPDefinition: ");
		res.append("name=").append(getAVPName());
		res.append(", code=").append(getAVPCode());
		res.append(", vendor-id=").append(getVendorId());
		res.append(", needs encoding=").append(needsEncryption());
		res.append(", vFlag=").append(_vFlag);
		res.append(", mFlag=").append(_mFlag);
		res.append(", pFlag=").append(_pFlag);
		res.append(", format=").append(getDiameterAVPFormat());
		res.append("]");

		return res.toString();
	}

	/**
	 * The single static instance of the Dictionary.
	 */
	public static final Dictionary DICTIONARY = new Dictionary();

	
	public static class Dictionary  {
		
		private static final DictionaryImpl impl = new DictionaryImpl();
		
		/**
		 * Registers a Diameter AVP definition.
		 * 
		 * @param definition The definition to register.
		 */
		public static void registerDiameterAVPDefinition(DiameterAVPDefinition definition) {
			impl.registerDiameterAVPDefinition(definition, false);
		}

		/**
		 * Gets the definition matching a code and a vendor identifier.
		 * 
		 * @param code The AVP code.
		 * @param vendorId The vendor identifier.
		 * @return The DiameterAVPDefinition or <code>null</code> in no
		 *         DiameterAVPDefinition was found.
		 */
		public static DiameterAVPDefinition getDiameterAVPDefinition(long code, long vendorId) {
			return impl.getAVPDefinitionByCode(code, vendorId);
		}
		
		public static AbstractDiameterAVPDictionary getBackingAVPDictionary() {
			return impl;
		}
	}
	
	private static class DictionaryImpl extends AbstractDiameterAVPDictionary {
		public DictionaryImpl() {
			super();
		}
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_code ^ (_code >>> 32));
		result = prime * result + ((_format == null) ? 0 : _format.hashCode());
		result = prime * result + _mFlag;
		result = prime * result + ((_name == null) ? 0 : _name.hashCode());
		result = prime * result + (_needEnc ? 1231 : 1237);
		result = prime * result + _pFlag;
		result = prime * result + _vFlag;
		result = prime * result + (int) (_vendorId ^ (_vendorId >>> 32));
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DiameterAVPDefinition other = (DiameterAVPDefinition) obj;
		if (_code != other._code) {
			return false;
		}
		if (_format == null) {
			if (other._format != null) {
				return false;
			}
		} else if (!_format.equals(other._format)) {
			return false;
		}
		if (_mFlag != other._mFlag) {
			return false;
		}
		if (_name == null) {
			if (other._name != null) {
				return false;
			}
		} else if (!_name.equals(other._name)) {
			return false;
		}
		if (_needEnc != other._needEnc) {
			return false;
		}
		if (_pFlag != other._pFlag) {
			return false;
		}
		if (_vFlag != other._vFlag) {
			return false;
		}
		if (_vendorId != other._vendorId) {
			return false;
		}
		return true;
	}
}
