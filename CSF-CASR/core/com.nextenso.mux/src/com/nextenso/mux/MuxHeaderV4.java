package com.nextenso.mux;

/**
 * This is the Version-4 of the MuxHeader.
 * <p/>
 * The fields are in the order:<br/>
 * <ul>
 * <li>the flags (1 byte)
 * <li>the length (2 bytes)
 * </ul>
 * <br/>
 * The length is not part of the Header. The length is directly determined by
 * the size of the data sent along with the Header.
 */
public class MuxHeaderV4 extends MuxHeaderV0
{

    public MuxHeaderV4()
    {
    }

    @Override
    public int getVersion()
    {
        return 4;
    }
}
