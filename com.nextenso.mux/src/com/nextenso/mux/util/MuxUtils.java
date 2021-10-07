package com.nextenso.mux.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.nextenso.mux.MuxConnection;

public class MuxUtils
{
    /**
     * The Min value (inclusive) for a flag indicating mux opaque data. <br/>
     * This value is set at Runtime by the implementation.
     */
    public static int MUX_DATA_FLAGS_MIN_VALUE = 0;
    /**
     * The Max value (inclusive) for a flag indicating mux opaque data. <br/>
     * This value is set at Runtime by the implementation.
     */
    public static int MUX_DATA_FLAGS_MAX_VALUE = 0xFF;

    public MuxUtils()
    {
    }

    /**
     * to hex converter
     */
    private static final char[] toHex =
            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    // ASC - copy from NxSipStack/gov/nist/javax/sip/Utils.java
    /**
     * convert an array of bytes to an hexadecimal string
     * 
     * @return a string
     * @param b bytes array to convert to a hexadecimal string
     */
    public static String toHexString(byte b[])
    {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++)
        {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    /**
     * Converts an array of bytes with a given offset and length to an hexadecimal
     * string
     * 
     * @return a string
     * @param b bytes array to convert to a hexadecimal string
     */
    public static String toHexString(byte b[], int off, int len)
    {
        int pos = 0;
        char[] c = new char[len * 2];
        for (int i = off; i < off + len; i++)
        {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    public static int encodeMuxDataFlags(int flags)
    {
        if (flags > MUX_DATA_FLAGS_MAX_VALUE)
            throw new IllegalArgumentException("Cannot encode muxData flags: " + flags);
        return MUX_DATA_FLAGS_MIN_VALUE + flags;
    }

    public static int decodeMuxDataFlags(int flags)
    {
        if (flags < MUX_DATA_FLAGS_MIN_VALUE || flags > MUX_DATA_FLAGS_MAX_VALUE)
            throw new IllegalArgumentException("Cannot decode muxData flags: " + flags);
        return flags - MUX_DATA_FLAGS_MIN_VALUE;
    }

    public static int getIPAsInt(byte[] from, int off)
    {
        int ip = from[off] & 0xFF;
        ip <<= 8;
        ip |= from[off + 1] & 0xFF;
        ip <<= 8;
        ip |= from[off + 2] & 0xFF;
        ip <<= 8;
        ip |= from[off + 3] & 0xFF;
        return ip;
    }

    public static int getIPAsInt(ByteBuffer from)
    {
        int ip = from.get() & 0xFF;
        ip <<= 8;
        ip |= from.get() & 0xFF;
        ip <<= 8;
        ip |= from.get() & 0xFF;
        ip <<= 8;
        ip |= from.get() & 0xFF;
        return ip;
    }

    public static boolean isNull(String ipOrHostName)
    {
        return ipOrHostName == null || ipOrHostName.equals("");
    }

    public static String getIPAsString(int ip)
    {
        if (ip == 0)
        {
            return "";
        }
        StringBuilder buff = new StringBuilder(15);
        buff.append( (ip >>> 24) & 0xFF);
        buff.append('.');
        buff.append( ((ip >> 16) & 0xFF));
        buff.append('.');
        buff.append( ((ip >> 8) & 0xFF));
        buff.append('.');
        buff.append( ip & 0xFF);
        return buff.toString();
    }

    public static int getIPAsInt(String ip)
    {
        try
        {
            int res = 0;
            int index1 = 0;

            if (ip == null || ip.length() == 0)
            {
                return 0;
            }

            for (int i = 0; i < 4; i++)
            {
                int index2 = ip.indexOf('.', index1);
                if (index2 == -1)
                {
                    if (i == 3)
                        index2 = ip.length();
                    else
                        // will be caught below
                        throw new Exception();
                }
                int value = Integer.parseInt(ip.substring(index1, index2));
                if (value < 0 || value > 255)
                {
                    throw new Exception();
                }
                if (i == 0)
                {
                    res = value << 24;
                }
                else if (i == 1)
                {
                    res |= (value << 16) & 0xFF0000;
                }
                else if (i == 2)
                {
                    res |= (value << 8) & 0xFF00;
                }
                else if (i == 3)
                {
                    res |= value & 0xFF;
                }
                index1 = index2 + 1;
            }
            return res;
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    public static final int get_8(byte[] from, int off)
    {
        return from[off] & 0xFF;
    }

    public static final int get_8(ByteBuffer from)
    {
        return from.get() & 0xFF;
    }

    public static final int get_16(byte[] from, int off, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            int res = from[off] & 0xFF;
            res <<= 8;
            res |= from[off + 1] & 0xFF;
            return res;
        }

        int res = from[off + 1] & 0xFF;
        res <<= 8;
        res |= from[off] & 0xFF;
        return res;
    }

    public static final int get_16(ByteBuffer from)
    {
        int res = from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        return res;
    }

    public static final int get_32(byte[] from, int off, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            int res = from[off];
            res <<= 8;
            res |= from[off + 1] & 0xFF;
            res <<= 8;
            res |= from[off + 2] & 0xFF;
            res <<= 8;
            res |= from[off + 3] & 0xFF;
            return res;
        }

        int res = from[off + 3];
        res <<= 8;
        res |= from[off + 2] & 0xFF;
        res <<= 8;
        res |= from[off + 1] & 0xFF;
        res <<= 8;
        res |= from[off] & 0xFF;
        return res;
    }

    public static final int get_32(ByteBuffer from)
    {
        int res = from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        return res;
    }

    public static final long get_64(byte[] from, int off, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            long res = from[off];
            res <<= 8;
            res |= from[off + 1] & 0xFF;
            res <<= 8;
            res |= from[off + 2] & 0xFF;
            res <<= 8;
            res |= from[off + 3] & 0xFF;
            res <<= 8;
            res |= from[off + 4] & 0xFF;
            res <<= 8;
            res |= from[off + 5] & 0xFF;
            res <<= 8;
            res |= from[off + 6] & 0xFF;
            res <<= 8;
            res |= from[off + 7] & 0xFF;
            return res;
        }

        long res = from[off + 7];
        res <<= 8;
        res |= from[off + 6] & 0xFF;
        res <<= 8;
        res |= from[off + 5] & 0xFF;
        res <<= 8;
        res |= from[off + 4] & 0xFF;
        res <<= 8;
        res |= from[off + 3] & 0xFF;
        res <<= 8;
        res |= from[off + 2] & 0xFF;
        res <<= 8;
        res |= from[off + 1] & 0xFF;
        res <<= 8;
        res |= from[off] & 0xFF;
        return res;
    }

    public static final int get_64(ByteBuffer from)
    {
        int res = from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        res <<= 8;
        res |= from.get() & 0xFF;
        return res;
    }

    public static final void put_8(byte[] dest, int off, int val)
    {
        dest[off] = (byte) val;
    }

    public static final void put_8(OutputStream out, int val) throws IOException
    {
        out.write((byte) val);
    }

    public static final void put_8(ByteBuffer dest, int val)
    {
        dest.put((byte) val);
    }

    public static final void put_16(byte[] dest, int off, int val, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            dest[off] = (byte) (val >> 8);
            dest[off + 1] = (byte) val;
        }
        else
        {
            dest[off + 1] = (byte) (val >> 8);
            dest[off] = (byte) val;
        }
    }

    public static final void put_16(ByteBuffer dest, int val, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            dest.put((byte) (val >> 8));
            dest.put((byte) val);
        }
        else
        {
            dest.put((byte) val);
            dest.put((byte) (val >> 8));
        }
    }

    public static final void put_16(OutputStream dest, int val, boolean networkByteOrder) throws IOException
    {
        if (networkByteOrder)
        {
            dest.write((byte) (val >> 8));
            dest.write((byte) val);
        }
        else
        {
            dest.write((byte) val);
            dest.write((byte) (val >> 8));
        }
    }

    public static final void put_32(byte[] dest, int off, int val, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            dest[off] = (byte) (val >> 24);
            dest[off + 1] = (byte) (val >> 16);
            dest[off + 2] = (byte) (val >> 8);
            dest[off + 3] = (byte) val;
        }
        else
        {
            dest[off + 3] = (byte) (val >> 24);
            dest[off + 2] = (byte) (val >> 16);
            dest[off + 1] = (byte) (val >> 8);
            dest[off] = (byte) val;
        }
    }

    public static final void put_32(ByteBuffer dest, int val, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            dest.put((byte) (val >> 24));
            dest.put((byte) (val >> 16));
            dest.put((byte) (val >> 8));
            dest.put((byte) val);
        }
        else
        {
            dest.put((byte) val);
            dest.put((byte) (val >> 8));
            dest.put((byte) (val >> 16));
            dest.put((byte) (val >> 24));
        }
    }

    public static final void put_32(OutputStream dest, int val, boolean networkByteOrder) throws IOException
    {
        if (networkByteOrder)
        {
            dest.write((byte) (val >> 24));
            dest.write((byte) (val >> 16));
            dest.write((byte) (val >> 8));
            dest.write((byte) val);
        }
        else
        {
            dest.write((byte) val);
            dest.write((byte) (val >> 8));
            dest.write((byte) (val >> 16));
            dest.write((byte) (val >> 24));
        }
    }

    public static final void put_64(byte[] dest, int off, long val, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            dest[off] = (byte) (val >> 56);
            dest[off + 1] = (byte) (val >> 48);
            dest[off + 2] = (byte) (val >> 40);
            dest[off + 3] = (byte) (val >> 32);
            dest[off + 4] = (byte) (val >> 24);
            dest[off + 5] = (byte) (val >> 16);
            dest[off + 6] = (byte) (val >> 8);
            dest[off + 7] = (byte) val;
        }
        else
        {
            dest[off + 7] = (byte) (val >> 56);
            dest[off + 6] = (byte) (val >> 48);
            dest[off + 5] = (byte) (val >> 40);
            dest[off + 4] = (byte) (val >> 32);
            dest[off + 3] = (byte) (val >> 24);
            dest[off + 2] = (byte) (val >> 16);
            dest[off + 1] = (byte) (val >> 8);
            dest[off] = (byte) val;
        }
    }

    public static final void put_64(ByteBuffer dest, long val, boolean networkByteOrder)
    {
        if (networkByteOrder)
        {
            dest.put((byte) (val >> 56));
            dest.put((byte) (val >> 48));
            dest.put((byte) (val >> 40));
            dest.put((byte) (val >> 32));
            dest.put((byte) (val >> 24));
            dest.put((byte) (val >> 16));
            dest.put((byte) (val >> 8));
            dest.put((byte) val);
        }
        else
        {
            dest.put((byte) val);
            dest.put((byte) (val >> 8));
            dest.put((byte) (val >> 16));
            dest.put((byte) (val >> 24));
            dest.put((byte) (val >> 32));
            dest.put((byte) (val >> 40));
            dest.put((byte) (val >> 48));
            dest.put((byte) (val >> 56));
        }
    }

    public static final void put_64(OutputStream dest, long val, boolean networkByteOrder) throws IOException
    {
        if (networkByteOrder)
        {
            dest.write((byte) (val >> 56));
            dest.write((byte) (val >> 48));
            dest.write((byte) (val >> 40));
            dest.write((byte) (val >> 32));
            dest.write((byte) (val >> 24));
            dest.write((byte) (val >> 16));
            dest.write((byte) (val >> 8));
            dest.write((byte) val);
        }
        else
        {
            dest.write((byte) val);
            dest.write((byte) (val >> 8));
            dest.write((byte) (val >> 16));
            dest.write((byte) (val >> 24));
            dest.write((byte) (val >> 32));
            dest.write((byte) (val >> 40));
            dest.write((byte) (val >> 48));
            dest.write((byte) (val >> 56));
        }
    }

    public static final void put_string(byte[] dest, int off, String s)
    {
        byte[] b = s.getBytes();
        System.arraycopy(b, 0, dest, off, b.length);
    }

    public static final long concat(int a, int b)
    {
        return (((long) a) << 32) | b;
    }

    public static final int getFirstInt(long l)
    {
        return (int) (l >>> 32);
    }

    public static final int getSecondInt(long l)
    {
        return (int) l;
    }

    public static final int ERROR_UNDEFINED = 1;
    public static final int ERROR_CONNECTION_REFUSED = 2;
    public static final int ERROR_ADDRESS_IN_USE = 3;
    public static final int ERROR_TIMEOUT = 4;
    public static final int ERROR_HOST_NOT_FOUND = 5;
    public static final int ERROR_TOO_MANY_SOCKETS = 6;

    public static final String getErrorMessage(int errno)
    {
        switch (errno) {
        case ERROR_UNDEFINED:
            return "Undefined error";
        case ERROR_CONNECTION_REFUSED:
            return "Connection refused";
        case ERROR_ADDRESS_IN_USE:
            return "Address in use";
        case ERROR_TIMEOUT:
            return "Timeout";
        case ERROR_HOST_NOT_FOUND:
            return "Host not found";
        case ERROR_TOO_MANY_SOCKETS:
            return "Too many sockets connected";
        default:
            return "Invalid errno";
        }
    }
  
}
