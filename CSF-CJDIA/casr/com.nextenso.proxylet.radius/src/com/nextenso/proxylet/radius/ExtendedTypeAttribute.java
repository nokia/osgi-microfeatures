package com.nextenso.proxylet.radius;

/**
 * This class encapsulates an Extended Type Attribute (See RFC 6929 section
 * 2.1) and a Long Extended Type Attribute (See RFC 6929 section
 * 2.2)
 * <p/>
 * A (Long) Extended Type Attribute is a regular Attribute whose type can be:
 * <ul>
 * <li>241 for Extended-Type-1
 * <li>242 for Extended-Type-2
 * <li>243 for Extended-Type-3
 * <li>244 for Extended-Type-4
 * <li>245 for LongExtended-Type-1
 * <li>244 for LongExtended-Type-2
 * </ul>
 * Then it carries a new field : the extended type, which makes it possible to extend the attribute type space.
 */
public class ExtendedTypeAttribute
    extends RadiusAttribute {

    /**
     * The type for Extended-Type-1 : 241
     */
    public static final int Extended_Type_1 = 241;
    /**
     * The type for Extended-Type-2 : 242
     */
    public static final int Extended_Type_2 = 242;
    /**
     * The type for Extended-Type-3 : 243
     */
    public static final int Extended_Type_3 = 243;
    /**
     * The type for Extended-Type-4 : 244
     */
    public static final int Extended_Type_4 = 244;
    /**
     * The type for Long-Extended-Type-1 : 245
     */
    public static final int Long_Extended_Type_1 = 245;
    /**
     * The type for Long-Extended-Type-2 : 246
     */
    public static final int Long_Extended_Type_2 = 246;

    private int _extendedType;

    /**
     * Constructs a new ExtendedTypeAttribute with the specified type and extendedType.
     * 
     * @param type the type
     * @param extendedType the extended type
     */
    public ExtendedTypeAttribute(int type, int extendedType){
	super ();
	_extendedType = extendedType;
	switch(type){
	case Extended_Type_1:
	case Extended_Type_2:
	case Extended_Type_3:
	case Extended_Type_4:
	case Long_Extended_Type_1:
	case Long_Extended_Type_2: setType (type); break;
	default : throw new IllegalArgumentException ("Invalid ExtendedTypeAttribute type : "+type);
	}
    }

    /**
     * Gets the extended type.
     * 
     * @return The extended type.
     */
    public int getExtendedType() {
	return _extendedType;
    }

    /**
     * Indicates if it is a Long Extended Type Attribute.
     * 
     * @return true if long extended type.
     */
    public boolean isLongExtendedType () {
	return _extendedType == Long_Extended_Type_1 || _extendedType == Long_Extended_Type_2;
    }

    /**
     * @see com.nextenso.proxylet.radius.RadiusAttribute#toString()
     */
    @Override
    public String toString() {
	StringBuilder buff = new StringBuilder();
	buff.append (super.toString());
	buff.append(" [ExtendedType=");
	buff.append(String.valueOf(_extendedType));
	buff.append(']');
	return buff.toString();
    }
}
